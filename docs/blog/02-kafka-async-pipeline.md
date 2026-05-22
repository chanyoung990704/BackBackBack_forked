# 외부 API가 15초 걸릴 때: Kafka 비동기 파이프라인 설계기

## 1. 문제 상황: "AI 리포트 한 번 받는 데 15초... 사용자는 뒤로가기를 누른다"
우리 프로젝트의 킬러 서비스는 특정 상장 기업에 대한 다각도 AI 분석 결과를 제공하고, 이를 기반으로 종합 평가 PDF 리포트를 자동 생성하여 다운로드할 수 있는 기능이다. 

문제는 이 "AI PDF 리포트 생성" 과정이 극단적으로 무거운 작업이라는 점이었다.
* **15초 이상의 무거운 지연:** 백엔드가 AI 분석 모델 서버로 REST API 요청을 보내면, AI 서버는 복잡한 재무 지표 시계열 분석을 수행하고 차트를 그린 뒤 PDF 파일 바이너리를 생성해 내려준다. 평균 응답 시간이 최소 15초에서 최대 1분까지 달했다.
* **사용자 경험의 최악화:** 사용자가 "AI 보고서 요청" 버튼을 누르는 순간 브라우저는 15초 동안 멈춘다. 그 시간 동안 사용자는 진행 상황을 알 수 없어 답답해하며 새로고침을 연타하거나 아예 페이지를 이탈해버렸다.
* **서버 커넥션 풀 고갈:** HTTP 동기 요청 모델의 특성상, 하나의 요청이 완료될 때까지 서블릿 스레드와 RDB 커넥션, 외부 HTTP 클라이언트 커넥션이 계속 묶여 있게 된다. 이러한 요청이 동시에 20개만 몰려도 서버는 급격하게 모든 리소스를 잠식당하며 장애 상태에 빠져 버렸다.

이를 근본적으로 극복하기 위해 기존의 동기식 결합 모델을 깨고, **이벤트 기반 비동기 파이프라인(Event-Driven Asynchronous Pipeline)**으로 아키텍처를 전면 전환하기로 결심했다.

---

## 2. 비동기/Fallback 아키텍처 다이어그램 (Mermaid)

다음은 최종 구성한 Kafka 비동기 파이프라인 및 백업 장애 대응(Fallback) 구조이다.

```mermaid
graph TD
    Client[클라이언트 브라우저] -->|1. 리포트 생성 비동기 요청| Controller[CompanyAiController]
    Controller -->|2. 디스패치 위임| Dispatcher[AiJobDispatchService]
    
    subgraph Kafka Pipeline (주요 경로)
        Dispatcher -->|3. 메시지 발행 성공| Kafka[(Kafka Topic: ai-job-request)]
        Kafka -->|4. 이벤트 소비| Consumer[AiJobKafkaConsumer]
        Consumer -->|5. 동기 생성 작업 수행| AiService[CompanyAiService / processReportGeneration]
    end

    subgraph Spring Async Fallback (장애 복구 경로)
        Dispatcher -->|3'. Kafka 다운 / Bean 미존재로 실패| Controller
        Controller -->|4'. Spring @Async 실행기로 위임| FallbackAsync[generateReportAsync]
        FallbackAsync -->|5'. 동기 생성 작업 수행| AiService
    end

    AiService -->|6. AI 서버 통신 (15초)| ExternalAi[외부 AI 서버]
    AiService -->|7. PDF 다운로드 및 스토리지 보관| S3[(File Storage / DB)]
    AiService -->|8. 처리 완료 상태 업데이트| Redis[(Redis: ai-report:requestId:status)]

    Client -->|9. 폴링: 진행 상황 확인 (3초 주기)| Controller
    Controller -->|10. 현재 상태 조회| Redis
```

---

## 3. 시도한 방법들과 솔루션

### 해결 1: 단순 `@Async` 도입과 그 한계점
가장 먼저 시도한 방식은 스프링이 기본 제공하는 `@Async` 어노테이션을 사용하여 요청을 백그라운드 스레드로 넘겨버리는 방식이었다. 
* **구현 방식:** 컨트롤러에서 요청을 받고, `ThreadPoolTaskExecutor` 기반의 독립 스레드를 만들어 AI 서버와 통신하게 했다. 컨트롤러는 즉시 사용자에게 "요청 완료(Accepted - 202)"와 함께 `requestId`를 리턴해 주고, 클라이언트는 이 `requestId`로 서버를 3초마다 폴링(Polling)하며 상태가 `PROCESSING`에서 `COMPLETED`로 바뀌는지를 추적하게 했다.
* **한계점:** 이 방식은 즉각적인 스레드 고갈 문제는 해결해 줬지만 두 가지 치명적인 한계가 있었다.
  1. **내부 메모리 기반 대기열의 위험:** 동시 요청이 백그라운드 실행기 풀(Queue) 크기를 초과하면 메모리에 작업이 대기하게 된다. 이 상황에서 서버 프로세스가 재기동되거나 크래시가 나면 대기 중이던 모든 AI 요청 메시지가 증발한다. (영속성 보장 안 됨)
  2. **수평 확장(Scale-out) 시 통제 불가:** 다중 서버 환경에서 1번 백엔드 서버가 비동기 요청을 받았는데, 다음 클라이언트 폴링이 L4 로드밸런서에 의해 2번 백엔드 서버로 흐른다면 로컬 메모리 상태가 동기화되지 않아 상태 추적이 불가능해진다.

### 해결 2: 분산 메시지 브로커 Kafka 도입 결정
메모리 기반 비동기의 치명적 한계를 완전히 극복하기 위해 **분산 이벤트 스트리밍 플랫폼인 Apache Kafka**를 중앙 파이프라인으로 채택했다.
* **장점 1 - 메시지 영속성(Durability):** Kafka는 프로듀서가 보낸 메시지를 디스크에 기록하므로, 소비하는 백엔드 컨슈머 서버가 다운되었다가 살아나도 손실 없이 마지막 처리 지점(Offset)부터 안정적으로 재개할 수 있다.
* **장점 2 - 부하 분산 및 큐 제어:** AI 서버의 수용 한계량에 맞게 백엔드 컨슈머 서버 개수를 조절하거나 컨슈머 내 스레드 풀을 제한함으로써, 외부 AI 서버에 트래픽이 폭증하는 "DDoS형 부하 상황"을 안전하게 통제할 수 있다.
* **장점 3 - 유연한 수평 확장:** 클라이언트의 폴링 요청이 어느 다중 인스턴스로 분산되어도, 공통 분산 세션(Redis)과 메시지 큐를 보며 정상적으로 상태 업데이트가 이뤄진다.

### 안전 장치: 우아한 Fallback 설계 (Kafka 장애 복구 경로)
우수한 아키텍처는 "중앙 시스템(Kafka)이 다운되더라도 전체 서비스가 멈추지 않는 구조"를 지녀야 한다. 따라서 **Kafka 프로듀서 발행 실패 시 로컬 `@Async` 백그라운드 스레드 풀로 작업을 우회(Fallback)시키는 동적 전환 구조**를 구현했다.
* 컨트롤러는 `AiJobDispatchService`를 통해 카프카 메시지 발행을 시도한다.
* 카프카 브로커 자체가 응답 불가이거나, 토글 설정(`app.ai.job.kafka-enabled=false`)이 비활성화되어 `false`를 리턴하는 즉시, 백엔드 로컬 스레드 풀 기반 비동기 메서드(`generateReportAsync`)를 실행하여 서비스 연속성을 안정적으로 이행한다.

---

## 4. 실제 프로젝트 소스 코드 분석

### 1) 카프카 토글에 따른 위임 및 안전장치가 반영된 디스패처 (`AiJobDispatchService`)
`app.ai.job.kafka-enabled` 플래그 및 `ObjectProvider<KafkaTemplate>`를 활용하여, 카프카 인프라의 존재 유무 및 비활성 환경에서도 예외를 터뜨리지 않고 `false`를 우아하게 반환하도록 설계했다.

```java
package com.aivle.project.company.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobDispatchService {

	@Value("${app.ai.job.kafka-enabled:false}")
	private boolean kafkaEnabled;

	@Value("${app.ai.job.request-topic:ai-job-request}")
	private String requestTopic;

	private final ObjectMapper objectMapper;
	private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

	public boolean dispatchReport(String requestId, Long companyId, Integer year, Integer quarter) {
		return dispatch(AiJobMessage.forReport(requestId, companyId, year, quarter));
	}

	private boolean dispatch(AiJobMessage message) {
		if (!kafkaEnabled) {
			log.info("Kafka is disabled by toggle. Skip dispatch: requestId={}", message.requestId());
			return false;
		}
		KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
		if (kafkaTemplate == null) {
			log.warn("KafkaTemplate bean missing. Skip ai job dispatch: type={}, requestId={}", message.type(), message.requestId());
			return false;
		}
		try {
			String payload = objectMapper.writeValueAsString(message);
			kafkaTemplate.send(requestTopic, message.requestId(), payload);
			log.info("Dispatched AI job to Kafka: topic={}, type={}, requestId={}, companyId={}",
				requestTopic, message.type(), message.requestId(), message.companyId());
			return true;
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize AI job message: type={}, requestId={}", message.type(), message.requestId(), e);
			return false;
		}
	}
}
```

### 2) 컨트롤러 단의 Fallback 복구 코드 설계 (`CompanyAiController`)
디스패처의 리턴값(`dispatched`)을 기준으로 정상 경로와 로컬 스레드 우회 경로를 깔끔하게 분기 처리했다.

```java
    @PostMapping({"/{companyId}/ai-reports/requests", "/{companyId}/ai-report/request"})
    @Operation(summary = "AI 리포트 생성 요청", description = "AI 리포트 생성을 비동기로 요청합니다. 반환된 requestId로 상태를 확인할 수 있습니다.")
    public ResponseEntity<ApiResponse<AiReportRequestResponse>> requestAiReport(
        @PathVariable("companyId") Long companyId,
        @RequestParam(value = "year", required = false) Integer year,
        @RequestParam(value = "quarter", required = false) Integer quarter
    ) {
        String requestId = UUID.randomUUID().toString();
        // 1. Redis/RDB에 초기 PENDING 상태 등록
        aiReportRequestStatusService.createPending(requestId, companyId.toString(), year, quarter);
        
        // 2. 카프카 분산 큐로 발행 시도
        boolean dispatched = aiJobDispatchService.dispatchReport(requestId, companyId, year, quarter);
        
        if (!dispatched) {
            // [핵심 Fallback] 카프카 비활성/미설정/장애 환경에서는 로컬 백그라운드 스레드 경로로 안전하게 Fallback한다.
            log.warn("Kafka dispatch failed. Fallback to local Spring @Async task for requestId={}", requestId);
            companyAiService.generateReportAsync(requestId, companyId, year, quarter);
        }
        
        // 즉각적인 202 Accepted 반환
        return ResponseEntity.accepted().body(ApiResponse.ok(new AiReportRequestResponse(requestId)));
    }
```

### 3) `@Async` 어노테이션 기반의 안전한 폴백 타스크 실행 메서드 (`CompanyAiService`)
`@Async("insightExecutor")`를 선언하여 컨트롤러 스레드를 방해하지 않고 독립 실행기에서 비동기로 PDF를 생성하여 보관한다.

```java
    @Async("insightExecutor")
    @Transactional
    public void generateReportAsync(String requestId, Long companyId, Integer year, Integer quarter) {
        processReportGeneration(requestId, companyId, year, quarter);
    }
```

### 4) 카프카 메시지를 전담 소비하는 안정적인 리스너 (`AiJobKafkaConsumer`)
메시지를 안전하게 언마샬링하고 실제 도메인 로직(`processReportGeneration`)으로 토스해준다.

```java
package com.aivle.project.company.job;

import com.aivle.project.company.service.CompanyAiCommentService;
import com.aivle.project.company.service.CompanyAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final CompanyAiService companyAiService;
	private final CompanyAiCommentService companyAiCommentService;

	@KafkaListener(
		topics = "${app.ai.job.request-topic:ai-job-request}",
		groupId = "${APP_AI_JOB_KAFKA_GROUP_ID:ai-job-consumer}"
	)
	public void consume(String payload) {
		try {
			AiJobMessage message = objectMapper.readValue(payload, AiJobMessage.class);
			process(message);
		} catch (Exception e) {
			log.error("Failed to consume AI job payload: {}", payload, e);
			throw new RuntimeException("Failed to process AI job", e);
		}
	}

	private void process(AiJobMessage message) {
		switch (message.type()) {
			case AI_REPORT -> companyAiService.processReportGeneration(
				message.requestId(),
				message.companyId(),
				message.year(),
				message.quarter()
			);
			case AI_COMMENT_WARMUP -> companyAiCommentService.ensureAiCommentCached(
				message.companyId(),
				message.period()
			);
			default -> log.warn("Unsupported AI job type: {}", message.type());
		}
	}
}
```

---

## 5. 최종 결과 및 배운 점

### 1) 느슨한 결합(Loose Coupling)과 트래픽 대응력 극대화
중앙에 Kafka 파이프라인을 도입한 뒤로, HTTP 요청의 커넥션 타임아웃과 스레드 풀 잠식으로 비명을 지르던 백엔드 서버가 평온을 되찾았다. 
이제 동시 요청이 100건이 들어와도 백엔드는 0.05초 만에 `202 Accepted` 응답을 뿜어낸 뒤 차례차례 메시지를 소비한다. 트래픽의 스파이크 충격이 안전하게 Kafka라는 "완충 영역"에 흡수된 것이다.

### 2) 언제나 실패를 염두에 둔 "우아한 기능 저하(Graceful Degradation)"
메시지 큐(Kafka)를 도입하면서 발생할 수 있는 잠재적 인프라 실패에 대비하여 **스프링 비동기 `@Async` 백업 경로**를 미리 촘촘하게 짜 두었던 것이 신의 한 수였다.
개발 도중 로컬 카프카 컨테이너가 예기치 않게 죽었을 때도, 콘솔에는 경고 로그만 남을 뿐 웹 애플리케이션은 중단 없이 로컬 스레드로 안전하게 PDF를 생성하며 동작을 이어갔다. "기술적 멋짐"보다 중요한 것은 언제 어떤 모듈이 고장 나도 시스템이 버텨내야 한다는 "프로덕션 엔지니어의 철학"임을 이 비동기 파이프라인 설계를 통해 깊이 깨달았다.
