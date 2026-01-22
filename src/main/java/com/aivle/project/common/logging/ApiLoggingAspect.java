package com.aivle.project.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * API 요청/응답 로깅을 위한 Aspect.
 */
@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

	@Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
	public void restController() {}

	@Around("restController()")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

		long start = System.currentTimeMillis();
		try {
			Object result = joinPoint.proceed();
			long end = System.currentTimeMillis();

			log.info("API Request: [{} {}] | Method: {}.{} | Args: {} | Time: {}ms",
				request.getMethod(), request.getRequestURI(),
				joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName(),
				Arrays.toString(joinPoint.getArgs()), (end - start));

			return result;
		} catch (Throwable e) {
			long end = System.currentTimeMillis();
			log.error("API Error: [{} {}] | Method: {}.{} | Error: {} | Time: {}ms",
				request.getMethod(), request.getRequestURI(),
				joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName(),
				e.getMessage(), (end - start));
			throw e;
		}
	}
}
