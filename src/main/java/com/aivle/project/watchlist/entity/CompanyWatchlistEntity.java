package com.aivle.project.watchlist.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.user.entity.UserEntity;
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

/** 사용자 관심 기업 엔티티. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "company_watchlists")
public class CompanyWatchlistEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	private CompaniesEntity company;

	@Column(name = "note", length = 255)
	private String note;

	public static CompanyWatchlistEntity create(UserEntity user, CompaniesEntity company, String note) {
		CompanyWatchlistEntity entity = new CompanyWatchlistEntity();
		entity.user = user;
		entity.company = company;
		entity.note = note;
		return entity;
	}

	public void updateNote(String note) {
		this.note = note;
	}
}
