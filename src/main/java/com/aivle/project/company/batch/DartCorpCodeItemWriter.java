package com.aivle.project.company.batch;

import com.aivle.project.company.repository.CompaniesJdbcRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * DART 기업 코드 Writer.
 */
@Component
@RequiredArgsConstructor
public class DartCorpCodeItemWriter implements ItemWriter<DartCorpCodeItem> {

	private final CompaniesJdbcRepository companiesJdbcRepository;

	@Override
	public void write(Chunk<? extends DartCorpCodeItem> items) {
		// 1. 유효한 데이터 필터링 및 기업 코드 추출
		List<DartCorpCodeItem> candidates = new ArrayList<>();
		Set<String> corpCodes = new HashSet<>();

		for (DartCorpCodeItem item : items) {
			if (item == null || item.corpCode() == null) {
				continue;
			}
			candidates.add(item);
			corpCodes.add(item.corpCode());
		}

		if (candidates.isEmpty()) {
			return;
		}

		// 2. DB에 존재하는 기존 수정일 대량 조회
		Map<String, LocalDate> existingDates = companiesJdbcRepository.findModifyDatesByCorpCodes(List.copyOf(corpCodes));

		// 3. 신규 또는 수정된 기업 데이터 식별
		List<DartCorpCodeItem> toUpsert = new ArrayList<>();

		for (DartCorpCodeItem item : candidates) {
			LocalDate existingDate = existingDates.get(item.corpCode());
			LocalDate incomingDate = item.modifyDate();

			if (existingDate == null) {
				toUpsert.add(item);
				continue;
			}

			if (incomingDate != null && incomingDate.isAfter(existingDate)) {
				toUpsert.add(item);
			}
		}

		// 4. 최종 데이터 대량 저장/업데이트
		companiesJdbcRepository.upsertBatch(toUpsert);
	}
}
