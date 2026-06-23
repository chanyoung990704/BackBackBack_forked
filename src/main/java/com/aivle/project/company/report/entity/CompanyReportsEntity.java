package com.aivle.project.company.report.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.company.report.quarter.entity.QuartersEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * company_reports 테이블에 매핑되는 보고서 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "company_reports")
public class CompanyReportsEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	private CompaniesEntity company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quarter_id", nullable = false)
	private QuartersEntity quarter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private PostsEntity post;

	/**
	 * 분기 보고서 생성.
	 */
	public static CompanyReportsEntity create(CompaniesEntity company, QuartersEntity quarter, PostsEntity post) {
		CompanyReportsEntity report = new CompanyReportsEntity();
		report.company = company;
		report.quarter = quarter;
		report.post = post;
		return report;
	}

}
