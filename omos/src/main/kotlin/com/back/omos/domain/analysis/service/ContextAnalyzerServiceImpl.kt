package com.back.omos.domain.analysis.service

import com.back.omos.domain.analysis.ai.GlmClient
import com.back.omos.domain.analysis.dto.GuideResponseDto
import com.back.omos.domain.analysis.dto.PseudoCodeResponseDto
import com.back.omos.domain.analysis.entity.AnalysisResult
import com.back.omos.domain.analysis.github.GitHubClient
import com.back.omos.domain.analysis.repository.AnalysisResultRepository
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.global.exception.errorCode.AnalysisErrorCode
import com.back.omos.global.exception.exceptions.AnalysisException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Context Analyzer의 핵심 비즈니스 로직을 담당하는 서비스 구현체입니다.
 * <p>
 * 이슈에 대한 코드 수정 가이드 조회 요청을 처리하며,
 * 캐시된 분석 결과가 유효하면 즉시 반환하고,
 * 이슈가 수정되었거나 캐시가 없으면 GLM API를 호출하여 새로운 분석 결과를 생성합니다.
 *
 * <p><b>캐시 갱신 정책:</b><br>
 * 이슈의 updatedAt과 분석 결과의 createdAt을 비교하여,
 * 이슈가 분석 이후 수정된 경우에만 가이드를 재생성합니다.
 * 기존 결과가 있으면 동일 row를 업데이트하고, 없으면 새로 생성합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * GitHub API — 관련 소스코드 수집 <br>
 * GLM API — 코드 수정 가이드 생성
 *
 * @author Jaewon Ryu
 * @since 2026-04-22
 * @see ContextAnalyzerService
 * @see AnalysisResult
 */
@Service
class ContextAnalyzerServiceImpl(
    private val analysisResultRepository: AnalysisResultRepository,
    private val issueRepository: IssueRepository,
    @Qualifier(("analysisGitHubClientImpl"))
    private val gitHubClient: GitHubClient,
    private val glmClient: GlmClient,
    private val objectMapper: ObjectMapper
) : ContextAnalyzerService {

    @Transactional
    override fun getGuide(issueId: Long): GuideResponseDto {
        val issue = issueRepository.findById(issueId)
            .orElseThrow {
                AnalysisException(
                    AnalysisErrorCode.ISSUE_NOT_FOUND,
                    "[ContextAnalyzerServiceImpl#getGuide] 이슈를 찾을 수 없습니다: issueId=$issueId",
                    "해당 이슈를 찾을 수 없습니다."
                )
            }

        val cached = analysisResultRepository.findByIssueId(issueId)
        if (cached != null) {
            return toGuideDto(cached)
        }

        val analysisResult = generateAnalysis(issue)
        return toGuideDto(analysisResult)
    }

    @Transactional
    override fun getPseudoCode(issueId: Long): PseudoCodeResponseDto {
        val issue = issueRepository.findById(issueId)
            .orElseThrow {
                AnalysisException(
                    AnalysisErrorCode.ISSUE_NOT_FOUND,
                    "[ContextAnalyzerServiceImpl#getPseudoCode] 이슈를 찾을 수 없습니다: issueId=$issueId",
                    "해당 이슈를 찾을 수 없습니다."
                )
            }

        val cached = analysisResultRepository.findByIssueId(issueId)
        if (cached != null) {
            return toPseudoCodeDto(cached)
        }

        val analysisResult = generateAnalysis(issue)
        return toPseudoCodeDto(analysisResult)
    }

    /**
     * 이슈가 분석 이후 수정되었는지 판단합니다.
     */
    private fun isIssueModifiedAfterAnalysis(issue: Issue, result: AnalysisResult): Boolean {
        return issue.updatedAt.isAfter(result.createdAt)
    }

    /**
     * GitHub API로 소스코드를 수집하고 GLM API로 분석 결과를 생성합니다.
     */
    private fun generateAnalysis(issue: Issue): AnalysisResult {

        // 1. repositoryId로 Repo 조회 → owner/repo 파싱
        val parts = issue.repoFullName.split("/")
        if (parts.size != 2) {
            throw AnalysisException(
                AnalysisErrorCode.GITHUB_API_FAIL,
                "[ContextAnalyzerServiceImpl#generateAnalysis] repoFullName 형식이 올바르지 않습니다: ${issue.repoFullName}",
                "레포지토리 정보가 올바르지 않습니다."
            )
        }
        val (owner, repoName) = parts

        // 2. 이슈 정보 fetch
        val issueInfo = gitHubClient.fetchIssue(owner, repoName, issue.issueNumber.toInt())

        // 3. 이슈 제목으로 관련 파일 검색
        val searchResult = gitHubClient.searchCode(issueInfo.title, owner, repoName)

        // 4. 검색된 파일들 내용 fetch (최대 5개로 제한)
        val fileContents = searchResult.items
            .take(5)
            .mapNotNull { item ->
                val content = gitHubClient.fetchFileContent(owner, repoName, item.path)
                if (content != null) item.path to content else null
            }
            .toMap()

        // 5. filePaths JSON 직렬화
        val filePaths = objectMapper.writeValueAsString(fileContents.keys.toList())

        // 6. GLM API로 가이드 생성
        val labels = issueInfo.labels.map { it.name }
        val glmResult = glmClient.analyze(
            issueTitle = issueInfo.title,
            issueBody = issueInfo.body,
            labels = labels,
            fileContents = fileContents
        )

        val newResult = AnalysisResult(
            issue = issue,
            filePaths = filePaths,
            guideline = glmResult.guideline,
            pseudoCode = glmResult.pseudoCode,
            sideEffects = glmResult.sideEffects
        )
        return analysisResultRepository.save(newResult)
    }

    private fun toGuideDto(result: AnalysisResult): GuideResponseDto {
        return GuideResponseDto(
            filePaths = parseFilePaths(result.filePaths),
            guideline = result.guideline,
            sideEffects = result.sideEffects
        )
    }

    private fun toPseudoCodeDto(result: AnalysisResult): PseudoCodeResponseDto {
        return PseudoCodeResponseDto(
            filePaths = parseFilePaths(result.filePaths),
            pseudoCode = result.pseudoCode
        )
    }

    /**
     * JSON 배열 문자열을 List<String>으로 파싱합니다.
     *
     * @param filePaths JSON 배열 형태의 파일 경로 문자열
     * @return 파싱된 파일 경로 목록
     */
    private fun parseFilePaths(filePaths: String): List<String> {
        return objectMapper.readValue(filePaths, object : TypeReference<List<String>>() {})
    }
}