package com.aivle.project.risk.service;

import com.aivle.project.risk.dto.RiskScoreBatchTargetProjection;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 모든 기업-분기의 최신 보고서 버전을 대상으로 위험도 요약을 배치 계산한다.
 */
@Service
@RequiredArgsConstructor
public class RiskScoreBatchService {

	private static final int DEFAULT_PAGE_SIZE = 500;

	private final CompanyReportVersionsRepository companyReportVersionsRepository;
	private final RiskScoreCalculationService riskScoreCalculationService;

	public int calculateAndUpsertAllLatest() {
		return calculateAndUpsertAllLatest(DEFAULT_PAGE_SIZE);
	}

	int calculateAndUpsertAllLatest(int pageSize) {
		int page = 0;
		int processed = 0;

		while (true) {
			Page<RiskScoreBatchTargetProjection> targets =
				companyReportVersionsRepository.findLatestRiskScoreTargets(PageRequest.of(page, pageSize));
			if (targets.isEmpty()) {
				break;
			}

			for (RiskScoreBatchTargetProjection target : targets.getContent()) {
				riskScoreCalculationService.calculateAndUpsert(
					target.getCompanyId(),
					target.getQuarterId(),
					target.getReportVersionId()
				);
				processed++;
			}

			if (!targets.hasNext()) {
				break;
			}
			page++;
		}

		return processed;
	}
}
