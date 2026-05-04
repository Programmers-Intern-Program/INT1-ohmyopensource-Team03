package com.back.omos.domain.issue.service

import com.back.omos.domain.issue.dto.RecommendIssueHistoryRes
import com.back.omos.domain.issue.dto.RecommendIssueRes

/**
 * 사용자 맞춤형 추천 비즈니스 로직을 처리하는 서비스 인터페이스입니다.
 *
 * <p>
 * 사용자의 GitHub 활동 데이터를 기반으로 생성된 벡터 프로필과
 * 수집된 오픈소스 이슈들 간의 의미적 유사도를 분석하여 최적의 기여 기회를 제공합니다.
 *
 * @author 유재원
 * @since 2026-04-25
 */
interface RecommendService {
    /**
     * 사용자의 GitHub 고유 ID를 기반으로 개인화된 오픈소스 이슈 추천 정보를 생성합니다.
     * * <p>
     * 이 메서드는 RAG(Retrieval-Augmented Generation) 패턴을 따르며 다음 과정을 수행합니다:
     * 1. 사용자의 프로필 벡터를 활용한 벡터 유사도 검색 (Retrieval)
     * 2. 검색된 이슈 후보들과 유저 스택 정보를 조합한 컨텍스트 구성 (Augmentation)
     * 3. AI 모델(GLM)을 통한 최종 선정 및 맞춤형 추천 사유 생성 (Generation)
     *
     * 추천 결과는 [getUserRecommendationHistory]를 통해 이후에도 조회할 수 있도록 저장됩니다.
     * 동일 이슈가 재추천된 경우 기존 레코드의 추천 사유를 최신 내용으로 갱신합니다.
     *
     * @param githubId 추천을 진행할 사용자의 GitHub 고유 식별자
     * @return AI가 선정한 최적의 이슈 정보와 상세 추천 사유를 포함한 List<RecommendIssueRes>
     */
    fun getPersonalizedRecommendation(githubId: String): List<RecommendIssueRes>

    /**
     * 사용자가 이전에 추천받은 이슈 목록을 최근 추천 순으로 반환합니다.
     *
     * 서비스 재방문 시 기존 추천 이력을 다시 확인할 수 있도록 제공됩니다.
     *
     * @param githubId 조회할 사용자의 GitHub 고유 식별자
     * @return 추천 이력 목록 (최근 추천 순), 이력이 없으면 빈 리스트 반환
     * @author MintyU
     * @since 2026-04-29
     */
    fun getUserRecommendationHistory(githubId: String): List<RecommendIssueHistoryRes>
}