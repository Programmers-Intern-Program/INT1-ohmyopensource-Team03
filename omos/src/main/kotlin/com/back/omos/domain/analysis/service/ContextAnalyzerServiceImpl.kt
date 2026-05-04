package com.back.omos.domain.analysis.service

import com.back.omos.domain.analysis.ai.GlmClient
import com.back.omos.domain.analysis.dto.GuideResponseDto
import com.back.omos.domain.analysis.dto.PseudoCodeResponseDto
import com.back.omos.domain.analysis.entity.AnalysisResult
import com.back.omos.domain.analysis.entity.UserAnalysisRequest
import com.back.omos.domain.analysis.github.GitHubClient
import com.back.omos.domain.analysis.repository.AnalysisResultRepository
import com.back.omos.domain.analysis.repository.UserAnalysisRequestRepository
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.errorCode.AnalysisErrorCode
import com.back.omos.global.exception.errorCode.AuthErrorCode
import com.back.omos.global.exception.exceptions.AnalysisException
import com.back.omos.global.exception.exceptions.AuthException
import com.back.omos.domain.analysis.github.GitHubIssueRes
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

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
    private val userAnalysisRequestRepository: UserAnalysisRequestRepository,
    private val issueRepository: IssueRepository,
    private val userRepository: UserRepository,
    @Qualifier(("analysisGitHubClientImpl"))
    private val gitHubClient: GitHubClient,
    private val glmClient: GlmClient,
    private val objectMapper: ObjectMapper
) : ContextAnalyzerService {

    private val log = LoggerFactory.getLogger(ContextAnalyzerServiceImpl::class.java)

    @Transactional
    override fun getGuide(issueId: Long, githubId: String): GuideResponseDto {
        return toGuideDto(resolveOrCreateAnalysis(issueId, githubId))
    }

    @Transactional
    override fun getPseudoCode(issueId: Long, githubId: String): PseudoCodeResponseDto {
        return toPseudoCodeDto(resolveOrCreateAnalysis(issueId, githubId))
    }

    /**
     * 분석 결과를 반환합니다.
     *
     * 사용자별 캐시 확인 → 일일 횟수 제한 검사 → 이슈별 캐시 확인(재사용/갱신) → 신규 생성 순으로 처리합니다.
     * 캐시된 분석 결과가 [CACHE_VALIDITY_DAYS]일 이상 경과한 경우 GitHub API로 이슈 수정 여부를 확인하고,
     * 수정된 경우 캐시를 무효화하고 재생성합니다.
     *
     * @param issueId 분석할 이슈의 식별자
     * @param githubId 요청한 사용자의 GitHub ID
     * @return 분석 결과 [AnalysisResult]
     */
    private fun resolveOrCreateAnalysis(issueId: Long, githubId: String): AnalysisResult {
        val issue = issueRepository.findById(issueId)
            .orElseThrow {
                AnalysisException(
                    AnalysisErrorCode.ISSUE_NOT_FOUND,
                    "[ContextAnalyzerServiceImpl#resolveOrCreateAnalysis] 이슈를 찾을 수 없습니다: issueId=$issueId",
                    "해당 이슈를 찾을 수 없습니다."
                )
            }

        // repoFullName 파싱을 상단으로 올림 (캐시 갱신 체크에 필요)
        val parts = issue.repoFullName.split("/")
        if (parts.size != 2) {
            throw AnalysisException(
                AnalysisErrorCode.GITHUB_API_FAIL,
                "[ContextAnalyzerServiceImpl#resolveOrCreateAnalysis] repoFullName 형식이 올바르지 않습니다: ${issue.repoFullName}",
                "레포지토리 정보가 올바르지 않습니다."
            )
        }
        val (owner, repoName) = parts

        val user = userRepository.findByGithubIdWithLock(githubId)
            .orElseThrow {
                AuthException(
                    AuthErrorCode.USER_NOT_FOUND,
                    "[ContextAnalyzerServiceImpl#resolveOrCreateAnalysis] 사용자를 찾을 수 없습니다: githubId=$githubId"
                )
            }

        // 해당 이슈에 대해 사용자가 이미 완료된 요청을 가지고 있으면 즉시 반환 (횟수 미차감)
        val existingRequest = userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(user.id!!, issueId)
        if (existingRequest != null) {
            return existingRequest.analysisResult!!
        }

        // 새 요청이므로 일일 횟수 제한 검사
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        val requestCount = userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(user.id!!, startOfDay, now)
        if (requestCount >= DAILY_REQUEST_LIMIT) {
            throw AnalysisException(
                AnalysisErrorCode.ANALYSIS_RATE_LIMIT_EXCEEDED,
                "[ContextAnalyzerServiceImpl#resolveOrCreateAnalysis] 일일 분석 요청 횟수 초과: userId=${user.id}, count=$requestCount"
            )
        }

        // 이슈별 캐시 확인 + 갱신 체크
        val cached = analysisResultRepository.findByIssueId(issueId)
        val cacheThreshold = now.minusDays(CACHE_VALIDITY_DAYS)

        val analysisResult = if (cached != null) {
            if (cached.createdAt.isBefore(cacheThreshold)) {
                // 캐시가 3일 이상 경과 → GitHub API로 이슈 수정 여부 확인
                log.info("[resolveOrCreateAnalysis] 캐시 유효기간 초과, 이슈 수정 여부 확인: issueId=$issueId")
                val latestIssue = gitHubClient.fetchIssue(owner, repoName, issue.issueNumber.toInt())

                if (isIssueModifiedAfterAnalysis(latestIssue, cached)) {
                    // 이슈 수정됨 → 캐시 무효화 후 재생성
                    log.info("[resolveOrCreateAnalysis] 이슈 수정 감지, 캐시 무효화: issueId=$issueId")
                    analysisResultRepository.delete(cached)
                    analysisResultRepository.flush()
                    generateAnalysis(issue, owner, repoName)
                } else {
                    // 이슈 수정 안 됨 → 기존 캐시 반환
                    cached
                }
            } else {
                // 3일 이내 캐시 → 그대로 반환
                cached
            }
        } else {
            generateAnalysis(issue, owner, repoName)
        }

        // 사용자 분석 요청 이력 저장
        userAnalysisRequestRepository.save(
            UserAnalysisRequest(user = user).apply { complete(analysisResult) }
        )

        return analysisResult
    }

    /**
     * 이슈가 분석 이후 수정되었는지 판단합니다.
     *
     * 캐시된 분석 결과의 createdAt과 GitHub API에서 가져온 이슈의 updatedAt을 비교합니다.
     * [resolveOrCreateAnalysis]에서 캐시 유효기간([CACHE_VALIDITY_DAYS]일) 초과 시 호출됩니다.
     *
     * @param latestIssue GitHub API에서 가져온 최신 이슈 정보
     * @param result 비교 대상인 기존 분석 결과
     * @return 이슈의 updatedAt이 분석 결과의 createdAt보다 늦으면 true
     */
    private fun isIssueModifiedAfterAnalysis(latestIssue: GitHubIssueRes, result: AnalysisResult): Boolean {
        return latestIssue.updatedAt?.isAfter(result.createdAt) ?: false
    }

    /**
     * GitHub API로 관련 소스코드를 수집하고 GLM API로 분석 결과를 생성한 뒤 저장합니다.
     *
     * 처리 흐름:
     * 1. GitHub API로 이슈 상세 정보 fetch
     * 2. GitHub Tree API로 전체 파일 경로 목록 fetch 후 확장자 필터링
     * 3. 확장자 필터링된 경로 전체 + 이슈 → GLM 1차 호출로 관련 파일 선별 (동적 배치)
     * 4. GraphQL로 선별된 파일 내용 한 번에 fetch
     * 5. 파일 내용 파싱/가공 (주석 제거, 토큰 제한)
     * 6. 파싱한 코드 + 이슈 → GLM 2차 호출로 가이드 생성
     *
     * @param issue 분석 대상 이슈
     * @return 저장된 [AnalysisResult]
     * @throws AnalysisException [Issue.repoFullName] 형식이 `owner/repo`가 아닌 경우,
     *                           또는 GitHub/GLM API 호출에 실패하는 경우
     */
    private fun generateAnalysis(issue: Issue, owner: String, repoName: String): AnalysisResult {

        val issueInfo = gitHubClient.fetchIssue(owner, repoName, issue.issueNumber.toInt())

        // 1단계: 확장자 필터링
        val allFilePaths = gitHubClient.fetchTree(owner, repoName)
            .filter { path ->
                path.endsWith(".ts") || path.endsWith(".js") ||
                        path.endsWith(".kt") || path.endsWith(".java") ||
                        path.endsWith(".py") || path.endsWith(".go") ||
                        path.endsWith(".rs") || path.endsWith(".cpp")
            }
        log.info("[generateAnalysis] 확장자 필터링 후 파일 수: ${allFilePaths.size}")
// 2단계: 동적 배치로 GLM 1차 호출 (최대한 많은 파일 경로 전달)
        val batchSizes = listOf(3300, 3000, 2000, 1000, 700, 300, 100)
        var selectedPaths: List<String> = emptyList()
        val allFilePathsSet = allFilePaths.toSet()

        for (size in batchSizes) {
            try {
                log.info("[generateAnalysis] GLM selectFiles 시도: ${size}개")
                selectedPaths = glmClient.selectFiles(
                    issueTitle = issueInfo.title,
                    issueBody = issueInfo.body,
                    filePaths = allFilePaths.take(size)
                )
                    .asSequence()
                    .filter { it in allFilePathsSet }
                    .distinct()
                    .take(MAX_SELECT_FILES)
                    .toList()
                log.info("[generateAnalysis] GLM selectFiles 성공! 배치 사이즈: $size, 선별 파일: $selectedPaths")
                break
            } catch (e: Error) {
                throw e
            } catch (e: Exception) {
                log.warn("[generateAnalysis] GLM selectFiles 실패 (배치 사이즈: $size): ${e.message}")
            }
        }

        if (selectedPaths.isEmpty()) {
            throw AnalysisException(
                AnalysisErrorCode.GLM_API_FAIL,
                "[generateAnalysis] 모든 배치 사이즈에서 GLM selectFiles 실패",
                "파일 선별에 실패했습니다."
            )
        }
        log.info("[generateAnalysis] GLM 선별 파일: $selectedPaths")
        // 3단계: GraphQL로 선별된 파일 내용 한 번에 fetch
        val fileContents = gitHubClient.fetchFileContents(owner, repoName, selectedPaths)
        log.info("[generateAnalysis] GraphQL fetch 완료 파일 수: ${fileContents.size}")
        fileContents.forEach { (path, content) ->
            log.info("[generateAnalysis] $path — ${content.length}자")
        }
        // 4단계: 파일 내용 파싱/가공 (토큰 절약)
        val parsedFileContents = fileContents.mapValues { (_, content) ->
            parseFileContent(content)
        }
        parsedFileContents.forEach { (path, content) ->
            log.info("[generateAnalysis] 파싱 후 $path — ${content.length}자")
        }
        // 5단계: 파싱한 코드 + 이슈 → GLM 2차 호출
        val filePaths = objectMapper.writeValueAsString(parsedFileContents.keys.toList())
        val labels = issueInfo.labels.map { it.name }
        val glmResult = glmClient.analyze(
            issueTitle = issueInfo.title,
            issueBody = issueInfo.body,
            labels = labels,
            fileContents = parsedFileContents
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

    /**
     * 파일 내용을 파싱/가공합니다.
     *
     * 빈 줄, 주석 제거 및 토큰 제한을 적용합니다.
     *
     * @param content 원본 파일 내용
     * @return 가공된 파일 내용
     */
    private fun parseFileContent(content: String): String {
        return content
            .lines()
            .filter { it.isNotBlank() }
            .filter { line ->
                val trimmed = line.trimStart()
                !trimmed.startsWith("//") &&
                        !trimmed.startsWith("*") &&
                        !trimmed.startsWith("/*") &&
                        !trimmed.startsWith("#")
            }
            .joinToString("\n")
            .take(MAX_FILE_CONTENT_LENGTH)
    }

    companion object {
        /** GLM이 최종 선별하는 파일 최대 개수 */
        private const val MAX_SELECT_FILES = 5
        /** 사용자 1인당 하루 최대 분석 요청 횟수 */
        private const val DAILY_REQUEST_LIMIT = 5L
        /** 파일당 최대 문자 수 (토큰 절약) */
        private const val MAX_FILE_CONTENT_LENGTH = 3000
        /** 캐시 유효기간 (일). 이 기간이 지나면 GitHub API로 이슈 수정 여부를 확인. */
        private const val CACHE_VALIDITY_DAYS = 3L
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