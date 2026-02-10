package com.aivle.project.user.service;

import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.user.dto.AdminUserListItemDto;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 사용자 조회/검증 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserQueryService {

	private final UserRepository userRepository;

	/**
	 * 활성 + 미삭제 사용자 목록을 반환한다.
	 */
	public List<AdminUserListItemDto> getActiveUsers() {
		return userRepository.findListByStatusAndDeletedAtIsNullOrderByIdAsc(UserStatus.ACTIVE);
	}

	/**
	 * 조회 대상 userId가 활성 사용자인지 검증한다.
	 */
	public void validateActiveUser(Long userId) {
		if (userId == null) {
			throw new CommonException(CommonErrorCode.COMMON_400);
		}
		boolean exists = userRepository.existsByIdAndStatusAndDeletedAtIsNull(userId, UserStatus.ACTIVE);
		if (!exists) {
			throw new CommonException(CommonErrorCode.COMMON_404);
		}
	}
}
