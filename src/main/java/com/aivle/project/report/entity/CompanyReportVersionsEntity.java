package com.aivle.project.report.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.file.entity.FilesEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * company_report_versions 테이블에 매핑되는 보고서 버전 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "company_report_versions")
public class CompanyReportVersionsEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_report_id", nullable = false)
	private CompanyReportsEntity companyReport;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pdf_file_id")
	private FilesEntity pdfFile;

	@Column(name = "version_no", nullable = false)
	private int versionNo;

	@Column(name = "generated_at", nullable = false)
	private LocalDateTime generatedAt;

	@Column(name = "is_published", nullable = false)
	private boolean published;

	/**
	 * 보고서 버전 생성.
	 */
	public static CompanyReportVersionsEntity create(
		CompanyReportsEntity companyReport,
		int versionNo,
		LocalDateTime generatedAt,
		boolean published,
		FilesEntity pdfFile
	) {
		CompanyReportVersionsEntity version = new CompanyReportVersionsEntity();
		version.companyReport = companyReport;
		version.versionNo = versionNo;
		version.generatedAt = generatedAt;
		version.published = published;
		version.pdfFile = pdfFile;
		return version;
	}

	/**
	 * PDF를 연결하고 발행 상태로 전환한다.
	 */
	public void publishWithPdf(FilesEntity pdfFile) {
		this.pdfFile = pdfFile;
		this.published = true;
	}

	/**
	 * PDF만 연결한다.
	 */
	public void attachPdf(FilesEntity pdfFile) {
		this.pdfFile = pdfFile;
	}

	/**
	 * 발행 상태로 전환한다.
	 */
	public void publish() {
		this.published = true;
	}
}
