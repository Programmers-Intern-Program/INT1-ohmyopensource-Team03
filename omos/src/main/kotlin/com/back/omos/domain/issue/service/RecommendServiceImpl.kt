package com.back.omos.domain.issue.service

import com.back.omos.domain.analysis.repository.UserAnalysisRequestRepository
import com.back.omos.domain.issue.ai.IssueGlmClient
import com.back.omos.domain.issue.dto.RecommendIssueHistoryRes
import com.back.omos.domain.issue.dto.RecommendIssueRes
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.entity.UserRecommendedIssue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.issue.repository.UserRecommendedIssueRepository
import com.back.omos.domain.user.entity.User
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.errorCode.AuthErrorCode
import com.back.omos.global.exception.errorCode.IssueErrorCode
import com.back.omos.global.exception.exceptions.AuthException
import com.back.omos.global.exception.exceptions.IssueException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 맞춤형 추천 비즈니스 로직을 처리하는 서비스 구현체입니다.
 *
 * @author 유재원
 * @since 2026-04-27
 */
@Service
class RecommendServiceImpl(
    private val issueRepository: IssueRepository,
    private val userRepository: UserRepository,
    private val issueGlmClient: IssueGlmClient,
    private val userRecommendedIssueRepository: UserRecommendedIssueRepository,
    private val userAnalysisRequestRepository: UserAnalysisRequestRepository
) : RecommendService {

    @Transactional
    override fun getPersonalizedRecommendation(githubId: String): List<RecommendIssueRes> {

        // 1. 유저 조회
        val user = userRepository.findByGithubId(githubId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND, "GitHub ID [$githubId]에 해당하는 유저를 찾을 수 없습니다.") }

        // 2. 벡터 가져오기
        val userVector = user.profileVector
            ?: throw AuthException(AuthErrorCode.USER_NOT_FOUND, "유저의 프로필 벡터가 존재하지 않습니다. 먼저 GitHub 분석을 완료해주세요.")

        // 3. 유사 이슈 검색 현재 5개
        val topIssues = issueRepository.findBySimilarity(userVector, 5)

        if (topIssues.isEmpty()) {
            throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND, "추천할 수 있는 이슈가 DB에 없습니다.")
        }

        // 4. 컨텍스트 구성
        val userStackText = user.primaryLanguages?.joinToString(", ") ?: "알 수 없음"
        val userContext = "주요 사용 언어: $userStackText"

        // 5. [Generation] AI 추천 사유 생성 (RAG)
        val aiRecommendationReasons = issueGlmClient.generateRecommendationReasons(
            userProfile = userContext,
            candidateIssues = topIssues
        )

        // 6. AI 결과와 이슈 매칭
        val recommendations = aiRecommendationReasons.mapNotNull { aiResult ->
            val matchedIssue = topIssues.find {
                it.title == aiResult.title && it.repoFullName == aiResult.repoName
            } ?: return@mapNotNull null

            matchedIssue to aiResult.reason
        }

        // 7. 추천 이력 저장 (upsert)
        saveRecommendationHistory(user, recommendations)

        // 8. 결과 반환
        return recommendations.map { (issue, reason) ->
            RecommendIssueRes.from(issue).copy(summary = reason)
        }
    }

    @Transactional(readOnly = true)
    override fun getUserRecommendationHistory(githubId: String): List<RecommendIssueHistoryRes> {
        val user = userRepository.findByGithubId(githubId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND, "GitHub ID [$githubId]에 해당하는 유저를 찾을 수 없습니다.") }

        val history = userRecommendedIssueRepository.findAllByUserIdOrderByUpdatedAtDesc(user.id!!)

        if (history.isEmpty()) return emptyList()

        // 추천 이력의 이슈 ID 목록으로 분석 완료 여부를 한 번에 조회
        val issueIds = history.mapNotNull { it.issue.id }
        val analyzedMap = userAnalysisRequestRepository
            .findAllByUserIdAndAnalysisResultIssueIdIn(user.id!!, issueIds)
            .associateBy { it.analysisResult!!.issue.id!! }

        return history.map { recommended ->
            val analysisRequest = recommended.issue.id?.let { analyzedMap[it] }
            RecommendIssueHistoryRes.from(recommended, analysisRequest)
        }.sortedByDescending { it.isAnalyzed }
    }

    /**
     * 추천 결과를 이력으로 저장합니다. 이미 추천된 이슈는 요약을 최신 내용으로 갱신합니다.
     *
     * 기존 레코드를 한 번의 IN 쿼리로 일괄 조회한 뒤 메모리에서 upsert를 처리하여
     * N+1 문제를 방지합니다.
     *
     * @param user 추천을 받은 사용자
     * @param recommendations 추천된 이슈와 AI 생성 사유의 쌍 목록
     * @author MintyU
     * @since 2026-04-29
     */
    private fun saveRecommendationHistory(user: User, recommendations: List<Pair<Issue, String>>) {
        val userId = user.id ?: return
        val issueIds = recommendations.mapNotNull { (issue, _) -> issue.id }

        val existingMap = userRecommendedIssueRepository
            .findAllByUserIdAndIssueIdIn(userId, issueIds)
            .associateBy { it.issue.id!! }

        recommendations.forEach { (issue, summary) ->
            val issueId = issue.id ?: return@forEach
            val existing = existingMap[issueId]
            if (existing != null) {
                existing.summary = summary
            } else {
                userRecommendedIssueRepository.save(UserRecommendedIssue(user = user, issue = issue, summary = summary))
            }
        }
    }
}
