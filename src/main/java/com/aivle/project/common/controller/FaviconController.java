package com.aivle.project.common.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * favicon 요청을 처리해 불필요한 예외 로그를 방지한다.
 */
@Controller
@Hidden
public class FaviconController {

	@GetMapping("/favicon.ico")
	public ResponseEntity<Void> favicon() {
		return ResponseEntity.noContent().build();
	}
}
