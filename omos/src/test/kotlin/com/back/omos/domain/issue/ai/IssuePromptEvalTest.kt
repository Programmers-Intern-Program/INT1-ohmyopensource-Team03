package com.back.omos.domain.issue.ai

import com.back.omos.domain.issue.entity.Issue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 이슈 추천 프롬프트 버전별 성능을 비교하기 위한 평가 테스트입니다.
 *
 * <p>
 * 언어별(Java, C++, Kotlin, Python, Go) 고정 프로필 샘플로 AI를 호출하고,
 * 결과를 Langfuse에 자동 기록하여 매칭 정확도와 품질을 비교합니다.
 *
 * <p><b>테스트 구성:</b><br>
 * - 샘플 1: Java (백엔드/Spring Boot 중심)<br>
 * - 샘플 2: C++ (시스템 프로그래밍/최적화 중심)<br>
 * - 샘플 3: Kotlin (안드로이드/비동기 중심)<br>
 * - 샘플 4: Python (데이터 사이언스/API 중심)<br>
 * - 샘플 5: Go (MSA/인프라 중심)<br>
 *
 * <p><b>실행 방법:</b><br>
 * IntelliJ에서 실행하며, 실행 후 Langfuse 대시보드에서 버전별 Latency와 파싱 성공률을 확인합니다.
 * 프롬프트 수정 시 [IssueGlmClientImpl.GENERATION_NAME]의 버전을 갱신해야 합니다.
 *
 * @author 유재원
 * @since 2026-05-06
 */
@SpringBootTest
@ActiveProfiles("dev")
class IssuePromptEvalTest {

    @Autowired
    lateinit var issueGlmClient: IssueGlmClient

    @Test
    fun `언어별 고정 프로필 기반 추천 성능 평가 실행`() {
        // 1. 공통 후보 이슈 풀 (AI가 이 중에서 각 언어에 맞는 것을 골라야 함)
        val candidatePool = createFixedCandidateIssues()

        // 2. 5가지 언어별 테스트 케이스 정의
        val testCases = listOf(
            EvalCase("Java", "Java 17, Spring Boot, JPA 숙련자. 대용량 트래픽 처리와 DB 성능 최적화에 관심이 많음."),
            EvalCase("C++", "C++20 표준 선호. 임베디드 시스템, 그래픽스 엔진 개발 및 메모리 관리 최적화 경험 보유."),
            EvalCase("Kotlin", "Kotlin 코루틴과 Flow 활용 능숙. 안드로이드 클린 아키텍처 및 Jetpack Compose 개발자."),
            EvalCase("Python", "FastAPI를 활용한 백엔드 개발 및 Pandas 기반 데이터 전처리, Scikit-learn 모델 서빙 관심."),
            EvalCase("Go", "Goroutine 기반 고성능 서버 개발 및 Kubernetes 커스텀 리소스, MSA 환경 구축 선호.")
        )

        // 3. 테스트 실행
        testCases.forEach { case ->
            println("=== [Evaluating] Language: ${case.language} ===")
            try {
                val results = issueGlmClient.generateRecommendationReasons(case.profile, candidatePool)

                // 결과 요약 출력
                results.forEach {
                    println(" - Recommended: ${it.title} (Reason: ${it.reason.take(30)}...)")
                }
            } catch (e: Exception) {
                println(" ! Error during ${case.language} evaluation: ${e.message}")
            }
        }
    }


    @Test
    fun `기술 스택이 모호한 관심사 기반 프로필 추천 성능 평가`() {
        val candidatePool = createFixedCandidateIssues()

        val ambiguousCases = listOf(
            EvalCase("Data-Oriented", "데이터의 무결성을 유지하면서 수만 건의 쿼리를 최적화하는 데 집착합니다."),
            EvalCase("Low-Level", "하드웨어 리소스를 직접 제어하거나 메모리 할당 효율을 높이는 저수준 최적화에 관심이 많습니다."),
            EvalCase("UI-UX-Flow", "사용자에게 부드러운 화면 전환을 제공하고 상태 관리를 선언적으로 처리하는 것을 선호합니다."),
            EvalCase("Infrastructure", "서버의 확장성을 고민하며, 동시성 처리를 통해 처리량을 극대화하는 설계를 좋아합니다."),
            EvalCase("Middleware", "클라이언트와 서버 사이에서 요청을 가로채거나 비동기적으로 메시지를 처리하는 구조를 설계하고 싶습니다.")
        )

        ambiguousCases.forEach { case ->
            println("=== [Evaluating Ambiguous] Focus: ${case.language} ===")
            try {
                val results = issueGlmClient.generateRecommendationReasons(case.profile, candidatePool)
                results.forEach { println(" - [${case.language}] -> ${it.title}") }
            } catch (e: Exception) {
                println(" ! Error: ${e.message}")
            }
        }
    }

    @Test
    fun `후보 풀에 없는 기술 스택 프로필 추천 성능 평가`() {
        val candidatePool = createFixedCandidateIssues()

        val unmatchedCases = listOf(
            EvalCase("Rust", "Rust 언어의 소유권 개념을 사랑하며, Rocket 프레임워크로 웹 서버를 구축합니다."),
            EvalCase("Swift", "iOS 환경에서 Swift를 사용해 고성능 애니메이션과 Combine을 활용한 비동기 처리를 구현합니다."),
            EvalCase("Ruby", "Ruby on Rails의 생산성을 즐기며, 가독성 높은 코드를 작성하는 데 자부심이 있습니다."),
            EvalCase("Flutter", "Dart 언어를 기반으로 하나의 코드베이스에서 멀티 플랫폼 UI를 구현하는 데 특화되어 있습니다."),
            EvalCase("PHP", "Laravel 프레임워크를 활용해 대규모 커머스 시스템의 백엔드를 구축해온 시니어 개발자입니다.")
        )

        unmatchedCases.forEach { case ->
            println("=== [Evaluating Unmatched] Tech: ${case.language} ===")
            try {
                val results = issueGlmClient.generateRecommendationReasons(case.profile, candidatePool)
                results.forEach { println(" - [${case.language}] -> ${it.title}") }
            } catch (e: Exception) {
                println(" ! Error: ${e.message}")
            }
        }
    }


    private fun createFixedCandidateIssues(): List<Issue> {
        return listOf(
            Issue(
                repoFullName = "spring-projects/spring-data-jpa",
                issueNumber = 101L,
                title = "Fix N+1 query in Spring Data JPA",
                labels = listOf("java", "bug")
            ),
            Issue(
                repoFullName = "khronos/vulkan",
                issueNumber = 202L,
                title = "Optimize memory allocation in Vulkan renderer",
                labels = listOf("cpp", "performance")
            ),
            Issue(
                repoFullName = "google/android-samples",
                issueNumber = 303L,
                title = "Migrate LiveData to StateFlow in Main Screen",
                labels = listOf("kotlin", "refactor")
            ),
            Issue(
                repoFullName = "tiangolo/fastapi",
                issueNumber = 404L,
                title = "Add async support for FastAPI middleware",
                labels = listOf("python", "feature")
            ),
            Issue(
                repoFullName = "golang/go",
                issueNumber = 505L,
                title = "Implement worker pool using channels",
                labels = listOf("go", "improvement")
            )
        )
    }

    data class EvalCase(val language: String, val profile: String)
}