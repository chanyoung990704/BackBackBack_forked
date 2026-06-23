package com.aivle.project.company.watchlist.repository;

import com.aivle.project.company.watchlist.entity.CompanyWatchlistEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyWatchlistRepository extends JpaRepository<CompanyWatchlistEntity, Long>, CompanyWatchlistRepositoryCustom {

	Optional<CompanyWatchlistEntity> findByUserIdAndCompanyId(Long userId, Long companyId);
	Optional<CompanyWatchlistEntity> findByUserIdAndCompanyIdAndDeletedAtIsNull(Long userId, Long companyId);
	Optional<CompanyWatchlistEntity> findByUserIdAndCompanyIdAndDeletedAtIsNotNull(Long userId, Long companyId);

	@Query("""
		select cw from CompanyWatchlistEntity cw
		join fetch cw.company c
		left join fetch c.industryCode
		where cw.user.id = :userId and cw.deletedAt is null
		order by cw.id desc
		""")
	List<CompanyWatchlistEntity> findActiveByUserId(@Param("userId") Long userId);
}
