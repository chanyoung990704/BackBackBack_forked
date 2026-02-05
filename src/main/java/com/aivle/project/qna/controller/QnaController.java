package com.aivle.project.qna.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.qna.dto.QaPostCreateRequest;
import com.aivle.project.qna.dto.QaPostResponse;
import com.aivle.project.qna.service.QnaService;
import com.aivle.project.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Q&A (사용자)", description = "사용자용 Q&A API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qna")
@SecurityRequirement(name = "bearerAuth")
public class QnaController {

	private final QnaService qnaService;

	@GetMapping
	@Operation(summary = "내 Q&A 목록 조회")
	public ResponseEntity<ApiResponse<List<QaPostResponse>>> listMyQna(@CurrentUser UserEntity user) {
		return ResponseEntity.ok(ApiResponse.ok(qnaService.listMyQna(user)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Q&A 상세 조회")
	public ResponseEntity<ApiResponse<QaPostResponse>> get(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(qnaService.get(id)));
	}

	@PostMapping
	@Operation(summary = "Q&A 질문 생성")
	public ResponseEntity<ApiResponse<QaPostResponse>> create(
		@CurrentUser UserEntity user,
		@Valid @RequestBody QaPostCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(qnaService.create(user, request)));
	}
}
