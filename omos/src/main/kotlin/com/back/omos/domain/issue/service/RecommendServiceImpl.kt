package com.back.omos.domain.issue.service

import com.back.omos.domain.issue.ai.IssueGlmClient
import com.back.omos.domain.issue.dto.RecommendIssueRes
import com.back.omos.domain.issue.repository.IssueRepository
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
    private val issueGlmClient: IssueGlmClient
) : RecommendService {

    @Transactional(readOnly = true)
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

        // 6. 결과 반환
        return aiRecommendationReasons.mapNotNull { aiResult ->
            // 제목과 레포지토리 이름이 모두 일치하는 이슈를 찾음
            val matchedIssue = topIssues.find {
                it.title == aiResult.title && it.repoFullName == aiResult.repoName
            }

            // 매칭되는 이슈를 못 찾으면 null처리
            if (matchedIssue == null) {
                return@mapNotNull null
            }

            // DTO로 변환
            RecommendIssueRes.from(matchedIssue).copy(
                summary = aiResult.reason
            )
        }
    }
}