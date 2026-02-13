package com.aivle.project.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class GetOrCreateResolverTest {

	@Test
	@DisplayName("조회 결과가 있으면 생성 로직을 실행하지 않는다")
	void resolve_ReturnsFoundEntity() {
		AtomicInteger creatorCalls = new AtomicInteger();

		String result = GetOrCreateResolver.resolve(
			() -> Optional.of("found"),
			() -> {
				creatorCalls.incrementAndGet();
				return "created";
			},
			Optional::<String>empty
		);

		assertThat(result).isEqualTo("found");
		assertThat(creatorCalls.get()).isZero();
	}

	@Test
	@DisplayName("조회 결과가 없으면 생성 결과를 반환한다")
	void resolve_ReturnsCreatedEntity() {
		String result = GetOrCreateResolver.resolve(
			Optional::<String>empty,
			() -> "created",
			Optional::<String>empty
		);

		assertThat(result).isEqualTo("created");
	}

	@Test
	@DisplayName("생성 시 유니크 충돌이 발생하면 재조회 결과를 반환한다")
	void resolve_RequeriesAfterConflict() {
		String result = GetOrCreateResolver.resolve(
			Optional::<String>empty,
			() -> {
				throw new DataIntegrityViolationException("unique conflict");
			},
			() -> Optional.of("re-fetched")
		);

		assertThat(result).isEqualTo("re-fetched");
	}

	@Test
	@DisplayName("유니크 충돌 후 재조회도 실패하면 원본 예외를 다시 던진다")
	void resolve_ThrowsWhenRefetchFails() {
		DataIntegrityViolationException exception = new DataIntegrityViolationException("unique conflict");

		assertThatThrownBy(() -> GetOrCreateResolver.resolve(
			Optional::<String>empty,
			() -> {
				throw exception;
			},
			Optional::<String>empty
		))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("unique conflict");
	}
}
