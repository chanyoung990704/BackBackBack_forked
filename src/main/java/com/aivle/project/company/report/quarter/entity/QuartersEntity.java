package com.aivle.project.company.report.quarter.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * quarters 테이블에 매핑되는 분기 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "quarters")
public class QuartersEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "year", nullable = false)
	private short year;

	@Column(name = "quarter", nullable = false)
	private byte quarter;

	@Column(name = "quarter_key", nullable = false)
	private int quarterKey;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	/**
	 * 분기 엔티티 생성.
	 */
	public static QuartersEntity create(int year, int quarter, int quarterKey, LocalDate startDate, LocalDate endDate) {
		QuartersEntity entity = new QuartersEntity();
		entity.year = (short) year;
		entity.quarter = (byte) quarter;
		entity.quarterKey = quarterKey;
		entity.startDate = startDate;
		entity.endDate = endDate;
		return entity;
	}
}
