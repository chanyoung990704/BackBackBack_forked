package com.aivle.project.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * API 요청/응답 로깅을 위한 Aspect.
 * 포인트컷 정의 및 비동기 로깅 프로세서 호출을 담당함.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiLoggingAspect {

	private final ApiLogProcessor apiLogProcessor;

	@Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
	public void restController() {}

	@Around("restController()")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!log.isInfoEnabled()) {
			return joinPoint.proceed();
		}

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		String requestId = UUID.randomUUID().toString();
		MDC.put("requestId", requestId);

		long start = System.currentTimeMillis();
		Object result = null;
		Throwable exception = null;

		try {
			result = joinPoint.proceed();
			return result;
		} catch (Throwable e) {
			exception = e;
			throw e;
		} finally {
			long time = System.currentTimeMillis() - start;
			
			// 비동기로 로깅 위임
			apiLogProcessor.processLog(
				request.getMethod(),
				request.getRequestURI(),
				joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(),
				joinPoint.getArgs(),
				(exception != null ? exception : result),
				time,
				MDC.getCopyOfContextMap()
			);
			
			MDC.remove("requestId");
		}
	}
}
