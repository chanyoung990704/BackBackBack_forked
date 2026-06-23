package com.aivle.project.company.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * DART 기업 코드 Processor.
 */
@Component
public class DartCorpCodeItemProcessor implements ItemProcessor<DartCorpCodeItem, DartCorpCodeItem> {

	@Override
	public DartCorpCodeItem process(@NonNull DartCorpCodeItem item) {
		if (item == null || !StringUtils.hasText(item.corpCode())) {
			return null;
		}
		return new DartCorpCodeItem(
			normalize(item.corpCode()),
			normalize(item.corpName()),
			normalize(item.corpEngName()),
			normalize(item.stockCode()),
			item.modifyDate()
		);
	}

	private String normalize(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
