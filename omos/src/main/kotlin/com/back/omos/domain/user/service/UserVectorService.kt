package com.back.omos.domain.user.service

import com.back.omos.domain.user.dto.UserInfoRes

/**
 * 사용자 프로필 벡터 임베딩을 담당하는 서비스 인터페이스입니다.
 *
 * <p>
 * GitHub API를 통해 사용자의 주요 사용 언어, 레포지토리 설명, 기술 토픽을 수집하고,
 * Gemini 임베딩 모델을 사용하여 1536차원 프로필 벡터를 생성합니다.
 *
 * <p>
 * 생성된 벡터는 추후 Good First Issue 추천 시 이슈 벡터와의 코사인 유사도 비교에 활용됩니다.
 *
 * @author MintyU
 * @since 2026-04-27
 * @see com.back.omos.domain.user.service.UserVectorServiceImpl
 */
interface UserVectorService {

    /**
     * 사용자의 GitHub 프로필 정보를 기반으로 프로필 벡터를 생성하고 저장합니다.
     *
     * <p>
     * GitHub API 호출 → 텍스트 변환 → Gemini 임베딩 → 벡터 저장 순으로 처리됩니다.
     *
     * @param githubId 벡터를 갱신할 사용자의 GitHub 고유 ID (숫자형 문자열)
     * @return 벡터 업데이트 후 사용자 정보 DTO
     * @throws com.back.omos.global.exception.exceptions.AuthException
     *         사용자를 찾을 수 없거나 임베딩 생성에 실패한 경우
     */
    fun updateUserVector(githubId: String): UserInfoRes
}
