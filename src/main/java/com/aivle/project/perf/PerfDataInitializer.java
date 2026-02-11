package com.aivle.project.perf;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * perf 프로파일 실행 시 벤치마크용 기본 데이터를 준비한다.
 */
@Slf4j
@Component
@Profile("perf")
@RequiredArgsConstructor
public class PerfDataInitializer implements ApplicationRunner {

	public static final String PERF_STOCK_CODE = "900001";
	public static final int PERF_BENCHMARK_YEAR = 2026;
	public static final int PERF_BENCHMARK_QUARTER = 1;
	private static final String PERF_CORP_CODE = "90000001";
	private static final String PERF_CORP_NAME = "PERF_MOCK_COMPANY";

	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final CompanyReportsRepository companyReportsRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		CompaniesEntity company = companiesRepository.findByStockCode(PERF_STOCK_CODE)
			.orElseGet(() -> companiesRepository.save(CompaniesEntity.create(
				PERF_CORP_CODE,
				PERF_CORP_NAME,
				"PERF MOCK COMPANY",
				PERF_STOCK_CODE,
				LocalDate.now()
			)));

		QuartersEntity quarter = quartersRepository.findByYearAndQuarter(
				(short) PERF_BENCHMARK_YEAR,
				(byte) PERF_BENCHMARK_QUARTER
			)
			.orElseGet(() -> {
				LocalDate startDate = LocalDate.of(PERF_BENCHMARK_YEAR, 1, 1);
				LocalDate endDate = startDate.plusMonths(3).minusDays(1);
				int quarterKey = PERF_BENCHMARK_YEAR * 10 + PERF_BENCHMARK_QUARTER;
				return quartersRepository.save(QuartersEntity.create(
					PERF_BENCHMARK_YEAR,
					PERF_BENCHMARK_QUARTER,
					quarterKey,
					startDate,
					endDate
				));
			});

		companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId())
			.orElseGet(() -> companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null)));

		log.info("perf 기본 데이터를 초기화했습니다. stockCode={}, benchmarkQuarter={}Q{}",
			PERF_STOCK_CODE, PERF_BENCHMARK_YEAR, PERF_BENCHMARK_QUARTER);
	}
}
