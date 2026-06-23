package com.aivle.project.company.job;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.aivle.project.company.service.CompanyAiCommentService;
import com.aivle.project.company.service.CompanyAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiJobKafkaConsumerTest {

	@InjectMocks
	private AiJobKafkaConsumer consumer;

	@Mock
	private CompanyAiService companyAiService;

	@Mock
	private CompanyAiCommentService companyAiCommentService;

	@Mock
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("AI_REPORT 메시지는 Java 웹서버 동기 실행을 스킵하고 성공적으로 종료된다")
	void consume_aiReport_shouldSkipJavaSynchronousPath() throws Exception {
		AiJobMessage message = new AiJobMessage(
			"req-1",
			AiJobType.AI_REPORT,
			1L,
			2026,
			1,
			null,
			OffsetDateTime.now()
		);

		doReturn(message).when(objectMapper).readValue("payload", AiJobMessage.class);
		consumer.consume("payload");

		org.mockito.Mockito.verifyNoInteractions(companyAiService);
	}

	@Test
	@DisplayName("유효하지 않은 payload는 런타임 예외를 던진다")
	void consume_invalidPayload_shouldThrowRuntimeException() {
		assertThatThrownBy(() -> consumer.consume("not-json"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process AI job");
	}
}
