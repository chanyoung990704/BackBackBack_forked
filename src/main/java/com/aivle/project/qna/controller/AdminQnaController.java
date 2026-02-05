package com.aivle.project.qna.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.qna.dto.QaPostResponse;
import com.aivle.project.qna.dto.QaReplyCreateRequest;
import com.aivle.project.qna.dto.QaReplyResponse;
import com.aivle.project.qna.service.QnaService;
import com.aivle.project.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Q&A (관리자)", description = "관리자용 Q&A API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/qna")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQnaController {

	private final QnaService qnaService;

	@GetMapping
	@Operation(summary = "전체 Q&A 목록 조회 (관리자용)")
	public ResponseEntity<ApiResponse<List<QaPostResponse>>> listAll() {
		return ResponseEntity.ok(ApiResponse.ok(qnaService.listAll()));
	}

	@PostMapping("/{id}/replies")
	@Operation(summary = "Q&A 답변 추가 (관리자용)")
	public ResponseEntity<ApiResponse<QaReplyResponse>> addReply(
		@CurrentUser UserEntity admin,
		@PathVariable Long id,
		@Valid @RequestBody QaReplyCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(qnaService.addReply(admin, id, request)));
	}
}
