package com.aivle.project.company.service;

import com.aivle.project.company.dto.CompanyIndustryImportDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.util.ExcelCompanyIndustryParser;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyIndustryImportService {

    private final CompaniesRepository companiesRepository;
    private final IndustryRepository industryRepository;
    private final ExcelCompanyIndustryParser excelParser;

    @Transactional
    public void importCompanyIndustries(MultipartFile file) throws IOException {
        List<CompanyIndustryImportDto> dtos = excelParser.parse(file.getInputStream());
        log.info("Parsing complete. Found {} records to process.", dtos.size());

        int updateCount = 0;
        int skipCount = 0;

        for (CompanyIndustryImportDto dto : dtos) {
            log.debug("Processing dto: stockCode={}, industryCode={}", dto.getStockCode(), dto.getIndustryCode());
            Optional<CompaniesEntity> companyOpt = companiesRepository.findByStockCode(dto.getStockCode());
            
            if (companyOpt.isPresent()) {
                CompaniesEntity company = companyOpt.get();
                Optional<IndustryEntity> industryOpt = industryRepository.findByIndustryCode(dto.getIndustryCode());
                
                if (industryOpt.isPresent()) {
                    company.updateIndustryCode(industryOpt.get());
                    updateCount++;
                    log.debug("Successfully updated company {} with industry {}", dto.getStockCode(), dto.getIndustryCode());
                } else {
                    log.warn("Industry code not found in DB: '{}' for company '{}'", dto.getIndustryCode(), dto.getStockCode());
                    skipCount++;
                }
            } else {
                log.warn("Company stock code not found in DB: '{}'", dto.getStockCode());
                skipCount++;
            }
        }

        log.info("Import finished. Updated: {}, Skipped: {}", updateCount, skipCount);
    }
}
