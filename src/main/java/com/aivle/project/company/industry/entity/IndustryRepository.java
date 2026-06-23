package com.aivle.project.company.industry.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndustryRepository extends JpaRepository<IndustryEntity, Long> {

    Optional<IndustryEntity> findByIndustryCode(String industryCode);

    List<IndustryEntity> findAllByIndustryName(String industryName);
}
