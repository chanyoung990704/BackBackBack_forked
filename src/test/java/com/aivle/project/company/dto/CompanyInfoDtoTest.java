package com.aivle.project.company.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompanyInfoDtoTest {

	@Test
	@DisplayName("CompanyInfoDto 필드가 정상적으로 생성된다")
	void createCompanyInfoDto() {
		// given
		CompanySectorDto sector = new CompanySectorDto("", "금융");

		// when
		CompanyInfoDto dto = new CompanyInfoDto(
			1L,
			"테스트기업",
			"000020",
			sector,
			75.5,
			"SAFE",
			80.0,
			65.0
		);

		// then
		assertThat(dto.getId()).isEqualTo(1L);
		assertThat(dto.getName()).isEqualTo("테스트기업");
		assertThat(dto.getStockCode()).isEqualTo("000020");
		assertThat(dto.getSector()).isEqualTo(sector);
		assertThat(dto.getOverallScore()).isEqualTo(75.5);
		assertThat(dto.getRiskLevel()).isEqualTo("SAFE");
		assertThat(dto.getNetworkHealth()).isEqualTo(80.0);
		assertThat(dto.getReputationScore()).isEqualTo(65.0);
	}
}
