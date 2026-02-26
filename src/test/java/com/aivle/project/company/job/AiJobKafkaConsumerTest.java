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
	@DisplayName("AI_REPORT 메시지는 report generation 서비스로 위임한다")
	void consume_aiReport_shouldDelegateToCompanyAiService() throws Exception {
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

		verify(companyAiService).processReportGeneration("req-1", 1L, 2026, 1);
	}

	@Test
	@DisplayName("유효하지 않은 payload는 런타임 예외를 던진다")
	void consume_invalidPayload_shouldThrowRuntimeException() {
		assertThatThrownBy(() -> consumer.consume("not-json"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process AI job");
	}
}
