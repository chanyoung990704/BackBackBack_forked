package com.aivle.project.common.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * dev 환경 API 점검용 콘솔 페이지.
 */
@Profile("dev")
@Controller
@RequestMapping("/dev")
public class DevConsoleController {

	@GetMapping("/console")
	public String console() {
		return "api-console";
	}

	@GetMapping("/file-console")
	public String fileConsole() {
		return "file-console";
	}
}
