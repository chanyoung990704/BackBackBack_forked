package com.aivle.project.user.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.user.dto.AdminUserListItemDto;
import com.aivle.project.user.service.AdminUserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 사용자 조회 API.
 */
@Tag(name = "Admin User Management", description = "관리자용 사용자 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final AdminUserQueryService adminUserQueryService;

	@GetMapping
	@Operation(summary = "관리자 사용자 목록 조회", description = "활성 + 미삭제 사용자 목록을 id/name/email로 조회합니다.")
	public ResponseEntity<ApiResponse<List<AdminUserListItemDto>>> getUsers() {
		List<AdminUserListItemDto> response = adminUserQueryService.getActiveUsers();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
