package com.aivle.project.common.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * dev 환경용 홈 페이지 컨트롤러.
 */
@Profile("dev")
@Controller
@Hidden
public class DevHomeController {

	@GetMapping("/")
	public String home() {
		return "dev-home";
	}
}
