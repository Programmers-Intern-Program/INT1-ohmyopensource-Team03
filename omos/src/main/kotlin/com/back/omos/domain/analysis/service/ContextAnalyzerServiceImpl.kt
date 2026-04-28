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
 * [ContextAnalyzerService]의 핵심 비즈니스 로직을 담당하는 서비스 구현체입니다.
 *
 * 이슈에 대한 코드 수정 가이드 및 수도 코드 조회 요청을 처리합니다.
 *
 * <p><b>현재 캐시 정책:</b><br>
 * [AnalysisResultRepository]에 해당 이슈의 분석 결과가 존재하면 즉시 반환합니다.
 * 분석 결과가 없는 경우에만 GitHub API와 GLM API를 통해 새 분석 결과를 생성하고 저장합니다.
 *
 * <p><b>캐시 갱신 (미구현 — TODO):</b><br>
 * [isIssueModifiedAfterAnalysis]를 통해 이슈의 updatedAt과 분석 결과의 createdAt을 비교,
 * 이슈가 분석 이후 수정된 경우 가이드를 재생성하는 로직이 설계되어 있으나 아직 적용되지 않았습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * GitHub API — 관련 소스코드 수집<br>
 * GLM API — 코드 수정 가이드 및 수도 코드 생성
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
     *
     * TODO: [getGuide], [getPseudoCode]의 캐시 갱신 조건으로 연결 필요.
     *       현재 이 메서드는 호출되지 않으므로 캐시 무효화가 동작하지 않습니다.
     *
     * @param issue 캐시 유효성을 검사할 이슈
     * @param result 비교 대상인 기존 분석 결과
     * @return 이슈의 [Issue.updatedAt]이 분석 결과의 [AnalysisResult.createdAt]보다 늦으면 true
     */
    private fun isIssueModifiedAfterAnalysis(issue: Issue, result: AnalysisResult): Boolean {
        return issue.updatedAt.isAfter(result.createdAt)
    }

    /**
     * GitHub API로 관련 소스코드를 수집하고 GLM API로 분석 결과를 생성한 뒤 저장합니다.
     *
     * 처리 흐름:
     * 1. [Issue.repoFullName]에서 owner/repo 파싱
     * 2. GitHub API로 이슈 상세 정보 fetch
     * 3. GitHub API로 레포지토리 전체 파일 트리 fetch 후 지원 확장자로 필터링
     * 4. 이슈 제목 키워드와 매칭되는 파일 경로 선별 (매칭 없으면 상위 5개 폴백)
     * 5. 선택된 파일의 내용 fetch
     * 6. 선택된 파일 경로를 JSON 배열 문자열로 직렬화
     * 7. GLM API로 가이드·수도 코드·사이드 이펙트 분석 수행
     *
     * @param issue 분석 대상 이슈
     * @return 저장된 [AnalysisResult]
     * @throws AnalysisException [Issue.repoFullName] 형식이 `owner/repo`가 아닌 경우,
     *                           또는 GitHub/GLM API 호출에 실패하는 경우
     */
    private fun generateAnalysis(issue: Issue): AnalysisResult {
        val parts = issue.repoFullName.split("/")
        if (parts.size != 2) {
            throw AnalysisException(
                AnalysisErrorCode.GITHUB_API_FAIL,
                "[ContextAnalyzerServiceImpl#generateAnalysis] repoFullName 형식이 올바르지 않습니다: ${issue.repoFullName}",
                "레포지토리 정보가 올바르지 않습니다."
            )
        }
        val (owner, repoName) = parts

        val issueInfo = gitHubClient.fetchIssue(owner, repoName, issue.issueNumber.toInt())

        // 1단계: 확장자 필터링
        val allFilePaths = gitHubClient.fetchTree(owner, repoName)
            .filter { path ->
                path.endsWith(".ts") || path.endsWith(".js") ||
                        path.endsWith(".kt") || path.endsWith(".java") ||
                        path.endsWith(".py") || path.endsWith(".go") ||
                        path.endsWith(".rs") || path.endsWith(".cpp")
            }

        // 2단계: 키워드 프리필터링으로 GLM에 넘길 후보를 MAX_CANDIDATE_FILES 이하로 제한
        val keywords = issueInfo.title
            .lowercase()
            .split(" ", "-", "_", ".")
            .filter { it.length > 3 }

        val candidatePaths = allFilePaths
            .filter { path -> keywords.any { keyword -> path.lowercase().contains(keyword) } }
            .take(MAX_CANDIDATE_FILES)
            .ifEmpty { allFilePaths.take(MAX_CANDIDATE_FILES) }

// 3단계: 좁혀진 후보 내에서 GLM이 최종 선별 + 결과 검증
        val candidatePathsSet = candidatePaths.toSet()
        val selectedPaths = glmClient.selectFiles(
            issueTitle = issueInfo.title,
            issueBody = issueInfo.body,
            filePaths = candidatePaths
        )
            .asSequence()
            .filter { it in candidatePathsSet }  // 후보 외 경로 제거
            .distinct()                           // 중복 제거
            .take(MAX_SELECT_FILES)               // 최대 5개 강제 제한
            .toList()

        val fileContents = selectedPaths
            .mapNotNull { path ->
                val content = gitHubClient.fetchFileContent(owner, repoName, path)
                if (content != null) path to content else null
            }
            .toMap()

        val filePaths = objectMapper.writeValueAsString(fileContents.keys.toList())
        val labels = issueInfo.labels.map { it.name }
        val glmResult = glmClient.analyze(
            issueTitle = issueInfo.title,
            issueBody = issueInfo.body,
            labels = labels,
            fileContents = fileContents
        )

        return analysisResultRepository.save(
            AnalysisResult(
                issue = issue,
                filePaths = filePaths,
                guideline = glmResult.guideline,
                pseudoCode = glmResult.pseudoCode,
                sideEffects = glmResult.sideEffects
            )
        )
    }

    companion object {
        /** GLM에 넘기는 후보 파일 최대 개수. 테스트 결과 30개 초과 시 GLM 응답 품질 저하 및 토큰 초과 가능성이 있어 설정. */
        private const val MAX_CANDIDATE_FILES = 30
        /** GLM이 최종 선별하는 파일 최대 개수. 5개 초과 시 fetchFileContent 호출 증가로 응답 지연 발생. */
        private const val MAX_SELECT_FILES = 5
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
     * JSON 배열 문자열을 [List]<[String]>으로 파싱합니다.
     *
     * [AnalysisResult.filePaths]는 [generateAnalysis]에서 Jackson으로 직렬화된 값이므로
     * 정상적인 흐름에서는 항상 유효한 JSON입니다.
     * 단, DB에 비정상적인 값이 저장된 경우 [com.fasterxml.jackson.core.JsonProcessingException]이
     * 발생할 수 있으므로, 필요 시 호출부에서 [AnalysisException]으로 래핑하는 것을 고려하세요.
     *
     * @param filePaths JSON 배열 형태의 파일 경로 문자열 (예: `["src/Foo.kt","src/Bar.kt"]`)
     * @return 파싱된 파일 경로 목록
     */
    private fun parseFilePaths(filePaths: String): List<String> {
        return objectMapper.readValue(filePaths, object : TypeReference<List<String>>() {})
    }
}