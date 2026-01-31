package com.aivle.project.common.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 헬스체크 테스트용 컨트롤러.
 */
@RestController
public class TestHealthController {

	@GetMapping("/actuator/health")
	public String health() {
		return "ok";
	}
}
