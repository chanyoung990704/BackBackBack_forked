package com.aivle.project.company.repository;

import com.aivle.project.company.entity.CompaniesEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 기업 조회/저장 리포지토리.
 */
public interface CompaniesRepository extends JpaRepository<CompaniesEntity, Long> {

	Optional<CompaniesEntity> findByStockCode(String stockCode);

	@Query("""
		select c
		from CompaniesEntity c
		where c.stockCode is not null
		  and (lower(c.corpName) like lower(concat('%', :keyword, '%'))
		       or lower(c.corpEngName) like lower(concat('%', :keyword, '%')))
		order by c.corpName asc
		""")
	List<CompaniesEntity> searchByKeywordExcludingNullStockCode(@Param("keyword") String keyword, Pageable pageable);

	@Query(
		value = """
			select c.*
			from companies c
			where c.stock_code is not null
			  and match(c.corp_name, c.corp_eng_name) against (:keyword in natural language mode)
			order by c.corp_name asc
			limit :limit
			""",
		nativeQuery = true
	)
	List<CompaniesEntity> searchByKeywordFullTextExcludingNullStockCode(@Param("keyword") String keyword, @Param("limit") int limit);
}
