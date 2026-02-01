package com.aivle.project.common.controller;

import com.aivle.project.auth.service.TurnstileService;
import com.aivle.project.category.dto.CategorySummaryResponse;
import com.aivle.project.category.mapper.CategoryMapper;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * dev 환경 API 점검용 콘솔 페이지.
 */
@Profile("dev")
@Controller
@RequestMapping("/dev")
@Tag(name = "개발", description = "개발용 콘솔 페이지")
@RequiredArgsConstructor
public class DevConsoleController {

	private final TurnstileService turnstileService;
	private final CategoriesRepository categoriesRepository;
	private final CategoryMapper categoryMapper;

	@Value("${turnstile.site-key:}")
	private String turnstileSiteKey;

	@GetMapping("/console")
	@Operation(hidden = true)
	public String console() {
		return "api-console";
	}

	@GetMapping("/file-console")
	@Operation(hidden = true)
	public String fileConsole() {
		return "file-console";
	}

	@GetMapping("/report-predict-console")
	@Operation(hidden = true)
	public String reportPredictConsole() {
		return "report-predict-console";
	}

	@GetMapping("/categories")
	@ResponseBody
	@Operation(summary = "개발용 카테고리 조회", description = "개발 환경에서 카테고리를 조회합니다.", security = {})
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ApiResponse<List<CategorySummaryResponse>> listCategories() {
		List<CategorySummaryResponse> response = categoriesRepository
			.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
			.map(categoryMapper::toSummaryResponse)
			.toList();
		return ApiResponse.ok(response);
	}

	@GetMapping("/auth/console")
	@Operation(hidden = true)
	public String authConsole(Model model) {
		model.addAttribute("siteKey", turnstileSiteKey);
		return "auth-console";
	}

	@GetMapping("/auth/console/turnstile")
	@Operation(hidden = true)
	public String turnstileConsole(Model model) {
		model.addAttribute("siteKey", turnstileSiteKey);
		return "turnstile-console";
	}

	@PostMapping("/auth/console/turnstile/verify")
	@ResponseBody
	@Operation(hidden = true)
	public ApiResponse<Map<String, Object>> verifyTurnstile(
		@RequestBody(required = false) Map<String, String> payload,
		@Parameter(hidden = true) HttpServletRequest request
	) {
		String token = payload == null ? null : payload.get("token");
		String clientIp = resolveIp(request);
		boolean verified = turnstileService.verifyTokenSync(token, clientIp);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("verified", verified);
		response.put("tokenPreview", maskToken(token));
		response.put("remoteIp", clientIp);
		return ApiResponse.ok(response);
	}

	@GetMapping("/auth/console/claims")
	@ResponseBody
	@Operation(summary = "토큰 클레임 조회", description = "현재 토큰의 클레임을 확인합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ApiResponse<Map<String, Object>> claims(@AuthenticationPrincipal Jwt jwt) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("sub", jwt.getSubject());
		response.put("email", jwt.getClaimAsString("email"));
		response.put("roles", jwt.getClaimAsStringList("roles"));
		response.put("deviceId", jwt.getClaimAsString("deviceId"));
		response.put("issuer", jwt.getClaimAsString("iss"));
		response.put("jti", jwt.getId());
		response.put("issuedAt", formatInstant(jwt.getIssuedAt()));
		response.put("expiresAt", formatInstant(jwt.getExpiresAt()));
		return ApiResponse.ok(response);
	}

	private String formatInstant(Instant instant) {
		if (instant == null) {
			return null;
		}
		return instant.toString();
	}

	private String resolveIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwarded)) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String maskToken(String token) {
		if (!StringUtils.hasText(token)) {
			return null;
		}
		int prefixLength = Math.min(10, token.length());
		return token.substring(0, prefixLength) + "...";
	}
}
