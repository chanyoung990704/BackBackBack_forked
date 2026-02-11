package com.aivle.project.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class InsightExecutorConfigTest {

	@Test
	@DisplayName("Virtual Thread 비활성 시 insightExecutor는 플랫폼 스레드에서 동작한다")
	void insightExecutor_shouldUsePlatformThreadWhenVirtualThreadDisabled() throws Exception {
		// given
		VirtualThreadProperties properties = new VirtualThreadProperties();
		properties.setEnabled(false);
		properties.setInsightEnabled(false);
		properties.setEmailEnabled(false);
		InsightExecutorConfig config = new InsightExecutorConfig(properties);
		Executor executor = config.insightExecutor();

		try {
			// when
			boolean virtual = isVirtualThread(executor);

			// then
			assertThat(virtual).isFalse();
		} finally {
			shutdown(executor);
		}
	}

	@Test
	@DisplayName("Virtual Thread 전역 활성 시 insightExecutor는 가상 스레드에서 동작한다")
	void insightExecutor_shouldUseVirtualThreadWhenGlobalVirtualThreadEnabled() throws Exception {
		// given
		VirtualThreadProperties properties = new VirtualThreadProperties();
		properties.setEnabled(true);
		properties.setInsightEnabled(false);
		properties.setEmailEnabled(false);
		InsightExecutorConfig config = new InsightExecutorConfig(properties);
		Executor executor = config.insightExecutor();

		try {
			// when
			boolean virtual = isVirtualThread(executor);

			// then
			assertThat(virtual).isTrue();
		} finally {
			shutdown(executor);
		}
	}

	@Test
	@DisplayName("email 전용 토글 활성 시 emailExecutor는 가상 스레드에서 동작한다")
	void emailExecutor_shouldUseVirtualThreadWhenEmailToggleEnabled() throws Exception {
		// given
		VirtualThreadProperties properties = new VirtualThreadProperties();
		properties.setEnabled(false);
		properties.setInsightEnabled(false);
		properties.setEmailEnabled(true);
		InsightExecutorConfig config = new InsightExecutorConfig(properties);
		Executor executor = config.emailExecutor();

		try {
			// when
			boolean virtual = isVirtualThread(executor);

			// then
			assertThat(virtual).isTrue();
		} finally {
			shutdown(executor);
		}
	}

	private boolean isVirtualThread(Executor executor) throws InterruptedException {
		AtomicReference<Boolean> result = new AtomicReference<>(null);
		CountDownLatch latch = new CountDownLatch(1);
		executor.execute(() -> {
			result.set(Thread.currentThread().isVirtual());
			latch.countDown();
		});
		assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
		return Boolean.TRUE.equals(result.get());
	}

	private void shutdown(Executor executor) {
		if (executor instanceof ThreadPoolTaskExecutor threadPoolTaskExecutor) {
			threadPoolTaskExecutor.shutdown();
			return;
		}
		if (executor instanceof ExecutorService executorService) {
			executorService.shutdownNow();
		}
	}
}
