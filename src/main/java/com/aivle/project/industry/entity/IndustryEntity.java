package com.aivle.project.industry.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 *  industry_code 테이블에 매핑되는 산업(업종) 분류 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "industry_codes")
public class IndustryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "industry_code", nullable = false, length = 5)
    private String industryCode;

    @Column(name = "industry_name", length = 100)
    private String industryName;

    public static IndustryEntity create(String industryCode, String industryName) {
        IndustryEntity entity = new IndustryEntity();
        entity.industryCode = industryCode;
        entity.industryName = industryName;
        return entity;
    }
}