# Resilience4j로 외부 장애가 내 서비스를 죽이지 않게 만들기

## 1. 문제 상황: "AI 서버가 죽었는데, 왜 우리 회원가입까지 먹통이 되지?"
우리 서비스 아키텍처는 내부 데이터 비즈니스를 처리하는 메인 백엔드 서버와 재무 분석 고성능 추론을 담당하는 외부 AI 서버로 나뉘어 있다. 

어느 날 갑자기 메인 서비스의 응답 지연이 무한정 길어지며 전체 백엔드가 완전히 마비되는 초대형 장애가 발생했다.
* **장애의 발원지:** 원인은 외부 AI 서버의 과부하로 인한 다운(Down)이었다.
* **눈사태 효과(Cascading Failure):** 우리 백엔드 서버의 여러 핵심 로직은 AI 분석 결과를 동기로 호출하고 있었다. 외부 AI 서버가 무반응 상태가 되자 백엔드에서 AI 서버로 찌르는 HTTP 커넥션들이 10초 이상 타임아웃 대기 상태에 빠졌다. 
* **스레드 잠식과 전파:** 지연되는 요청들이 쌓이면서 톰캣 서블릿 스레드 풀의 모든 스레드가 외부 API 응답을 하염없이 대기하느라 점유되었다. 이로 인해 AI 기능과 전혀 무관한 "단순 로그인", "회원가입" API마저 스레드를 할당받지 못해 큐에 갇히고 타임아웃 에러를 내며 동반 무너졌다.

외부 의존성 서비스의 고장이 우리 집 안방까지 초토화시키는 **눈사태 현상(Cascading Failure)**을 뼈저리게 겪은 뒤, 외부 장애를 철저히 물리적으로 격리하여 시스템의 회복탄력성을 보장하는 **장애 격리 아키텍처**를 구축하기로 결심했다.

---

## 2. 장애 격리 및 복구 아키텍처 다이어그램 (Mermaid)

다음은 Resilience4j의 **Circuit Breaker, Retry, Bulkhead** 3단 콤보를 활용한 장애 전파 선제 격리 설계도이다.

```mermaid
flowchart TD
    Client[클라이언트] -->|API 요청| Gateway[Spring Security / Tomcat]
    Gateway -->|스레드 할당| ClientMethod[AiServerClient]

    subgraph Resilience4j Barrier
        ClientMethod -->|1. Bulkhead 진입 검사| BH{Bulkhead: Max 20}
        BH -->|동시 요청 수 초과시| FastFail[즉시 거절: BulkheadFullException]
        BH -->|통과| CB{Circuit Breaker}
        
        CB -->|서킷 OPEN (장애 상태)| CBFail[즉시 거절: CallNotPermittedException]
        CB -->|서킷 CLOSED / HALF_OPEN| RetryRunner{Retry: Max 1}
    end

    RetryRunner -->|2. HTTP 호출 시도| ExternalAI[외부 AI 서버]
    
    ExternalAI -->|3. 타임아웃 / 5xx 장애 발생| RetryRunner
    RetryRunner -->|4. 예외 필터링| Resolver[resolveReasonCode]
    
    Resolver -->|Timeout 감지| Reason1[AI_TIMEOUT]
    Resolver -->|Circuit Open 감지| Reason2[AI_CIRCUIT_OPEN]
    Resolver -->|기타 커넥션 실패| Reason3[AI_UNAVAILABLE]

    Reason1 & Reason2 & Reason3 --> Exception[ExternalAiUnavailableException]
    Exception -->|5. 에러 응답 전달| Client
```

---

## 3. 시도한 방법들과 장애 격리 전략

외부 호출 장애를 극복하기 위해 Spring 진영의 표준 격리 라이브러리인 **Resilience4j**를 도입하여 3가지 상호보완적인 격리 장벽을 설치했다.

### 1단계: Circuit Breaker로 외부 호출 차단 (서킷 브레이커)
* **목적:** 외부 서버가 맛이 갔다고 판단되면, 더는 무의미한 네트워크 호출을 보내지 않고 **즉시 에러를 반환(Fail-fast)**하여 우리 측 스레드 리소스를 안전하게 보호한다.
* **설정 전략:** 
  * 분석 요청이 평균 70초 이상 걸리거나 타임아웃 예외가 연속으로 발생하면 상태를 기록한다.
  * 최근 20번의 요청 중(`slidingWindowSize: 20`), 최소 10번 이상 실행되었을 때(`minimumNumberOfCalls: 10`) 실패율이 50%를 초과하거나(`failureRateThreshold: 50`), 느린 호출 비율이 60%를 초과하면(`slowCallRateThreshold: 60`) 서킷을 즉시 **OPEN** 시킨다.
  * 서킷이 OPEN 되면 20초 동안(`waitDurationInOpenState: 20s`) 외부 서버 호출을 원천 차단하고 즉시 예외를 반환하여 백엔드 스레드가 대기 상태에 빠지지 않게 막는다.

### 2단계: Retry + Bulkhead 조합으로 무한 대기 격리
* **Retry (재시도 전략):** AI 리포트 생성은 무겁고 일시적인 네트워크 순instant 발작 오류일 확률이 크므로, 재시도 횟수를 딱 **1회**로 한정하고(`maxAttempts: 1`) 300ms의 베이스 지연 시간과 함께 지수 백오프(`exponentialBackoffMultiplier: 3`)를 걸어 외부 서버의 연쇄 부하 증가를 유발하지 않도록 했다.
* **Bulkhead (벌크헤드 격리):** 
  * 선박의 격벽(Bulkhead)처럼, 외부 AI 서버를 찌르는 동시 실행 스레드 수 자체를 최대 20개로 제한(`maxConcurrentCalls: 20`)했다.
  * 여기서 가장 중요한 결정은 **`maxWaitDuration: 0ms`** 설정이었다. 동시 요청이 20개를 초과할 때 스레드가 큐에서 0.1초라도 대기하게 내버려 두면, 그 순간 다시 백엔드 톰캣 스레드가 묶이기 시작한다. 대기 시간 없이 즉시 에러(`BulkheadFullException`)를 뿜으며 요청을 드랍하도록 하여 완벽한 리소스 벽을 쳤다.

### 3단계: 사용자 경험(UX) 복구 설계
에러가 발생할 때 사용자에게 날것의 "500 Internal Server Error"를 보여주는 것은 최악의 UX이다. 
우리는 예외 번역 구조를 구현하여, 서킷 브레이커가 차단했는지(`AI_CIRCUIT_OPEN`), 타임아웃이 났는지(`AI_TIMEOUT`) 등의 명확한 원인 코드를 실은 `ExternalAiUnavailableException`으로 래핑했다. 이를 통해 프론트엔드는 "현재 분석 트래픽 폭증으로 임시 모킹 데이터로 우선 보여드립니다" 같은 세련된 안내창을 띄우거나 차선책 데이터를 화면에 제공할 수 있게 되었다.

---

## 4. 실제 프로젝트 소스 코드 분석

### 1) `application.yaml`의 정밀한 Resilience4j 설정 튜닝
실제 운영 조건의 타임아웃 한계치(Connection Timeout 5초, Read/Write 70초)와 에러 감지 예외 목록을 세밀하게 매칭했다.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      aiServer:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 5
        waitDurationInOpenState: 20s
        failureRateThreshold: 50
        slowCallDurationThreshold: 70s
        slowCallRateThreshold: 60
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.reactive.function.client.WebClientRequestException
          - org.springframework.web.reactive.function.client.WebClientResponseException
          - org.springframework.web.reactive.function.client.WebClientException
          - reactor.core.Exceptions$ReactiveException
  retry:
    instances:
      aiServer:
        maxAttempts: 1
        waitDuration: 300ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 3
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.reactive.function.client.WebClientRequestException
          - org.springframework.web.reactive.function.client.WebClientException
  bulkhead:
    instances:
      aiServer:
        maxConcurrentCalls: 20
        maxWaitDuration: 0ms  # 대기 없이 즉시 거절하여 격벽 효과 극대화
```

### 2) 3중 장애 장벽 어노테이션 및 예외 추적 로직이 탑재된 `AiServerClient`
`@CircuitBreaker`, `@Retry`, `@Bulkhead` 어노테이션을 결합하여 중첩 방어막을 쳤으며, `resolveReasonCode` 메서드를 통해 예외 체인을 따라 내려가며 어떤 Resilience4j 모듈 혹은 네트워크 레이어에서 에러가 발생했는지를 정밀 추적하도록 구현했다.

```java
package com.aivle.project.company.client;

import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.common.error.ExternalAiUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Slf4j
@Component
public class AiServerClient {

    private final WebClient webClient;
    private final boolean mockEnabled;
    private final Duration callTimeout;

    // ... 생성자 생략

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiServer")
    @Bulkhead(name = "aiServer", type = Bulkhead.Type.SEMAPHORE)
    public AiAnalysisResponse getPrediction(String companyCode) {
        log.info("Requesting AI prediction for company: {}", companyCode);

        if (mockEnabled) {
            return mockPrediction(companyCode);
        }

        try {
            return webClient.get()
                .uri(builder -> builder.path("/api/v1/analysis/{companyCode}/predict").build(companyCode))
                .retrieve()
                .bodyToMono(AiAnalysisResponse.class)
                .timeout(callTimeout)
                .block();
        } catch (Exception e) {
            log.error("Failed to get prediction for company {}: {}", companyCode, e.getMessage());
            throw toExternalAiUnavailable(e);
        }
    }

    private ExternalAiUnavailableException toExternalAiUnavailable(Throwable throwable) {
        String reasonCode = resolveReasonCode(throwable);
        return new ExternalAiUnavailableException("AI Server connection failed", reasonCode, throwable);
    }

    // 예외의 원인(Cause) 분석을 통해 장애 유발 모듈 규명
    private String resolveReasonCode(Throwable throwable) {
        if (containsCause(throwable, CallNotPermittedException.class)) {
            // 서킷 브레이커에 의해 호출이 차단된 경우
            return "AI_CIRCUIT_OPEN";
        }
        if (containsCause(throwable, io.netty.handler.timeout.ReadTimeoutException.class)
            || containsCause(throwable, java.util.concurrent.TimeoutException.class)) {
            // 타임아웃에 의한 에러인 경우
            return "AI_TIMEOUT";
        }
        if (containsCause(throwable, WebClientRequestException.class)) {
            // 외부 네트워크 커넥션 자체가 불가한 경우
            return "AI_UNAVAILABLE";
        }
        return "AI_UNAVAILABLE";
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
```

---

## 5. 최종 결과 및 배운 점

### 1) 격벽(Isolation) 설계를 통한 결함 한계 격리 완료
Resilience4j를 전격 적용한 뒤, 고의로 외부 AI 서버 컨테이너를 종료시키고 대규모 동시 회원가입 및 일반 API 요청 부하 테스트를 실시했다.
* **이전 결과:** 전체 백엔드 응답 불능 및 타임아웃 폭발 (장애 전파)
* **이후 결과:** AI 관련 API 요청은 `CallNotPermittedException`과 함께 0.01초 만에 `AI_CIRCUIT_OPEN` 원인 코드로 즉시 Fail-fast 처리되었고, 회원가입과 로그인은 스레드 간섭을 단 1%도 받지 않고 평온하게 100% 성공 지표를 유지했다.

### 2) 모니터링 파이프라인 구축의 중요성
장애 격리 장치가 동작하고 있다는 것은, 내부적으로 예외 처리가 조용히 일어나 서버 관리자가 알아채지 못하는 "침묵의 부분 장애"를 유발할 수 있음을 배웠다. 
이를 극복하기 위해 Prometheus에 Resilience4j 라이프사이클 메트릭을 연동하고, Grafana 대시보드에 서킷 상태 변동 모니터링 알람(CLOSED -> OPEN 시 슬랙 알림 발송)을 구축함으로써 시스템의 가시성(Observability)을 확보할 수 있었다.
기술적 설계보다 중요한 것은 **장애가 발생했을 때 이를 안전하게 가두고(격리), 정상 복구 상태를 실시간으로 모니터링하여 지속 관리하는 것**임을 뼈저리게 깨달은 경험이었다.
