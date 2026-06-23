package com.aivle.project.auth.user.entity;

/**
 * 이메일 인증 상태.
 */
public enum VerificationStatus {
    PENDING,      // 인증 대기 중
    VERIFIED,     // 인증 완료
    EXPIRED,      // 만료됨
    FAILED        // 인증 실패
}
