package com.back.omos.domain.issue.controller

import com.back.omos.domain.issue.service.IssueService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
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
    private val issueService: IssueService
) {

    /**
     * 특정 레포지토리 ID를 입력받아 깃허브 이슈를 크롤링하고 저장합니다.
     */
    @PostMapping("/crawl/{repoId}")
    fun crawlIssues(@PathVariable repoId: Long): ResponseEntity<String> {
        return try {
            issueService.crawlAndSave(repoId)
            ResponseEntity.ok("성공적으로 레포지토리($repoId)의 이슈를 수집했습니다.")
        } catch (e: Exception) {
            // 에러 발생 시 400 또는 500 에러와 함께 메시지 반환
            ResponseEntity.badRequest().body("크롤링 실패: ${e.message}")
        }
    }
}