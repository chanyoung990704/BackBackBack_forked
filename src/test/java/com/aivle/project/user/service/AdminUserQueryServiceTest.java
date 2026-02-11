package com.aivle.project.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.user.dto.AdminUserListItemDto;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserQueryServiceTest {

	@InjectMocks
	private AdminUserQueryService adminUserQueryService;

	@Mock
	private UserRepository userRepository;

	@Test
	@DisplayName("활성 + 미삭제 사용자 목록을 반환한다")
	void getActiveUsers_shouldReturnActiveUsers() {
		// given
		List<AdminUserListItemDto> expected = List.of(
			new AdminUserListItemDto(1L, "홍길동", "hong@test.com"),
			new AdminUserListItemDto(2L, "김테스트", "kim@test.com")
		);
		given(userRepository.findListByStatusAndDeletedAtIsNullOrderByIdAscExcludingRole(
			UserStatus.ACTIVE,
			RoleName.ROLE_ADMIN
		))
			.willReturn(expected);

		// when
		List<AdminUserListItemDto> result = adminUserQueryService.getActiveUsers();

		// then
		assertThat(result).hasSize(2);
		assertThat(result).containsExactlyElementsOf(expected);
	}

	@Test
	@DisplayName("활성 사용자 userId는 검증을 통과한다")
	void validateActiveUser_shouldPassWhenActive() {
		// given
		given(userRepository.existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE))
			.willReturn(true);

		// when
		adminUserQueryService.validateActiveUser(1L);

		// then
		verify(userRepository).existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("활성 사용자가 아니면 COMMON_404를 반환한다")
	void validateActiveUser_shouldThrowWhenNotFound() {
		// given
		given(userRepository.existsByIdAndStatusAndDeletedAtIsNull(99L, UserStatus.ACTIVE))
			.willReturn(false);

		// when & then
		assertThatThrownBy(() -> adminUserQueryService.validateActiveUser(99L))
			.isInstanceOf(CommonException.class)
			.extracting(ex -> ((CommonException) ex).getErrorCode())
			.isEqualTo(CommonErrorCode.COMMON_404);
	}
}
