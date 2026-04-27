package com.back.omos.domain.issue.controller

import com.back.omos.domain.issue.dto.RecommendIssueRes
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.issue.service.IssueService
import com.back.omos.global.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 이슈 관련 외부 요청을 처리하는 REST 컨트롤러입니다.
 * <p>
 * 깃허브 오픈소스 이슈의 수집, 조회 및 추천 시스템과의 상호작용을 위한
 * 진입점 역할을 수행하며, HTTP 요청을 서비스 계층으로 전달합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 해당 사항 없음
 *
 * <p><b>주요 생성자:</b><br>
 * {@code IssueController(IssueService issueService)} <br>
 * 이슈 관련 비즈니스 로직을 처리하는 [IssueService]를 주입받아 초기화합니다.
 *
 * <p><b>빈 관리:</b><br>
 * Spring Container에 의해 Singleton 빈으로 관리됩니다. (@RestController)
 *
 * <p><b>외부 모듈:</b><br>
 * Spring Web MVC를 사용하여 RESTful API 엔드포인트를 노출합니다.
 *
 * @author 유재원
 * @since 2026-04-23
 * @see com.back.omos.domain.issue.service.IssueService
 */
@RestController
@RequestMapping("/api/v1/issues")
class IssueController(
    private val issueService: IssueService,
    private val issueRepository: IssueRepository
) {
    /**
     * 현재 수집한 모든 이슈를 보여줍니다.
     * 깃허브 API 동작 확인용으로 넣었습니다.
     */
    @GetMapping
    fun getAllIssues(): CommonResponse<List<Issue>> {
        val issues = issueRepository.findAll()
        return CommonResponse.success(issues)
    }

    /**
     * 특정 언어나 라벨 조건에 맞는 깃허브 이슈를 수집하고 수집된 목록을 반환합니다.
     * <p>
     * 수집된 데이터는 [RecommendIssueRes] 형태로 변환되어 클라이언트에 즉시 전달됩니다.
     *
     * @param q 깃허브 검색 쿼리 (예: language:kotlin state:open)
     * @return 수집된 이슈 정보 리스트
     * @author 유재원
     */
    @PostMapping("/crawl/search")
    fun crawlBySearch(@RequestParam q: String): CommonResponse<List<RecommendIssueRes>> {
        return try {
            val savedIssues = issueService.crawlAndSaveByQuery(q)

            // 엔티티 리스트를 DTO 리스트로 변환
            val response = savedIssues.map { RecommendIssueRes.from(it) }

            CommonResponse.success(response)
        } catch (e: Exception) {
            //TODO 에러 로직 수정
            CommonResponse.fail(e.message)
        }
    }
}