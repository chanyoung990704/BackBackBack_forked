# [TRS-002] Java 21 Virtual Thread 도입 및 Pinning 성능 병목 트러블슈팅

## 현상 (Symptom)
- **대규모 I/O 처리 시 성능 저하**: 데이터 수집 파이프라인(기업 뉴스 수집, DART 데이터 동기화, 비동기 이메일 발송 등)에 대규모 동시 요청이 인입될 때, 전통적인 플랫폼 스레드 풀의 스레드 고갈 문제를 해결하기 위해 Java 21의 **가상 스레드(Virtual Thread)**를 활성화했습니다.
- **예상 밖의 성능 하락**: 이론적으로는 가상 스레드 도입 시 동시 처리량이 극대화되어야 했으나, 실제 부하 테스트(JMeter)를 수행한 결과 특정 비동기 작업(`insightExecutor`, `emailExecutor`)에서 오히려 응답 지연이 늘어나거나 타임아웃 예외가 급증하는 이상 현상이 관찰되었습니다.

---

## 원인 분석 (Root Cause)

### 1. 가상 스레드 Pinning 현상 (Thread Pinning)
가상 스레드가 I/O 블로킹 작업을 만날 때 캐리어 스레드(Carrier Thread, 실제 CPU 스케줄링을 담당하는 플랫폼 스레드)를 OS에 반환하고 양보(yield)해야 합니다.
그러나, 자바 코드 내에 **synchronized** 블록/메서드 내부에서 블로킹 I/O 작업이 실행되거나 JNI(Java Native Interface) 콜과 같은 네이티브 프레임이 스택에 쌓여 있을 때는 가상 스레드가 캐리어 스레드에 **고정(Pinned)**되는 **Pinning 현상**이 발생합니다.
이로 인해 가상 스레드가 캐리어 스레드를 양보하지 못하고 캐리어 스레드 자체가 블로킹되어, ForkJoinPool의 모든 캐리어 스레드가 고갈되어 전체 애플리케이션이 멈추게 된 것입니다.

### 2. 프로젝트 내의 Pinning 지점 발견
JVM 기동 옵션에 `-Djdk.tracePinnedThreads=full`을 추가하고 부하 테스트를 재수행하여 상세 스택 트레이스를 추적했습니다.
- **원인 1**: 외부 이메일 연동 라이브러리 및 일부 레거시 로깅 유틸리티 내부에서 `synchronized` 블록을 사용한 동기화 처리 도중 이메일 소켓 I/O(SMTP 전송)가 일어나면서 캐리어 스레드가 통째로 락에 걸림.
- **원인 2**: 데이터베이스 커넥션 풀(HikariCP 5.x 미만 버전 등) 또는 JDBC 드라이버 내부의 일부 레거시 구간에서 `synchronized` 기반 커넥션 획득 락 점유 도중 DB I/O 지연이 발생하며 Pinning 유발.

---

## 해결 과정 (Resolution)

### 1. 세밀한 독립 제어를 위한 '토글(Toggle) 아키텍처' 설계
모든 비동기 스레드 풀에 전면적으로 Virtual Thread를 일괄 적용하면 일부 Pinning 유발 로직으로 인해 시스템 전체가 마비될 위험이 컸습니다. 이에 따라 각 비동기 기능별로 Virtual Thread 적용 여부를 정밀하게 켜고 끌 수 있는 **이중 토글(Double-Toggle) 설정**을 도입했습니다.

#### [VirtualThreadProperties.java]
```java
@Getter
@Setter
@ConfigurationProperties(prefix = "app.virtual-thread")
public class VirtualThreadProperties {
	private boolean enabled;        // 전체 가상 스레드 마스터 스위치
	private boolean insightEnabled; // 인사이트 비동기 전용 스위치
	private boolean emailEnabled;   // 이메일 비동기 전용 스위치
}
```

#### [InsightExecutorConfig.java]
```java
@Configuration
@EnableAsync
@EnableConfigurationProperties(VirtualThreadProperties.class)
public class InsightExecutorConfig {

	private final VirtualThreadProperties virtualThreadProperties;

	public InsightExecutorConfig(VirtualThreadProperties virtualThreadProperties) {
		this.virtualThreadProperties = virtualThreadProperties;
	}

	@Bean(name = "insightExecutor")
	public Executor insightExecutor() {
		if (isInsightVirtualThreadEnabled()) {
			return newVirtualThreadExecutor("insight-vt-");
		}
		// Pinning이나 락 경합 시 안전하게 Fallback할 전통적 스레드 풀 설계
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(10);
		executor.setThreadNamePrefix("insight-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.initialize();
		return executor;
	}
    // isInsightVirtualThreadEnabled(), emailExecutor() 로직...
}
```

### 2. synchronized 키워드의 현대화 및 라이브러리 교체
- 레거시 락 구조에서 Pinning이 확인된 구간은 Java Concurrency API인 **`ReentrantLock`**으로 대체하여, 락 획득 대기 상태에서도 가상 스레드가 정상적으로 캐리어 스레드를 yield할 수 있도록 리팩터링했습니다.
- HikariCP 커넥션 풀 라이브러리를 Virtual Thread 친화적인 버전으로 업데이트하여 커넥션 획득 과정에서의 락 점유 지연을 제거했습니다.

### 3. Virtual Thread 마운트/디스마운트 메커니즘 시각화

```mermaid
flowchart TD
    subgraph Platform Thread Pool (Classic)
        T1[Platform Thread 1] -->|Blocking I/O| OS_Wait[OS Thread Blocked]
        T2[Platform Thread 2] -->|Active| Task2[Task 2 Run]
    end

    subgraph Virtual Thread model (Optimized)
        subgraph Carrier Pool (ForkJoinPool)
            C1[Carrier Thread A]
            C2[Carrier Thread B]
        end
        VT1[Virtual Thread 1] -.->|1. Mount| C1
        VT1 -->|2. Blocking I/O| Yield[3. Yield Carrier Thread]
        Yield -->|4. Dismount| C1
        C1 -->|5. Reuse Carrier A| VT2[Virtual Thread 2 Mount]
    end

    subgraph Pinning Scenario (Problem)
        C_Pinned[Carrier Thread B]
        VT_Pin[Virtual Thread 3] -->|1. Mount| C_Pinned
        VT_Pin -->|2. synchronized block| Pinned[3. PINNED!]
        VT_Pin -->|4. Blocking I/O| BlockedCarrier[5. Carrier B Blocked! Cannot Yield]
    end
```

---

## 방지책 (Prevention)
1. **CI/CD 파이프라인 내 Pinning 감지 테스트**: 성능 테스트 프로파일(`perf`) 환경에서 자동화 벤치마크 수행 시 JVM 옵션에 `-XX:+TracePinnedThreads`를 기본 주입하여, 새로운 기능이 머지될 때 Pinning 로그가 잡히면 빌드를 격리하거나 경고 데몬을 띄우는 방어 장치를 고안했습니다.
2. **I/O 작업 격리 규칙**: 대용량 비동기 I/O를 다루는 스레드에서는 `synchronized` 사용을 금하고, 대신 `ReentrantLock` 혹은 동시성 자료구조(`ConcurrentHashMap` 등)를 사용하도록 개발 규약을 수립했습니다.

---

## 교훈 (Lessons Learned)
- Java 21의 가상 스레드는 마법의 탄환(Silver Bullet)이 아니며, 내부 아키텍처의 **스레드 핀(Pinning) 제약 조건**을 완벽히 인지하지 못하고 무작정 전면 도입할 경우 레거시 동기화 키워드로 인해 대형 장애가 날 수 있음을 경험했습니다.
- 성능 최적화 대상을 설정할 때 전체를 한번에 전환하지 않고, 분리 제어가 가능한 **토글식 아키텍처**를 사전에 촘촘히 설계한 덕에 장애 구간만 즉시 플랫폼 스레드로 Fallback하여 위기를 모면할 수 있었습니다.
