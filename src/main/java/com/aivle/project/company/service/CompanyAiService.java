package com.aivle.project.company.service;

import com.aivle.project.common.util.SimpleMultipartFile;
import com.aivle.project.common.util.GetOrCreateResolver;
import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import com.aivle.project.report.service.CompanyReportVersionIssueService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAiService {

    private final AiServerClient aiServerClient;
    private final FileStorageService fileStorageService;
    private final FilesRepository filesRepository;
    private final CompaniesRepository companiesRepository;
    private final QuartersRepository quartersRepository;
    private final CompanyReportsRepository companyReportsRepository;
    private final CompanyReportVersionsRepository companyReportVersionsRepository;
    private final MetricsRepository metricsRepository;
    private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
    private final AiReportRequestStatusService aiReportRequestStatusService;
    private final CompanyReportVersionIssueService companyReportVersionIssueService;

    /**
     * 특정 기업의 AI 재무 분석 예측 결과를 조회하고 저장합니다.
     * DB에 해당 분기의 예측치가 이미 존재하면 DB 값을 반환하고,
     * 없으면 AI 서버를 호출하여 새로 분석 및 저장합니다.
     *
     * @param companyId 기업 ID
     * @param year      연도 (선택)
     * @param quarter   분기 (선택)
     * @return 예측 결과 DTO
     */
    @Transactional
    public AiAnalysisResponse getCompanyAnalysis(Long companyId, Integer year, Integer quarter) {
        log.info("Fetching AI analysis for companyId: {}, year: {}, quarter: {}", companyId, year, quarter);

        // 1. 기업 존재 확인
        CompaniesEntity company = companiesRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + companyId));

        int targetQuarterKey;
        String basePeriod = null;

        if (year != null && quarter != null) {
            // 사용자가 명시적으로 분기를 지정한 경우
            targetQuarterKey = year * 10 + quarter;
            // 지정된 분기의 이전 분기를 basePeriod로 가정 (단순화)
            int bYear = year;
            int bQuarter = quarter - 1;
            if (bQuarter == 0) {
                bYear -= 1;
                bQuarter = 4;
            }
            basePeriod = String.valueOf(bYear * 10 + bQuarter);
        } else {
            // 미지정 시 가장 최근 실제 데이터(ACTUAL) 분기 확인
            Optional<Integer> maxActualQuarterKey = companyReportMetricValuesRepository.findMaxActualQuarterKeyByCompanyId(companyId);
            if (maxActualQuarterKey.isPresent()) {
                int baseQuarterKey = maxActualQuarterKey.get();
                basePeriod = String.valueOf(baseQuarterKey);
                // 다음 분기 계산
                int targetYear = baseQuarterKey / 10;
                int tQuarter = baseQuarterKey % 10 + 1;
                if (tQuarter > 4) {
                    targetYear += 1;
                    tQuarter = 1;
                }
                targetQuarterKey = targetYear * 10 + tQuarter;
            } else {
                // 실적 데이터가 전혀 없는 경우 AI 서버에 위임
                log.info("No actual data found for company {}. Calling AI server directly.", companyId);
                AiAnalysisResponse response = aiServerClient.getPrediction(company.getStockCode());
                saveAiPredictions(company.getId(), response);
                return response;
            }
        }

        // 2. 해당 타겟 분기에 대한 최신 예측치 조회 (DB Hit 확인)
        var latestPredictions = companyReportMetricValuesRepository.findLatestMetricsByCompanyIdAndQuarterKeyAndType(
            companyId, targetQuarterKey, MetricValueType.PREDICTED);

        if (!latestPredictions.isEmpty()) {
            log.info("Found existing AI predictions in DB for quarterKey: {}", targetQuarterKey);

            Map<String, Double> predictionMap = latestPredictions.stream()
                .collect(Collectors.toMap(
                    p -> p.getMetricCode(),
                    p -> p.getMetricValue().doubleValue()
                ));

            return new AiAnalysisResponse(
                company.getStockCode(),
                company.getCorpName(),
                basePeriod,
                predictionMap
            );
        }

        // 3. DB에 없으면 AI 서버 호출 및 저장
        log.info("No existing predictions found for target quarter {}. Calling AI server...", targetQuarterKey);
        AiAnalysisResponse response = aiServerClient.getPrediction(company.getStockCode());
        saveAiPredictions(company.getId(), response);

        return response;
    }

    /**
     * AI 예측 결과를 DB에 저장합니다.
     * base_period의 다음 분기를 타겟으로 하여 PREDICTED 타입으로 저장합니다.
     */
    private void saveAiPredictions(Long companyId, AiAnalysisResponse response) {
        if (response == null || response.predictions() == null) {
            return;
        }

        try {
            // 1. Base Period 파싱 및 타겟 분기(다음 분기) 계산
            String basePeriod = response.basePeriod(); // e.g., "20253"
            int baseYear = Integer.parseInt(basePeriod.substring(0, 4));
            int baseQuarter = Integer.parseInt(basePeriod.substring(4));

            int nextYear = baseYear;
            int nextQuarter = baseQuarter + 1;
            if (nextQuarter > 4) {
                nextYear += 1;
                nextQuarter = 1;
            }
            final int targetYear = nextYear;
            final int targetQuarter = nextQuarter;

            log.info("Saving AI predictions for companyId={} based on {} -> Target: {}/{}", companyId, basePeriod, targetYear, targetQuarter);

            // 2. 기업 조회
            CompaniesEntity company = companiesRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + companyId));

            // 3. 타겟 분기 조회 또는 생성
            QuartersEntity targetQuarterEntity = getOrCreateQuarter(targetYear, targetQuarter);

            // 4. 리포트 조회 또는 생성
            CompanyReportsEntity report = getOrCreateReport(company, targetQuarterEntity);

            // 5. 새 리포트 버전 생성 (report row 잠금으로 version_no 충돌 방지)
            CompanyReportVersionsEntity version = companyReportVersionIssueService.issueNextVersion(report, true, null);

            // 6. 지표 매핑 및 값 저장
            Map<String, Double> predictions = response.predictions();
            List<MetricsEntity> metrics = metricsRepository.findAllByMetricCodeIn(predictions.keySet());
            Map<String, MetricsEntity> metricMap = metrics.stream()
                .collect(Collectors.toMap(MetricsEntity::getMetricCode, Function.identity()));

            for (Map.Entry<String, Double> entry : predictions.entrySet()) {
                String metricCode = entry.getKey();
                Double value = entry.getValue();

                MetricsEntity metric = metricMap.get(metricCode);
                if (metric != null && value != null) {
                    CompanyReportMetricValuesEntity metricValue = CompanyReportMetricValuesEntity.create(
                        version,
                        metric,
                        targetQuarterEntity,
                        BigDecimal.valueOf(value),
                        MetricValueType.PREDICTED
                    );
                    companyReportMetricValuesRepository.save(metricValue);
                }
            }
            log.info("Saved {} prediction metrics for companyId {}", predictions.size(), companyId);

        } catch (Exception e) {
            log.error("Failed to save AI predictions for companyId {}", companyId, e);
            // 저장 실패가 조회 응답에 영향을 주지 않도록 예외를 삼킴 (선택 사항이나 안전을 위해)
        }
    }

    /**
     * AI 서버에서 분석 리포트(PDF)를 생성하고 파일 서버에 저장합니다.
     * 항상 특정 분기의 보고서 버전으로 등록하며, 연도와 분기가 제공되지 않으면 현재 날짜 기준의 최신 분기를 사용합니다.
     * 해당 분기가 DB에 없으면 새로 생성합니다.
     * @param companyId 기업 ID
     * @param year       연도 (선택)
     * @param quarter    분기 (선택)
     * @return 저장된 파일 엔티티
     */
    @Transactional
    public FilesEntity generateAndSaveReport(Long companyId, Integer year, Integer quarter) {
        log.info("Generating and saving AI report for companyId: {}, year: {}, quarter: {}", companyId, year, quarter);

        // 1. 기업 조회
        CompaniesEntity company = companiesRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + companyId));

        // 2. 연도와 분기 결정 (미입력 시 현재 날짜 기준)
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        int targetQuarter = (quarter != null) ? quarter : ((LocalDate.now().getMonthValue() - 1) / 3 + 1);

        // 3. AI 서버에서 PDF 다운로드
        byte[] pdfContent = aiServerClient.getAnalysisReportPdf(company.getStockCode());

        // 4. MultipartFile 생성
        String filename = String.format("report_%s_%d_%d_%s.pdf", company.getStockCode(), targetYear, targetQuarter, LocalDate.now());
        MultipartFile multipartFile = new SimpleMultipartFile(
            "file",
            filename,
            "application/pdf",
            pdfContent
        );

        // 5. 파일 저장 (로컬 또는 S3)
        // 경로 구조: reports/{stockCode}/{year}/{quarter}
        String subDir = String.format("reports/%s/%d/%d", company.getStockCode(), targetYear, targetQuarter);
        StoredFile storedFile = fileStorageService.store(multipartFile, subDir);

        // 6. DB에 파일 메타데이터 저장
        FilesEntity filesEntity = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            storedFile.storageUrl(),
            storedFile.storageKey(),
            storedFile.originalFilename(),
            storedFile.fileSize(),
            storedFile.contentType()
        );
        FilesEntity savedFileEntity = filesRepository.save(filesEntity);

        // 7. 분기 조회 또는 생성
        QuartersEntity quarterEntity = getOrCreateQuarter(targetYear, targetQuarter);

        // 8. 기업-분기 보고서 조회 또는 생성
        CompanyReportsEntity report = getOrCreateReport(company, quarterEntity);

        // 9. 새 버전 등록 (report row 잠금으로 version_no 충돌 방지)
        CompanyReportVersionsEntity version = companyReportVersionIssueService.issueNextVersion(report, true, savedFileEntity);
        log.info("Linked AI report to company_report_versions (ID: {}, Year: {}, Quarter: {}, Version: {})",
            report.getId(), targetYear, targetQuarter, version.getVersionNo());

        return savedFileEntity;
    }

    /**
     * 특정 기업, 연도, 분기의 최신 AI 리포트 파일을 조회합니다.
     *
     * @param companyCode 기업 코드
     * @param year        연도
     * @param quarter     분기
     * @return 파일 엔티티
     * @throws IllegalArgumentException 파일이 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    public FilesEntity getReportFile(String companyCode, Integer year, Integer quarter) {
        if (year == null || quarter == null) {
            throw new IllegalArgumentException("연도와 분기는 필수입니다.");
        }

        String stockCode = resolveStockCode(companyCode);
        CompaniesEntity company = companiesRepository.findByStockCode(stockCode)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 코드입니다: " + stockCode));

        QuartersEntity quarterEntity = quartersRepository.findByYearAndQuarter(year.shortValue(), quarter.byteValue())
            .orElseThrow(() -> new IllegalArgumentException("해당 연도/분기 정보가 존재하지 않습니다."));

        CompanyReportsEntity report = companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarterEntity.getId())
            .orElseThrow(() -> new IllegalArgumentException("해당 분기의 리포트가 존재하지 않습니다."));

        CompanyReportVersionsEntity version = companyReportVersionsRepository
            .findTopByCompanyReportAndPdfFileIsNotNullOrderByVersionNoDesc(report)
            .orElseThrow(() -> new IllegalArgumentException("해당 분기의 PDF 리포트가 존재하지 않습니다."));

        FilesEntity file = version.getPdfFile();
        if (file == null) {
            throw new IllegalArgumentException("리포트 버전은 존재하나 파일이 연결되어 있지 않습니다.");
        }
        // Force initialization to prevent LazyInitializationException
        file.getStorageUrl();

        return file;
    }

    /**
     * 특정 기업 ID, 연도, 분기의 최신 AI 리포트 파일을 조회합니다.
     *
     * @param companyId 기업 ID
     * @param year      연도
     * @param quarter   분기
     * @return 파일 엔티티
     * @throws IllegalArgumentException 파일이 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    public FilesEntity getReportFileById(Long companyId, Integer year, Integer quarter) {
        if (year == null || quarter == null) {
            throw new IllegalArgumentException("연도와 분기는 필수입니다.");
        }

        CompaniesEntity company = companiesRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + companyId));

        QuartersEntity quarterEntity = quartersRepository.findByYearAndQuarter(year.shortValue(), quarter.byteValue())
            .orElseThrow(() -> new IllegalArgumentException("해당 연도/분기 정보가 존재하지 않습니다."));

        CompanyReportsEntity report = companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarterEntity.getId())
            .orElseThrow(() -> new IllegalArgumentException("해당 분기의 리포트가 존재하지 않습니다."));

        CompanyReportVersionsEntity version = companyReportVersionsRepository
            .findTopByCompanyReportAndPdfFileIsNotNullOrderByVersionNoDesc(report)
            .orElseThrow(() -> new IllegalArgumentException("해당 분기의 PDF 리포트가 존재하지 않습니다."));

        FilesEntity file = version.getPdfFile();
        if (file == null) {
            throw new IllegalArgumentException("리포트 버전은 존재하나 파일이 연결되어 있지 않습니다.");
        }
        // Force initialization to prevent LazyInitializationException
        file.getStorageUrl();

        return file;
    }

    private QuartersEntity createNewQuarter(int year, int quarter) {
        log.info("Creating new quarter: {} year {} quarter", year, quarter);
        int quarterKey = year * 10 + quarter;
        LocalDate startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate endDate = startDate.plusMonths(3).minusDays(1);

        QuartersEntity newQuarter = QuartersEntity.create(year, quarter, quarterKey, startDate, endDate);
        return quartersRepository.save(newQuarter);
    }

    private QuartersEntity getOrCreateQuarter(int year, int quarter) {
        return GetOrCreateResolver.resolve(
            () -> quartersRepository.findByYearAndQuarter((short) year, (byte) quarter),
            () -> createNewQuarter(year, quarter),
            () -> quartersRepository.findByYearAndQuarter((short) year, (byte) quarter)
        );
    }

    private CompanyReportsEntity getOrCreateReport(CompaniesEntity company, QuartersEntity quarter) {
        return GetOrCreateResolver.resolve(
            () -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId()),
            () -> companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null)),
            () -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId())
        );
    }

    /**
     * AI 리포트 생성을 비동기로 실행합니다.
     * 이미 해당 기업+분기의 보고서가 있으면 바로 COMPLETED, 없으면 새로 생성 후 COMPLETED.
     *
     * @param requestId 요청 ID
     * @param companyId 기업 ID
     * @param year 연도 (미지정 시 가장 최근 ACTUAL 분기의 다음 분기)
     * @param quarter 분기 (미지정 시 가장 최근 ACTUAL 분기의 다음 분기)
     */
    @Async("insightExecutor")
    @Transactional
    public void generateReportAsync(String requestId, Long companyId, Integer year, Integer quarter) {
        log.info("Starting async report generation for requestId: {}, companyId: {}", requestId, companyId);

        try {
            // 기업 조회
            CompaniesEntity company = companiesRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + companyId));

            // 연도와 분기 결정 (미지정 시 가장 최근 ACTUAL 분기의 다음 분기)
            int targetYear;
            int targetQuarter;

            if (year != null && quarter != null) {
                targetYear = year;
                targetQuarter = quarter;
            } else {
                // 가장 최근 ACTUAL 분기 조회
                Optional<Integer> maxActualQuarterKey = companyReportMetricValuesRepository.findMaxActualQuarterKeyByCompanyId(companyId);
                if (maxActualQuarterKey.isPresent()) {
                    int baseQuarterKey = maxActualQuarterKey.get();
                    int baseYear = baseQuarterKey / 10;
                    int baseQ = baseQuarterKey % 10;

                    // 다음 분기 계산
                    targetYear = baseQ == 4 ? baseYear + 1 : baseYear;
                    targetQuarter = baseQ == 4 ? 1 : baseQ + 1;
                } else {
                    // ACTUAL 데이터가 없으면 현재 날짜 기준
                    targetYear = LocalDate.now().getYear();
                    targetQuarter = ((LocalDate.now().getMonthValue() - 1) / 3 + 1);
                }
            }

            log.info("Target quarter for report: year={}, quarter={}", targetYear, targetQuarter);

            String downloadUrl = "/api/companies/" + companyId + "/ai-report/download?year=" + targetYear + "&quarter=" + targetQuarter;
            // 해당 기업+분기의 PDF 리포트가 이미 존재하면 바로 COMPLETED 처리
            try {
                FilesEntity existingFile = getReportFileById(companyId, targetYear, targetQuarter);
                log.info("Report already exists for companyId: {}, year: {}, quarter: {}", companyId, targetYear, targetQuarter);
                aiReportRequestStatusService.updateCompleted(requestId, String.valueOf(existingFile.getId()), downloadUrl);
                log.info("Completed async report generation for requestId: {} (existing file)", requestId);
                return;
            } catch (IllegalArgumentException e) {
                log.info("Report not found. Generating new report for companyId: {}, year: {}, quarter: {}", companyId, targetYear, targetQuarter);
            }

            // 보고서가 없을 때만 PROCESSING -> 생성 -> COMPLETED 순서로 처리
            aiReportRequestStatusService.updateProcessing(requestId);
            FilesEntity file = generateAndSaveReport(companyId, targetYear, targetQuarter);
            aiReportRequestStatusService.updateCompleted(requestId, String.valueOf(file.getId()), downloadUrl);

            log.info("Completed async report generation for requestId: {}", requestId);

        } catch (Exception e) {
            log.error("Failed async report generation for requestId: {}", requestId, e);
            aiReportRequestStatusService.updateFailed(requestId, e.getMessage());
        }
    }

    /**
     * companyCode가 ID인지 stock_code인지 판단하여 stock_code를 반환합니다.
     * @param companyCode 기업 ID 또는 Stock Code
     * @return Stock Code
     */
    private String resolveStockCode(String companyCode) {
        if (companyCode == null || companyCode.isEmpty()) {
            throw new IllegalArgumentException("companyCode는 필수입니다.");
        }

        // 숫자만으로 이루어지면 ID로 간주
        if (companyCode.matches("\\d+")) {
            return companiesRepository.findById(Long.parseLong(companyCode))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + companyCode))
                .getStockCode();
        }

        // 그렇지 않으면 stock_code로 간주
        return companyCode;
    }
}
