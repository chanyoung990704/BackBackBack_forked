package com.aivle.project.common.util;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 조회 후 생성 패턴의 충돌(UNIQUE) 시 재조회로 복구하는 공통 유틸.
 */
public final class GetOrCreateResolver {

	private GetOrCreateResolver() {
	}

	public static <T> T resolve(
		Supplier<Optional<T>> finder,
		Supplier<T> creator,
		Supplier<Optional<T>> refinder
	) {
		Optional<T> found = finder.get();
		if (found.isPresent()) {
			return found.get();
		}
		try {
			return creator.get();
		} catch (DataIntegrityViolationException ex) {
			return refinder.get().orElseThrow(() -> ex);
		}
	}
}
