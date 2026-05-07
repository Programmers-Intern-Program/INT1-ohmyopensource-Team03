package com.back.omos.domain.prdraft.ai

import com.back.omos.domain.prdraft.github.GitHubPrRes
import com.back.omos.domain.prdraft.service.PrDraftPromptBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.File

/**
 * 프롬프트 버전별 성능을 비교하기 위한 평가 테스트입니다.
 *
 * <p>
 * 고정된 diff 샘플 20종으로 AI를 호출하고, 결과를 Langfuse에 자동 기록합니다.
 * 프롬프트를 수정할 때마다 이 테스트를 실행하여 버전 간 score/latency를 비교하세요.
 *
 * <p><b>샘플 분류 (총 20종):</b><br>
 * - 샘플 1~5: 기본 케이스 (버그/기능/리팩토링/성능/보안, v1부터의 고정 기준선)<br>
 * - 샘플 6~7: 극소 diff (1~2줄 변경)<br>
 * - 샘플 8~9: 대형 diff (50줄+ 신규 기능/다중 파일)<br>
 * - 샘플 10~12: 애매한 케이스 (변경 이유가 코드만으로 불분명)<br>
 * - 샘플 13~20: 엣지케이스 (테스트 전용/빌드/삭제/공백/DB/로깅/예외/인터페이스)<br>
 *
 * <p><b>실행 방법:</b><br>
 * IntelliJ에서 실행하거나 {@code @Disabled}를 제거 후 실행합니다.
 * 실행 후 localhost:3001 → Traces에서 결과를 확인합니다.
 *
 * <p><b>주의:</b><br>
 * 실제 GLM API와 Langfuse를 호출하므로 dev 환경에서만 실행해야 합니다.
 * 프롬프트 버전을 바꾸면 {@link PrDraftPromptBuilder#PROMPT_VERSION}과
 * {@link SpringAiClient}의 {@code GENERATION_PR_DRAFT} 상수도 함께 올려야 합니다.
 *
 * @author 5h6vm
 * @since 2026-04-30
 */
//@Disabled("수동 프롬프트 평가 전용 — 실행 시 @Disabled 제거")
@SpringBootTest
@ActiveProfiles("test")
class PrDraftPromptEvalTest {

    @Autowired
    private lateinit var aiClient: SpringAiClient

    @Autowired
    private lateinit var promptBuilder: PrDraftPromptBuilder

    /**
     * 카테고리 대표 7종으로 빠르게 프롬프트를 평가합니다.
     * 프롬프트 수정할 때마다 사용. 총 소요 시간: 약 3~5분
     */
    @Test
    fun `빠른 평가 — 카테고리 대표 7종`() {
        QUICK_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 빠른 평가 ${index + 1}: ${sample.description} =====")

            try {
                val prompt = promptBuilder.build(
                    diffContent = sample.diff,
                    contributing = null,
                    prs = emptyList(),
                    issueTitle = sample.issueTitle,
                    issueContent = sample.issueContent,
                    issueGuideline = sample.issueGuideline
                )
                val result = aiClient.generatePrDraft(prompt)
                println("제목: ${result.title}")
                println("본문 미리보기: ${result.body.take(100)}...")
            } catch (e: Exception) {
                println("빠른 평가 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            if (index < QUICK_SAMPLES.lastIndex) Thread.sleep(10_000)
        }

        Thread.sleep(60_000)
    }

    /**
     * 20종의 고정 diff 샘플로 프롬프트를 평가합니다.
     * 총 소요 시간: 약 7분 (샘플 간 10초 대기 + judge 60초)
     */
    @Test
    fun `고정 샘플 전체로 프롬프트 평가`() {
        EVAL_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 샘플 ${index + 1}: ${sample.description} =====")

            try {
                val prompt = promptBuilder.build(
                    diffContent = sample.diff,
                    contributing = null,
                    prs = emptyList(),
                    issueTitle = sample.issueTitle,
                    issueContent = sample.issueContent,
                    issueGuideline = sample.issueGuideline
                )

                val result = aiClient.generatePrDraft(prompt)

                println("제목: ${result.title}")
                println("본문 미리보기: ${result.body.take(100)}...")
            } catch (e: Exception) {
                println("샘플 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            // 연속 호출 시 rate limit 방지
            if (index < EVAL_SAMPLES.lastIndex) Thread.sleep(10_000)
        }

        // Langfuse fire-and-forget 전송 + LLM judge 채점(별도 스레드) 완료 대기
        Thread.sleep(60_000)
    }

    /**
     * 엣지케이스 샘플만 골라서 집중 평가합니다.
     * 프롬프트가 무너지기 쉬운 케이스(공백만 변경, 빌드 파일, 삭제 위주 등)를
     * 빠르게 확인할 때 사용합니다. 총 소요 시간: 약 3분
     */
    @Test
    fun `엣지케이스 집중 평가`() {
        EDGE_CASE_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 엣지 ${index + 1}: ${sample.description} =====")

            try {
                val prompt = promptBuilder.build(
                    diffContent = sample.diff,
                    contributing = null,
                    prs = emptyList(),
                    issueTitle = sample.issueTitle,
                    issueContent = sample.issueContent,
                    issueGuideline = sample.issueGuideline
                )
                val result = aiClient.generatePrDraft(prompt)
                println("제목: ${result.title}")
                println("본문 미리보기: ${result.body.take(100)}...")
            } catch (e: Exception) {
                println("엣지 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            if (index < EDGE_CASE_SAMPLES.lastIndex) Thread.sleep(10_000)
        }

        Thread.sleep(60_000)
    }

    /**
     * 대규모 diff 샘플로 집중 평가합니다.
     * 신규 기능·다중 파일 변경 시 프롬프트 품질을 확인할 때 사용합니다.
     */
    @Test
    fun `대규모 diff 집중 평가`() {
        LARGE_DIFF_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 대형 ${index + 1}: ${sample.description} =====")

            try {
                val prompt = promptBuilder.build(
                    diffContent = sample.diff,
                    contributing = null,
                    prs = emptyList(),
                    issueTitle = sample.issueTitle,
                    issueContent = sample.issueContent,
                    issueGuideline = sample.issueGuideline
                )
                val result = aiClient.generatePrDraft(prompt)
                println("제목: ${result.title}")
                println("본문 미리보기: ${result.body.take(200)}...")
            } catch (e: Exception) {
                println("대형 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            if (index < LARGE_DIFF_SAMPLES.lastIndex) Thread.sleep(10_000)
        }

        Thread.sleep(60_000)
    }

    /**
     * 기본 CONTRIBUTING.md 컨텍스트로 샘플 1을 평가합니다.
     */
    @Test
    fun `CONTRIBUTING md 컨텍스트로 프롬프트 평가`() {
        val sample = EVAL_SAMPLES[0]
        try {
            val prompt = promptBuilder.build(
                diffContent = sample.diff,
                contributing = SAMPLE_CONTRIBUTING,
                prs = emptyList(),
                issueTitle = sample.issueTitle,
                issueContent = sample.issueContent,
                issueGuideline = sample.issueGuideline
            )
            val result = aiClient.generatePrDraft(prompt)
            println("제목: ${result.title}")
            println("본문 미리보기: ${result.body.take(200)}...")
        } catch (e: Exception) {
            println("CONTRIBUTING 테스트 실패 (건너뜀): ${e.message}")
        }
        Thread.sleep(60_000)
    }

    /**
     * 매우 긴 CONTRIBUTING.md로 프롬프트 품질을 평가합니다.
     * 컨텍스트가 길 때 AI가 지시를 제대로 따르는지 확인합니다.
     */
    @Test
    fun `긴 CONTRIBUTING md 컨텍스트로 프롬프트 평가`() {
        EVAL_SAMPLES.take(3).forEachIndexed { index, sample ->
            println("\n===== 긴 CONTRIBUTING / 샘플 ${index + 1}: ${sample.description} =====")

            try {
                val prompt = promptBuilder.build(
                    diffContent = sample.diff,
                    contributing = SAMPLE_CONTRIBUTING_LONG,
                    prs = emptyList(),
                    issueTitle = sample.issueTitle,
                    issueContent = sample.issueContent,
                    issueGuideline = sample.issueGuideline
                )
                val result = aiClient.generatePrDraft(prompt)
                println("제목: ${result.title}")
                println("본문 미리보기: ${result.body.take(200)}...")
            } catch (e: Exception) {
                println("샘플 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            if (index < 2) Thread.sleep(10_000)
        }
        Thread.sleep(60_000)
    }

    /**
     * 기존 PR 예시 컨텍스트로 프롬프트를 평가합니다.
     */
    @Test
    fun `기존 PR 컨텍스트로 프롬프트 평가`() {
        val sample = EVAL_SAMPLES[1]
        try {
            val prompt = promptBuilder.build(
                diffContent = sample.diff,
                contributing = null,
                prs = SAMPLE_PRS,
                issueTitle = sample.issueTitle,
                issueContent = sample.issueContent,
                issueGuideline = sample.issueGuideline
            )
            val result = aiClient.generatePrDraft(prompt)
            println("제목: ${result.title}")
            println("본문 미리보기: ${result.body.take(200)}...")
        } catch (e: Exception) {
            println("기존 PR 테스트 실패 (건너뜀): ${e.message}")
        }
        Thread.sleep(60_000)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 고정 평가 샘플 (같은 샘플로 버전 간 비교해야 의미 있음 — 임의로 수정 금지)
    // ──────────────────────────────────────────────────────────────────────────
    companion object {

        @JvmStatic
        @DynamicPropertySource
        fun loadDotEnv(registry: DynamicPropertyRegistry) {
            val envFile = File(".env").takeIf { it.exists() }
                ?: File("../.env").takeIf { it.exists() }
                ?: return

            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith('#')) {
                    val idx = trimmed.indexOf('=')
                    if (idx > 0) {
                        val key = trimmed.substring(0, idx).trim()
                        val value = trimmed.substring(idx + 1).trim()
                        registry.add(key) { value }
                    }
                }
            }
        }

        val SAMPLE_CONTRIBUTING = """
            # Contributing Guide

            ## PR 제목
            - feat:, fix:, refactor:, chore:, docs: 중 하나로 시작, 50자 이내

            ## PR 본문
            - 변경 이유(Why), 수정 내용(What), 테스트 방법(How to Test) 순으로 작성
            - 수정 내용은 bullet point로, 기술적 세부사항(타입, 메서드 시그니처 등) 포함
            - 테스트 방법은 엔드포인트·파라미터·예상 응답을 명시
        """.trimIndent()

        /**
         * 섹션이 많고 분량이 긴 CONTRIBUTING.md 샘플.
         * 대형 오픈소스 프로젝트 수준의 가이드를 시뮬레이션합니다.
         */
        val SAMPLE_CONTRIBUTING_LONG = """
            # Contributing Guide

            이 가이드는 프로젝트에 기여하기 위한 전체 절차를 설명합니다.

            ## 개발 환경 설정
            - JDK 17 이상, Gradle 8.x 필요
            - `./gradlew bootRun --args='--spring.profiles.active=local'`로 로컬 실행
            - `.env.example`을 복사해 `.env`를 만들고 값을 채웁니다

            ## 브랜치 전략
            - `main`: 프로덕션 릴리즈 전용, 직접 push 금지
            - `develop`: 통합 브랜치
            - 기능 브랜치: `feat/이슈번호-간단한-설명` 형식

            ## 커밋 메시지 규칙
            - type: feat | fix | refactor | chore | docs | test | perf
            - scope: 변경 대상 모듈 (예: auth, user, pr-draft)
            - subject: 50자 이내, 명령형, 현재 시제

            ## PR 제목 규칙
            - `<type>: <변경 내용 요약>` 형식, 최대 50자
            - 타입: feat / fix / refactor / chore / docs / test / perf

            ## PR 본문 필수 항목
            ### 변경 이유 (Why)
            - 이 PR이 필요한 이유를 1~3줄로 설명
            - 관련 이슈 번호 링크: `close #이슈번호`

            ### 수정 내용 (What)
            - 주요 변경 사항을 bullet point로 나열
            - 클래스/메서드/파라미터 변경 시 이전·이후 시그니처를 함께 명시
            - DB 스키마 변경이 있으면 마이그레이션 파일명을 기재

            ### 테스트 방법 (How to Test)
            - 재현 가능한 단계 (curl 예시 또는 테스트 클래스/메서드명)
            - 예상 결과 명시
            - 사이드이펙트가 있는 경우 회귀 테스트 범위 기재

            ## 코드 스타일
            - Kotlin 공식 컨벤션 준수
            - KDoc 주석: public API에는 반드시 작성
            - 함수 길이 30줄 초과 시 분리 고려

            ## 리뷰 기준
            - 단일 책임 원칙 준수 여부
            - 예외 처리의 적절성
            - 테스트 커버리지 (신규 로직은 단위 테스트 필수)
            - N+1 쿼리 여부

            ## 릴리즈 프로세스
            - `develop` → `main` PR은 팀 리뷰 2명 이상 필수
            - 배포 후 Langfuse에서 latency·token 이상 여부 확인
        """.trimIndent()

        val SAMPLE_PRS = listOf(
            GitHubPrRes(
                title = "feat: 사용자 프로필 이미지 업로드 기능 추가",
                body = """
                    ## 변경 이유
                    프로필 이미지 설정 불가로 UX 불편 피드백 반영

                    ## 수정 내용
                    - `UserController`에 `POST /api/users/profile-image` 추가
                    - `MultipartFile` → S3 업로드 후 URL을 `User.profileImageUrl`에 저장
                    - 파일 크기 5MB, 확장자 jpg/png/webp 제한

                    ## 테스트 방법
                    POST /api/users/profile-image (multipart/form-data)
                    Expected: 200 OK { "imageUrl": "https://..." }
                """.trimIndent(),
                mergedAt = "2026-04-01T10:00:00Z"
            ),
            GitHubPrRes(
                title = "fix: 토큰 만료 시 갱신 실패 버그 수정",
                body = """
                    ## 변경 이유
                    리프레시 토큰 재발급 시 NullPointerException 발생

                    ## 수정 내용
                    - `AuthService.refresh()`에 null 체크 추가 → `InvalidTokenException` 반환
                    - `RefreshToken` 타입 String → String?으로 변경

                    ## 테스트 방법
                    POST /api/auth/refresh Body: { "refreshToken": null }
                    Expected: 401 { "message": "유효하지 않은 토큰입니다" }
                """.trimIndent(),
                mergedAt = "2026-04-10T14:00:00Z"
            ),
            GitHubPrRes(
                title = "perf: 이슈 목록 조회 N+1 쿼리 개선",
                body = """
                    ## 변경 이유
                    이슈 목록 조회 시 참여자 정보 로드에 N+1 쿼리 발생
                    100건 기준 120개 쿼리 → 2개로 감소

                    ## 수정 내용
                    - `IssueRepository.findAll()`에 `@EntityGraph(attributePaths = ["participants"])` 적용
                    - 페치 전략 LAZY → EAGER (participants 한정)

                    ## 테스트 방법
                    GET /api/issues?page=0&size=20
                    Langfuse에서 쿼리 수 모니터링
                """.trimIndent(),
                mergedAt = "2026-04-20T09:00:00Z"
            )
        )

        data class EvalSample(
            val description: String,
            val diff: String,
            val issueTitle: String? = null,
            val issueContent: String? = null,
            val issueGuideline: String? = null
        )

        val EVAL_SAMPLES = listOf(

            // ────────── 샘플 1~5: 기본 케이스 (v1부터의 고정 기준선, 수정 금지) ──────────

            // 샘플 1: 단순 버그 수정 (null 안전성)
            EvalSample(
                description = "null 안전성 버그 수정",
                issueTitle = "getUserProfile() 호출 시 NullPointerException 발생",
                issueContent = "name 또는 email이 null인 사용자 조회 시 UserProfileRes 생성 단계에서 NPE 발생. null 안전 처리 및 예외 메시지 개선 필요.",
                issueGuideline = "UserService.getUserProfile()에서 User.name·User.email이 nullable임에도 직접 접근해 UserProfileRes 생성 시 NPE가 발생함. orElseThrow 예외를 RuntimeException 대신 도메인 예외(UserNotFoundException)로 교체하고, 반환 시 엘비스 연산자(?:)로 null 기본값을 처리할 것. UserProfileRes 생성자 파라미터 타입 변경 여부도 함께 확인 필요.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/UserService.kt b/src/main/kotlin/com/example/service/UserService.kt
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/kotlin/com/example/service/UserService.kt
                    +++ b/src/main/kotlin/com/example/service/UserService.kt
                    @@ -24,7 +24,9 @@ class UserService(
                         fun getUserProfile(userId: Long): UserProfileRes {
                             val user = userRepository.findById(userId)
                    -            .orElseThrow { RuntimeException("User not found") }
                    +            .orElseThrow { UserNotFoundException("존재하지 않는 사용자입니다: userId=${'$'}userId") }
                    -        return UserProfileRes(user.name, user.email)
                    +        return UserProfileRes(
                    +            name = user.name ?: "Unknown",
                    +            email = user.email ?: ""
                    +        )
                         }
                    }
                """.trimIndent()
            ),

            // 샘플 2: 신규 기능 (페이지네이션 추가)
            EvalSample(
                description = "목록 조회 API에 페이지네이션 추가",
                issueTitle = "게시글 목록 API 데이터 증가 시 응답 속도 저하",
                issueContent = "게시글 수가 늘어나면서 /api/posts 응답 시간이 점점 길어지고 있음. page, size 파라미터를 받아 페이지 단위로 반환하도록 수정 요청.",
                issueGuideline = "PostController.getPosts()가 전체 목록을 List로 반환해 데이터 증가 시 메모리 과부하가 발생할 수 있음. 컨트롤러에 @RequestParam으로 page·size를 받아 PageRequest를 생성하고, PostService와 PostRepository의 반환 타입을 Page<PostRes>로 변경해야 함. 응답 타입 변경으로 API 클라이언트의 totalElements 처리 로직도 함께 수정 필요.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/controller/PostController.kt b/src/main/kotlin/com/example/controller/PostController.kt
                    index b2c3d4e..f5g6h7i 100644
                    --- a/src/main/kotlin/com/example/controller/PostController.kt
                    +++ b/src/main/kotlin/com/example/controller/PostController.kt
                    @@ -12,8 +12,12 @@ class PostController(private val postService: PostService) {
                    -    @GetMapping
                    -    fun getPosts(): ResponseEntity<List<PostRes>> {
                    -        return ResponseEntity.ok(postService.getPosts())
                    +    @GetMapping
                    +    fun getPosts(
                    +        @RequestParam(defaultValue = "0") page: Int,
                    +        @RequestParam(defaultValue = "20") size: Int
                    +    ): ResponseEntity<Page<PostRes>> {
                    +        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
                    +        return ResponseEntity.ok(postService.getPosts(pageable))
                         }
                    diff --git a/src/main/kotlin/com/example/service/PostService.kt b/src/main/kotlin/com/example/service/PostService.kt
                    @@ -8,6 +8,6 @@ class PostService(private val postRepository: PostRepository) {
                    -    fun getPosts(): List<PostRes> =
                    -        postRepository.findAll().map { PostRes.from(it) }
                    +    fun getPosts(pageable: Pageable): Page<PostRes> =
                    +        postRepository.findAll(pageable).map { PostRes.from(it) }
                    }
                """.trimIndent()
            ),

            // 샘플 3: 리팩토링 (중복 코드 추출)
            EvalSample(
                description = "공통 검증 로직 메서드 추출 리팩토링",
                issueTitle = "OrderService 주문 조회 + 권한 검증 코드 중복",
                issueContent = "cancelOrder, getOrderDetail 두 메서드에서 동일한 주문 조회 및 소유자 확인 로직이 반복됨. 공통 private 메서드로 추출하여 중복 제거 필요.",
                issueGuideline = "OrderService.cancelOrder()와 getOrderDetail()에서 주문 조회(findById + orElseThrow) 및 소유권 검증(order.userId != userId) 로직이 중복됨. 해당 공통 로직을 private 메서드로 추출하고 두 메서드에서 호출하도록 변경할 것. 추출된 메서드는 검증 실패 시 ForbiddenException을 던지고 Order를 반환하는 형태로 작성.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/OrderService.kt b/src/main/kotlin/com/example/service/OrderService.kt
                    index c3d4e5f..g6h7i8j 100644
                    --- a/src/main/kotlin/com/example/service/OrderService.kt
                    +++ b/src/main/kotlin/com/example/service/OrderService.kt
                    @@ -15,20 +15,16 @@ class OrderService(private val orderRepository: OrderRepository) {
                         fun cancelOrder(userId: Long, orderId: Long) {
                    -        val order = orderRepository.findById(orderId)
                    -            .orElseThrow { OrderNotFoundException(orderId) }
                    -        if (order.userId != userId) throw ForbiddenException()
                    +        val order = getOrderOrThrow(userId, orderId)
                             order.cancel()
                             orderRepository.save(order)
                         }

                         fun getOrderDetail(userId: Long, orderId: Long): OrderDetailRes {
                    -        val order = orderRepository.findById(orderId)
                    -            .orElseThrow { OrderNotFoundException(orderId) }
                    -        if (order.userId != userId) throw ForbiddenException()
                    +        val order = getOrderOrThrow(userId, orderId)
                             return OrderDetailRes.from(order)
                         }
                    +
                    +    private fun getOrderOrThrow(userId: Long, orderId: Long): Order {
                    +        val order = orderRepository.findById(orderId)
                    +            .orElseThrow { OrderNotFoundException(orderId) }
                    +        if (order.userId != userId) throw ForbiddenException()
                    +        return order
                    +    }
                    }
                """.trimIndent()
            ),

            // 샘플 4: 성능 개선 (캐싱 적용)
            EvalSample(
                description = "레포지토리 정보 조회에 캐싱 적용",
                issueTitle = "레포지토리 정보 반복 조회 시 DB 부하",
                issueContent = "대시보드 렌더링 시 동일한 레포지토리 정보를 여러 번 조회하는데 매번 DB 쿼리가 발생함. 캐싱 레이어 적용으로 부하 감소 필요.",
                issueGuideline = "RepoService.getRepoInfo()가 매 호출마다 DB를 조회하고 있음. @Cacheable(value=[\"repoInfo\"], key=\"#repoId\")를 적용해 첫 조회 결과를 캐시하고, 레포 정보 변경 시 @CacheEvict로 무효화하는 메서드를 추가할 것. 설정 클래스에 @EnableCaching이 없으면 함께 추가 필요.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/RepoService.kt b/src/main/kotlin/com/example/service/RepoService.kt
                    index d4e5f6g..h7i8j9k 100644
                    --- a/src/main/kotlin/com/example/service/RepoService.kt
                    +++ b/src/main/kotlin/com/example/service/RepoService.kt
                    @@ -1,10 +1,13 @@
                    +import org.springframework.cache.annotation.Cacheable
                    +import org.springframework.cache.annotation.CacheEvict
                    +
                     @Service
                    +@EnableCaching
                     class RepoService(private val repoRepository: RepoRepository) {

                    +    @Cacheable(value = ["repoInfo"], key = "#repoId")
                         fun getRepoInfo(repoId: Long): RepoInfoRes {
                             return repoRepository.findById(repoId)
                                 .map { RepoInfoRes.from(it) }
                                 .orElseThrow { RepoNotFoundException(repoId) }
                         }
                    +
                    +    @CacheEvict(value = ["repoInfo"], key = "#repoId")
                    +    fun invalidateCache(repoId: Long) = Unit
                     }
                """.trimIndent()
            ),

            // 샘플 5: 보안 수정 (입력값 검증 추가)
            EvalSample(
                description = "API 요청 입력값 검증 추가",
                issueTitle = "댓글 생성 API 입력값 검증 없음",
                issueContent = "현재 댓글 내용에 빈 문자열이나 500자를 초과하는 텍스트도 저장 가능함. Jakarta Validation(@NotBlank, @Size)을 적용하고 컨트롤러에 @Valid 추가 필요.",
                issueGuideline = "CreateCommentReq의 content 필드에 @field:NotBlank와 @field:Size(max=500), postId에 @field:Positive를 추가하고 CommentController.createComment()의 @RequestBody 앞에 @Valid를 붙여야 함. 유효성 검증 실패 시 400 응답을 반환하는 GlobalExceptionHandler의 MethodArgumentNotValidException 핸들러가 있는지도 확인 필요.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/dto/CreateCommentReq.kt b/src/main/kotlin/com/example/dto/CreateCommentReq.kt
                    index e5f6g7h..i8j9k0l 100644
                    --- a/src/main/kotlin/com/example/dto/CreateCommentReq.kt
                    +++ b/src/main/kotlin/com/example/dto/CreateCommentReq.kt
                    @@ -1,7 +1,12 @@
                    +import jakarta.validation.constraints.NotBlank
                    +import jakarta.validation.constraints.Size
                    +
                     data class CreateCommentReq(
                    -    val content: String,
                    -    val postId: Long
                    +    @field:NotBlank(message = "댓글 내용은 필수입니다.")
                    +    @field:Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하여야 합니다.")
                    +    val content: String,
                    +
                    +    @field:Positive(message = "게시글 ID는 양수여야 합니다.")
                    +    val postId: Long
                     )
                    diff --git a/src/main/kotlin/com/example/controller/CommentController.kt b/src/main/kotlin/com/example/controller/CommentController.kt
                    @@ -8,6 +8,7 @@ class CommentController(private val commentService: CommentService) {
                         @PostMapping
                         fun createComment(
                    +        @Valid
                             @RequestBody request: CreateCommentReq
                         ): ResponseEntity<CommentRes> {
                             return ResponseEntity.ok(commentService.create(request))
                """.trimIndent()
            ),

            // ────────── 샘플 6~7: 극소 diff ──────────

            // 샘플 6: 에러 메시지 한 줄 수정
            EvalSample(
                description = "에러 메시지 한 줄 수정 (극소 diff — 가장 쉬운 케이스)",
                issueTitle = "사용자 미존재 에러 메시지 영문으로 노출됨",
                issueContent = "USER_NOT_FOUND 에러 발생 시 클라이언트에 'User not found' 영문 메시지가 그대로 노출됨. 한국어로 변경 요청.",
                issueGuideline = "ErrorCode enum의 USER_NOT_FOUND 값에 설정된 message 파라미터 'User not found'를 한국어로 변경하면 됨. 다른 영문 메시지 항목도 함께 확인하여 일관성을 맞출 것.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/exception/ErrorCode.kt b/src/main/kotlin/com/example/exception/ErrorCode.kt
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/kotlin/com/example/exception/ErrorCode.kt
                    +++ b/src/main/kotlin/com/example/exception/ErrorCode.kt
                    @@ -8,7 +8,7 @@ enum class ErrorCode(val message: String) {
                    -    USER_NOT_FOUND("User not found"),
                    +    USER_NOT_FOUND("요청한 사용자를 찾을 수 없습니다"),
                         FORBIDDEN("접근 권한이 없습니다"),
                         INVALID_TOKEN("유효하지 않은 토큰입니다")
                    }
                """.trimIndent()
            ),

            // 샘플 7: 설정값 상수 변경
            EvalSample(
                description = "JWT 액세스 토큰 만료 시간 1시간 → 24시간 변경 (극소 diff)",
                issueTitle = "액세스 토큰 만료 시간이 짧아 사용 중 자동 로그아웃 발생",
                issueContent = "현재 액세스 토큰 TTL이 1시간으로 설정되어 있어 장시간 사용 중 세션이 끊기는 불편이 있음. 24시간으로 연장 요청.",
                issueGuideline = "JwtProperties의 accessExpirationMs 기본값 3_600_000(1시간)을 86_400_000(24시간)으로 변경하면 됨. 보안 정책상 만료 시간 연장의 적절성을 검토하고, 필요 시 리프레시 토큰 로직과의 균형도 확인할 것.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/config/JwtProperties.kt b/src/main/kotlin/com/example/config/JwtProperties.kt
                    index b2c3d4e..f5g6h7i 100644
                    --- a/src/main/kotlin/com/example/config/JwtProperties.kt
                    +++ b/src/main/kotlin/com/example/config/JwtProperties.kt
                    @@ -3,6 +3,6 @@
                     @ConfigurationProperties(prefix = "jwt")
                     data class JwtProperties(
                         val secret: String,
                    -    val accessExpirationMs: Long = 3_600_000L,    // 1시간
                    +    val accessExpirationMs: Long = 86_400_000L,   // 24시간
                         val refreshExpirationMs: Long = 1_209_600_000L // 2주
                     )
                """.trimIndent()
            ),

            // ────────── 샘플 8~9: 대형 diff ──────────

            // 샘플 8: 알림 서비스 신규 구현
            EvalSample(
                description = "알림 기능 신규 구현 (entity + service + controller, 대형 diff)",
                issueTitle = "알림 기능 구현 — 읽지 않은 알림 조회 및 읽음 처리",
                issueContent = "사용자에게 읽지 않은 알림 목록 조회(GET /notifications/unread), 전체 읽음 처리(PATCH /notifications/read-all), 단건 읽음 처리(PATCH /notifications/{id}/read) API가 필요함. Notification entity, service, controller 신규 구현.",
                issueGuideline = "Notification entity는 User와 ManyToOne(LAZY), type은 Enum(STRING), isRead 기본값 false로 설계할 것. NotificationService에 읽지 않은 알림 조회(@Transactional(readOnly=true)), 전체 읽음 벌크 처리, 단건 읽음(소유권 검증 후 markAsRead 호출) 메서드를 구현. NotificationController에서 @AuthenticationPrincipal로 userId를 추출해 서비스에 위임하는 방식으로 작성.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/domain/notification/Notification.kt b/src/main/kotlin/com/example/domain/notification/Notification.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/notification/Notification.kt
                    @@ -0,0 +1,16 @@
                    +package com.example.domain.notification
                    +
                    +import jakarta.persistence.*
                    +
                    +@Entity
                    +class Notification(
                    +    @ManyToOne(fetch = FetchType.LAZY) val user: User,
                    +    @Enumerated(EnumType.STRING) val type: NotificationType,
                    +    val payload: String,
                    +    var isRead: Boolean = false,
                    +    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0L
                    +) {
                    +    fun markAsRead() { isRead = true }
                    +}
                    diff --git a/src/main/kotlin/com/example/domain/notification/NotificationService.kt b/src/main/kotlin/com/example/domain/notification/NotificationService.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/notification/NotificationService.kt
                    @@ -0,0 +1,36 @@
                    +package com.example.domain.notification
                    +
                    +import org.springframework.stereotype.Service
                    +import org.springframework.transaction.annotation.Transactional
                    +
                    +@Service
                    +class NotificationService(
                    +    private val notificationRepository: NotificationRepository,
                    +    private val userRepository: UserRepository
                    +) {
                    +
                    +    @Transactional(readOnly = true)
                    +    fun getUnread(userId: Long): List<NotificationRes> {
                    +        val user = userRepository.findById(userId)
                    +            .orElseThrow { UserNotFoundException(userId) }
                    +        return notificationRepository
                    +            .findAllByUserAndIsReadFalseOrderByCreatedAtDesc(user)
                    +            .map { NotificationRes.from(it) }
                    +    }
                    +
                    +    @Transactional
                    +    fun markAllAsRead(userId: Long) {
                    +        val user = userRepository.findById(userId)
                    +            .orElseThrow { UserNotFoundException(userId) }
                    +        notificationRepository.markAllAsReadByUser(user.id)
                    +    }
                    +
                    +    @Transactional
                    +    fun markAsRead(userId: Long, notificationId: Long) {
                    +        val notification = notificationRepository.findById(notificationId)
                    +            .orElseThrow { NotificationNotFoundException(notificationId) }
                    +        if (notification.user.id != userId) throw ForbiddenException()
                    +        notification.markAsRead()
                    +    }
                    +}
                    diff --git a/src/main/kotlin/com/example/domain/notification/NotificationController.kt b/src/main/kotlin/com/example/domain/notification/NotificationController.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/notification/NotificationController.kt
                    @@ -0,0 +1,26 @@
                    +package com.example.domain.notification
                    +
                    +import org.springframework.http.ResponseEntity
                    +import org.springframework.security.core.annotation.AuthenticationPrincipal
                    +import org.springframework.web.bind.annotation.*
                    +
                    +@RestController
                    +@RequestMapping("/api/v1/notifications")
                    +class NotificationController(private val notificationService: NotificationService) {
                    +
                    +    @GetMapping("/unread")
                    +    fun getUnread(@AuthenticationPrincipal principal: OAuthPrincipal): ResponseEntity<List<NotificationRes>> =
                    +        ResponseEntity.ok(notificationService.getUnread(principal.userId))
                    +
                    +    @PatchMapping("/read-all")
                    +    fun markAllAsRead(@AuthenticationPrincipal principal: OAuthPrincipal): ResponseEntity<Void> {
                    +        notificationService.markAllAsRead(principal.userId)
                    +        return ResponseEntity.noContent().build()
                    +    }
                    +
                    +    @PatchMapping("/{id}/read")
                    +    fun markAsRead(
                    +        @AuthenticationPrincipal principal: OAuthPrincipal,
                    +        @PathVariable id: Long
                    +    ): ResponseEntity<Void> {
                    +        notificationService.markAsRead(principal.userId, id)
                    +        return ResponseEntity.noContent().build()
                    +    }
                    +}
                """.trimIndent()
            ),

            // 샘플 9: 댓글 기능 전체 구현 (entity + dto + repository + service + controller)
            EvalSample(
                description = "댓글 기능 전체 구현 (5개 파일 신규 추가, 가장 큰 diff)",
                issueTitle = "게시글 댓글 CRUD 기능 구현",
                issueContent = "게시글에 댓글 작성(POST /comments), 게시글별 댓글 목록 조회(GET /comments/post/{postId}) 기능 필요. Comment entity, DTO, Repository, Service, Controller 전체 신규 구현.",
                issueGuideline = "Comment entity는 Post·User와 각각 ManyToOne(LAZY), content는 @Column(TEXT)로 설계. CommentService에 댓글 생성(Post·User 조회 후 Comment 저장)과 게시글별 목록 조회(createdAt 내림차순) 메서드를 구현. CommentController는 @AuthenticationPrincipal로 작성자 userId를 받아 서비스에 전달하고, 조회 엔드포인트는 /post/{postId} 하위로 구성.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/domain/comment/Comment.kt b/src/main/kotlin/com/example/domain/comment/Comment.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/comment/Comment.kt
                    @@ -0,0 +1,13 @@
                    +package com.example.domain.comment
                    +
                    +import jakarta.persistence.*
                    +import java.time.Instant
                    +
                    +@Entity
                    +class Comment(
                    +    @ManyToOne(fetch = FetchType.LAZY) val post: Post,
                    +    @ManyToOne(fetch = FetchType.LAZY) val author: User,
                    +    @Column(columnDefinition = "TEXT") var content: String,
                    +    val createdAt: Instant = Instant.now(),
                    +    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0L
                    +)
                    diff --git a/src/main/kotlin/com/example/domain/comment/CommentDto.kt b/src/main/kotlin/com/example/domain/comment/CommentDto.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/comment/CommentDto.kt
                    @@ -0,0 +1,13 @@
                    +package com.example.domain.comment
                    +
                    +data class CreateCommentReq(val content: String, val postId: Long)
                    +
                    +data class CommentRes(
                    +    val id: Long,
                    +    val content: String,
                    +    val authorName: String,
                    +    val createdAt: String
                    +) {
                    +    companion object {
                    +        fun from(c: Comment) = CommentRes(c.id, c.content, c.author.name, c.createdAt.toString())
                    +    }
                    +}
                    diff --git a/src/main/kotlin/com/example/domain/comment/CommentRepository.kt b/src/main/kotlin/com/example/domain/comment/CommentRepository.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/comment/CommentRepository.kt
                    @@ -0,0 +1,6 @@
                    +package com.example.domain.comment
                    +
                    +import org.springframework.data.jpa.repository.JpaRepository
                    +
                    +interface CommentRepository : JpaRepository<Comment, Long> {
                    +    fun findAllByPostIdOrderByCreatedAtDesc(postId: Long): List<Comment>
                    +}
                    diff --git a/src/main/kotlin/com/example/domain/comment/CommentService.kt b/src/main/kotlin/com/example/domain/comment/CommentService.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/comment/CommentService.kt
                    @@ -0,0 +1,22 @@
                    +package com.example.domain.comment
                    +
                    +import org.springframework.stereotype.Service
                    +import org.springframework.transaction.annotation.Transactional
                    +
                    +@Service
                    +class CommentService(
                    +    private val commentRepository: CommentRepository,
                    +    private val postRepository: PostRepository,
                    +    private val userRepository: UserRepository
                    +) {
                    +
                    +    @Transactional
                    +    fun create(userId: Long, req: CreateCommentReq): CommentRes {
                    +        val post = postRepository.findById(req.postId).orElseThrow { PostNotFoundException(req.postId) }
                    +        val author = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
                    +        return CommentRes.from(commentRepository.save(Comment(post, author, req.content)))
                    +    }
                    +
                    +    @Transactional(readOnly = true)
                    +    fun list(postId: Long): List<CommentRes> =
                    +        commentRepository.findAllByPostIdOrderByCreatedAtDesc(postId).map { CommentRes.from(it) }
                    +}
                    diff --git a/src/main/kotlin/com/example/domain/comment/CommentController.kt b/src/main/kotlin/com/example/domain/comment/CommentController.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/domain/comment/CommentController.kt
                    @@ -0,0 +1,18 @@
                    +package com.example.domain.comment
                    +
                    +import org.springframework.http.ResponseEntity
                    +import org.springframework.security.core.annotation.AuthenticationPrincipal
                    +import org.springframework.web.bind.annotation.*
                    +
                    +@RestController
                    +@RequestMapping("/api/v1/comments")
                    +class CommentController(private val commentService: CommentService) {
                    +
                    +    @PostMapping
                    +    fun create(
                    +        @AuthenticationPrincipal principal: OAuthPrincipal,
                    +        @RequestBody req: CreateCommentReq
                    +    ): ResponseEntity<CommentRes> =
                    +        ResponseEntity.ok(commentService.create(principal.userId, req))
                    +
                    +    @GetMapping("/post/{postId}")
                    +    fun list(@PathVariable postId: Long): ResponseEntity<List<CommentRes>> =
                    +        ResponseEntity.ok(commentService.list(postId))
                    +}
                """.trimIndent()
            ),

            // ────────── 샘플 10~12: 애매한 케이스 ──────────

            // 샘플 10: 정렬 기준 변경 (왜 바꿨는지 코드만으로 불분명)
            EvalSample(
                description = "이슈 목록 정렬 기준 변경 (애매 — 변경 이유가 코드만으로 불분명)",
                issueTitle = "이슈 목록을 최근 활동 순으로 정렬 변경 요청",
                issueContent = "현재 이슈 목록이 생성일(createdAt) 기준으로 정렬되는데, 최근에 댓글이 달리거나 수정된 이슈가 위에 보여야 한다는 피드백이 있음. updatedAt 기준으로 변경 요청.",
                issueGuideline = "IssueService.getIssues()의 PageRequest 생성 시 Sort.by(\"createdAt\")를 Sort.by(\"updatedAt\")으로 변경하면 됨. issues 테이블의 updated_at 컬럼에 인덱스가 존재하는지 확인하고, 없으면 마이그레이션으로 추가하는 것을 권장.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/IssueService.kt b/src/main/kotlin/com/example/service/IssueService.kt
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/kotlin/com/example/service/IssueService.kt
                    +++ b/src/main/kotlin/com/example/service/IssueService.kt
                    @@ -14,7 +14,7 @@ class IssueService(private val issueRepository: IssueRepository) {
                         fun getIssues(pageable: Pageable): Page<IssueRes> {
                    -        val sorted = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by("createdAt").descending())
                    +        val sorted = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by("updatedAt").descending())
                             return issueRepository.findAll(sorted).map { IssueRes.from(it) }
                         }
                    }
                """.trimIndent()
            ),

            // 샘플 11: 트랜잭션 전파 전략 변경 (성능·정확성 트레이드오프인지 코드만으론 불분명)
            EvalSample(
                description = "결제 감사 로그 트랜잭션 전파 전략 변경 (애매 — REQUIRES_NEW 이유 불분명)",
                issueTitle = "결제 실패 시 감사 로그도 함께 롤백되어 감사 추적 불가",
                issueContent = "결제 처리 트랜잭션이 롤백될 때 같은 트랜잭션에 묶인 감사 로그도 삭제됨. 감사 로그는 결제 성공/실패 여부와 무관하게 항상 커밋되어야 함. REQUIRES_NEW 전파 전략 적용 필요.",
                issueGuideline = "PaymentService.recordAuditLog()의 @Transactional을 @Transactional(propagation = Propagation.REQUIRES_NEW)로 변경해 독립 트랜잭션에서 항상 커밋되도록 할 것. REQUIRES_NEW는 새 커넥션을 사용하므로 커넥션 풀 크기가 충분한지 확인하고, 부모 트랜잭션이 락을 보유한 경우 데드락 가능성도 검토 필요.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/PaymentService.kt b/src/main/kotlin/com/example/service/PaymentService.kt
                    index b2c3d4e..f5g6h7i 100644
                    --- a/src/main/kotlin/com/example/service/PaymentService.kt
                    +++ b/src/main/kotlin/com/example/service/PaymentService.kt
                    @@ -10,8 +10,8 @@ class PaymentService(
                         private val auditLogRepository: AuditLogRepository
                     ) {
                    -    @Transactional
                    +    @Transactional(propagation = Propagation.REQUIRES_NEW)
                         fun recordAuditLog(paymentId: Long, action: String) {
                             auditLogRepository.save(AuditLog(paymentId = paymentId, action = action))
                         }
                    }
                """.trimIndent()
            ),

            // 샘플 12: 설정 파일 + 서비스 로직 + DTO 동시 수정 (연관성 파악 필요)
            EvalSample(
                description = "파일 업로드 용량 제한 상향 (설정 + 서비스 + DTO 동시 수정, 애매)",
                issueTitle = "파일 업로드 5MB 제한으로 고화질 이미지 업로드 불가",
                issueContent = "현재 파일 업로드 최대 크기가 5MB로 제한되어 있어 고화질 이미지(보통 5~10MB)를 업로드할 수 없음. 서비스 제한, Spring multipart 설정, DTO 응답 필드를 함께 10MB로 상향 요청.",
                issueGuideline = "세 곳을 일관되게 수정해야 함. FileService.upload()의 하드코딩된 5MB 체크를 10MB로 변경하고, application.yml에 spring.servlet.multipart.max-file-size=10MB·max-request-size=20MB를 추가. 클라이언트가 업로드 파일 크기를 확인할 수 있도록 FileUploadRes DTO에 sizeBytes(Long) 필드도 추가할 것.",
                diff = """
                    diff --git a/src/main/resources/application.yml b/src/main/resources/application.yml
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/resources/application.yml
                    +++ b/src/main/resources/application.yml
                    @@ -8,4 +8,7 @@ spring:
                       datasource:
                         url: jdbc:mysql://localhost:3306/omos
                    +  servlet:
                    +    multipart:
                    +      max-file-size: 10MB
                    +      max-request-size: 20MB
                    diff --git a/src/main/kotlin/com/example/service/FileService.kt b/src/main/kotlin/com/example/service/FileService.kt
                    index b2c3d4e..f5g6h7i 100644
                    --- a/src/main/kotlin/com/example/service/FileService.kt
                    +++ b/src/main/kotlin/com/example/service/FileService.kt
                    @@ -15,6 +15,6 @@ class FileService(private val s3Client: S3Client) {
                         fun upload(file: MultipartFile): String {
                    -        if (file.size > 5 * 1024 * 1024) throw FileSizeLimitExceededException()
                    +        if (file.size > 10 * 1024 * 1024) throw FileSizeLimitExceededException()
                             val key = UUID.randomUUID().toString() + "-" + file.originalFilename
                             s3Client.putObject(key, file.bytes)
                             return "https://cdn.example.com/${'$'}key"
                         }
                    diff --git a/src/main/kotlin/com/example/dto/FileUploadRes.kt b/src/main/kotlin/com/example/dto/FileUploadRes.kt
                    --- a/src/main/kotlin/com/example/dto/FileUploadRes.kt
                    +++ b/src/main/kotlin/com/example/dto/FileUploadRes.kt
                    @@ -1,4 +1,5 @@
                     data class FileUploadRes(
                         val url: String,
                    -    val fileName: String
                    +    val fileName: String,
                    +    val sizeBytes: Long
                     )
                """.trimIndent()
            ),

            // ────────── 샘플 13~20: 엣지케이스 ──────────

            // 샘플 13: 테스트 파일만 추가 (프로덕션 코드 변경 없음)
            EvalSample(
                description = "테스트 파일만 추가 (프로덕션 변경 없는 케이스)",
                issueTitle = "UserService 단위 테스트 누락",
                issueContent = "getUserProfile 메서드에 대한 단위 테스트가 없음. 정상 조회 케이스와 미존재 사용자 케이스에 대한 테스트 클래스 추가 필요.",
                issueGuideline = "MockK로 UserRepository를 mock하고 UserServiceTest 클래스를 신규 작성. 정상 조회(존재하는 userId → UserProfileRes 반환 검증)와 예외 케이스(존재하지 않는 userId → UserNotFoundException 발생 검증) 두 시나리오를 @Nested 클래스 구조로 작성할 것.",
                diff = """
                    diff --git a/src/test/kotlin/com/example/service/UserServiceTest.kt b/src/test/kotlin/com/example/service/UserServiceTest.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/test/kotlin/com/example/service/UserServiceTest.kt
                    @@ -0,0 +1,38 @@
                    +package com.example.service
                    +
                    +import io.mockk.every
                    +import io.mockk.mockk
                    +import org.junit.jupiter.api.Nested
                    +import org.junit.jupiter.api.Test
                    +import org.junit.jupiter.api.assertThrows
                    +
                    +class UserServiceTest {
                    +
                    +    private val userRepository: UserRepository = mockk()
                    +    private val userService = UserService(userRepository)
                    +
                    +    @Nested
                    +    inner class GetUserProfileTest {
                    +
                    +        @Test
                    +        fun `존재하는 사용자 ID로 조회하면 프로필을 반환한다`() {
                    +            val user = User(name = "김민지", email = "test@example.com", id = 1L)
                    +            every { userRepository.findById(1L) } returns Optional.of(user)
                    +
                    +            val result = userService.getUserProfile(1L)
                    +
                    +            assert(result.name == "김민지")
                    +            assert(result.email == "test@example.com")
                    +        }
                    +
                    +        @Test
                    +        fun `존재하지 않는 ID로 조회하면 UserNotFoundException을 던진다`() {
                    +            every { userRepository.findById(any()) } returns Optional.empty()
                    +
                    +            assertThrows<UserNotFoundException> {
                    +                userService.getUserProfile(999L)
                    +            }
                    +        }
                    +    }
                    +}
                """.trimIndent()
            ),

            // 샘플 14: 빌드 파일만 변경 (dependency 추가)
            EvalSample(
                description = "모니터링 의존성 추가 (build.gradle.kts만 변경)",
                issueTitle = "Prometheus/Grafana 연동을 위한 메트릭 수집 설정 필요",
                issueContent = "운영 모니터링 대시보드 구축을 위해 Spring Actuator와 Micrometer Prometheus 의존성 추가 필요. build.gradle.kts에 의존성만 추가하면 됨.",
                issueGuideline = "build.gradle.kts의 dependencies 블록에 spring-boot-starter-actuator와 micrometer-registry-prometheus를 추가할 것. Prometheus 스크래핑 엔드포인트 노출을 위해 application.yml에 management.endpoints.web.exposure.include=prometheus 설정도 함께 추가 필요.",
                diff = """
                    diff --git a/build.gradle.kts b/build.gradle.kts
                    index a1b2c3d..e4f5g6h 100644
                    --- a/build.gradle.kts
                    +++ b/build.gradle.kts
                    @@ -22,6 +22,9 @@ dependencies {
                         implementation("org.springframework.boot:spring-boot-starter-data-jpa")
                         implementation("org.springframework.boot:spring-boot-starter-security")
                    +    implementation("org.springframework.boot:spring-boot-starter-actuator")
                    +    implementation("io.micrometer:micrometer-registry-prometheus")
                    +    runtimeOnly("io.micrometer:micrometer-core")
                         runtimeOnly("com.mysql:mysql-connector-j")
                         testImplementation("org.springframework.boot:spring-boot-starter-test")
                     }
                """.trimIndent()
            ),

            // 샘플 15: 기능 삭제 위주 (deprecated API 제거)
            EvalSample(
                description = "Deprecated 레거시 API 엔드포인트 제거 (삭제 위주 diff)",
                issueTitle = "v2 전환 완료 후 레거시 /legacy 엔드포인트 제거",
                issueContent = "v2 API 전환이 완료되었고 /api/users/legacy/* 엔드포인트를 사용하는 클라이언트가 없음을 확인. @Deprecated 엔드포인트 및 관련 서비스 메서드 제거 요청.",
                issueGuideline = "UserController의 getLegacyProfile(), getLegacyAvatar() 엔드포인트와 UserService의 getLegacyProfile(), getLegacyAvatarUrl() 메서드를 제거하면 됨. 관련 LegacyUserProfileRes DTO 클래스도 사용처가 없으면 함께 삭제하고, 해당 엔드포인트를 사용하는 클라이언트가 없음을 최종 확인 후 진행할 것.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/controller/UserController.kt b/src/main/kotlin/com/example/controller/UserController.kt
                    index b2c3d4e..f5g6h7i 100644
                    --- a/src/main/kotlin/com/example/controller/UserController.kt
                    +++ b/src/main/kotlin/com/example/controller/UserController.kt
                    @@ -18,18 +18,4 @@ class UserController(private val userService: UserService) {
                         }
                    -
                    -    @Deprecated("use /api/v2/users/{id}/profile instead")
                    -    @GetMapping("/legacy/{id}")
                    -    fun getLegacyProfile(@PathVariable id: Long): ResponseEntity<LegacyUserProfileRes> {
                    -        return ResponseEntity.ok(userService.getLegacyProfile(id))
                    -    }
                    -
                    -    @GetMapping("/legacy/{id}/avatar")
                    -    fun getLegacyAvatar(@PathVariable id: Long): ResponseEntity<String> {
                    -        return ResponseEntity.ok(userService.getLegacyAvatarUrl(id))
                    -    }
                    diff --git a/src/main/kotlin/com/example/service/UserService.kt b/src/main/kotlin/com/example/service/UserService.kt
                    @@ -35,12 +35,4 @@ class UserService(private val userRepository: UserRepository) {
                    -    @Deprecated("use getUserProfile instead")
                    -    fun getLegacyProfile(id: Long): LegacyUserProfileRes {
                    -        val user = userRepository.findById(id).orElseThrow { UserNotFoundException(id) }
                    -        return LegacyUserProfileRes(user.name, user.email, user.createdAt.toString())
                    -    }
                    -
                    -    fun getLegacyAvatarUrl(id: Long): String {
                    -        val user = userRepository.findById(id).orElseThrow { UserNotFoundException(id) }
                    -        return user.profileImageUrl ?: ""
                    -    }
                     }
                """.trimIndent()
            ),

            // 샘플 16: 공백/포맷팅만 변경 (내용 변경 없는 trivial diff — 가장 까다로운 케이스)
            EvalSample(
                description = "코드 포맷팅/공백만 변경 (내용 변경 없는 trivial diff)",
                issueTitle = "PostService ktlint 포맷팅 위반 수정",
                issueContent = "CI ktlint 검사에서 PostService 파일에 공백 및 포맷팅 위반이 감지됨. 로직 변경 없이 Kotlin 코딩 컨벤션에 맞게 포맷팅만 정리.",
                issueGuideline = "PostService의 함수 선언부 콜론 앞뒤 공백, 중괄호 앞 공백, 람다 중괄호 내부 공백을 Kotlin 코딩 컨벤션에 맞게 수정할 것. 로직 변경 없이 포맷팅만 정리하며, ./gradlew ktlintFormat 명령으로 자동 교정 후 변경 내역을 확인하는 것을 권장.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/PostService.kt b/src/main/kotlin/com/example/service/PostService.kt
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/kotlin/com/example/service/PostService.kt
                    +++ b/src/main/kotlin/com/example/service/PostService.kt
                    @@ -8,14 +8,14 @@ class PostService(private val postRepository: PostRepository) {
                    -    fun getPost(postId:Long):PostRes{
                    -        val post=postRepository.findById(postId)
                    -            .orElseThrow{PostNotFoundException(postId)}
                    -        return PostRes.from(post)
                    -    }
                    -    fun createPost(userId:Long,req:CreatePostReq):PostRes{
                    -        val author=userRepository.findById(userId).orElseThrow{UserNotFoundException(userId)}
                    -        return PostRes.from(postRepository.save(Post(author,req.title,req.content)))
                    -    }
                    +    fun getPost(postId: Long): PostRes {
                    +        val post = postRepository.findById(postId)
                    +            .orElseThrow { PostNotFoundException(postId) }
                    +        return PostRes.from(post)
                    +    }
                    +    fun createPost(userId: Long, req: CreatePostReq): PostRes {
                    +        val author = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
                    +        return PostRes.from(postRepository.save(Post(author, req.title, req.content)))
                    +    }
                """.trimIndent()
            ),

            // 샘플 17: DB 마이그레이션 SQL만 변경
            EvalSample(
                description = "DB 마이그레이션 파일 추가 — 컬럼 추가 및 인덱스 생성",
                issueTitle = "User 테이블에 프로필 이미지 URL 컬럼 추가",
                issueContent = "프로필 이미지 기능 구현을 위해 users 테이블에 profile_image_url, profile_image_updated_at 컬럼이 필요함. Flyway 마이그레이션 스크립트로 컬럼 추가 및 조회 성능을 위한 인덱스 생성.",
                issueGuideline = "Flyway 마이그레이션 파일(V5__...)을 신규 작성해 users 테이블에 profile_image_url(VARCHAR 500, NULL 허용)·profile_image_updated_at(DATETIME, NULL 허용) 컬럼을 ALTER TABLE로 추가할 것. profile_image_updated_at 기준 조회 성능을 위해 CREATE INDEX도 같은 파일에 포함. 기존 User entity에 해당 필드도 함께 추가 필요.",
                diff = """
                    diff --git a/src/main/resources/db/migration/V5__add_profile_image_to_user.sql b/src/main/resources/db/migration/V5__add_profile_image_to_user.sql
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/resources/db/migration/V5__add_profile_image_to_user.sql
                    @@ -0,0 +1,5 @@
                    +ALTER TABLE users
                    +    ADD COLUMN profile_image_url VARCHAR(500) NULL,
                    +    ADD COLUMN profile_image_updated_at DATETIME NULL;
                    +
                    +CREATE INDEX idx_users_profile_image_updated_at ON users (profile_image_updated_at);
                """.trimIndent()
            ),

            // 샘플 18: 로깅 추가 (여러 파일 cross-cutting)
            EvalSample(
                description = "주요 서비스 메서드에 구조화 로깅 추가 (여러 파일 cross-cutting)",
                issueTitle = "로그인·사용자 삭제 이벤트 로그 부재로 운영 중 디버깅 어려움",
                issueContent = "운영 환경에서 비정상 로그인 시도나 사용자 삭제 이슈 발생 시 추적이 불가능함. AuthService와 UserService 주요 메서드에 구조화 로그(SLF4J) 추가 필요.",
                issueGuideline = "AuthService와 UserService에 LoggerFactory.getLogger(javaClass)로 SLF4J 로거를 추가하고, 주요 이벤트(로그인 시도·성공, 사용자 삭제 요청·완료)에 구조화 로그를 남길 것. 로그 레벨은 시도/요청은 info, 삭제처럼 위험도 높은 작업은 warn으로 구분하고, 비밀번호·토큰 등 민감 정보가 로그에 포함되지 않도록 주의.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/AuthService.kt b/src/main/kotlin/com/example/service/AuthService.kt
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/kotlin/com/example/service/AuthService.kt
                    +++ b/src/main/kotlin/com/example/service/AuthService.kt
                    @@ -1,5 +1,8 @@
                    +import org.slf4j.LoggerFactory
                    +
                     @Service
                     class AuthService(private val jwtProvider: JwtProvider) {
                    +    private val log = LoggerFactory.getLogger(javaClass)
                    +
                         fun login(req: LoginReq): TokenRes {
                    +        log.info("login attempt: email={}", req.email)
                             val token = jwtProvider.generate(req.email)
                    +        log.info("login success: email={}", req.email)
                             return TokenRes(token)
                         }
                     }
                    diff --git a/src/main/kotlin/com/example/service/UserService.kt b/src/main/kotlin/com/example/service/UserService.kt
                    index b2c3d4e..f5g6h7i 100644
                    --- a/src/main/kotlin/com/example/service/UserService.kt
                    +++ b/src/main/kotlin/com/example/service/UserService.kt
                    @@ -1,5 +1,7 @@
                    +import org.slf4j.LoggerFactory
                    +
                     @Service
                     class UserService(private val userRepository: UserRepository) {
                    +    private val log = LoggerFactory.getLogger(javaClass)
                    +
                         fun deleteUser(userId: Long) {
                    +        log.warn("user deletion requested: userId={}", userId)
                             val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
                             userRepository.delete(user)
                    +        log.info("user deleted: userId={}", userId)
                         }
                     }
                """.trimIndent()
            ),

            // 샘플 19: 커스텀 예외 체계 도입 + 기존 코드 연결
            EvalSample(
                description = "커스텀 예외 클래스 추가 및 RuntimeException 교체",
                issueTitle = "RuntimeException 직접 사용으로 에러 핸들러에서 도메인 예외 구분 불가",
                issueContent = "현재 서비스 전반에서 RuntimeException을 직접 throw하고 있어 GlobalExceptionHandler에서 도메인별 에러를 분기할 수 없음. UserNotFoundException, PostNotFoundException, ForbiddenException 등 커스텀 예외 클래스 도입 요청.",
                issueGuideline = "exception 패키지에 UserNotFoundException(id: Long), PostNotFoundException(id: Long), ForbiddenException 커스텀 예외 클래스를 신규 생성할 것. 이후 서비스 코드에서 RuntimeException을 직접 throw하는 부분을 해당 도메인 예외로 교체하고, GlobalExceptionHandler에 각 예외 타입별 @ExceptionHandler 메서드도 추가 필요.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/exception/CustomExceptions.kt b/src/main/kotlin/com/example/exception/CustomExceptions.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/exception/CustomExceptions.kt
                    @@ -0,0 +1,9 @@
                    +package com.example.exception
                    +
                    +class UserNotFoundException(id: Long) :
                    +    RuntimeException("존재하지 않는 사용자입니다: id=${'$'}id")
                    +
                    +class PostNotFoundException(id: Long) :
                    +    RuntimeException("존재하지 않는 게시글입니다: id=${'$'}id")
                    +
                    +class ForbiddenException : RuntimeException("접근 권한이 없습니다")
                    diff --git a/src/main/kotlin/com/example/service/PostService.kt b/src/main/kotlin/com/example/service/PostService.kt
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/kotlin/com/example/service/PostService.kt
                    +++ b/src/main/kotlin/com/example/service/PostService.kt
                    @@ -12,8 +12,6 @@ class PostService(private val postRepository: PostRepository) {
                         fun deletePost(userId: Long, postId: Long) {
                             val post = postRepository.findById(postId)
                    -            .orElseThrow { RuntimeException("post not found") }
                    -        if (post.author.id != userId) throw RuntimeException("forbidden")
                    +            .orElseThrow { PostNotFoundException(postId) }
                    +        if (post.author.id != userId) throw ForbiddenException()
                             postRepository.delete(post)
                         }
                     }
                """.trimIndent()
            ),

            // 샘플 20: 인터페이스 분리 + SMTP 구현체 추가
            EvalSample(
                description = "메일 발송 인터페이스 분리 및 SMTP 구현체 추가",
                issueTitle = "NotificationService가 MailService 구현체에 직접 의존해 테스트 및 교체 어려움",
                issueContent = "NotificationService가 MailService 구현체를 직접 주입받아 단위 테스트 시 mock 처리가 번거롭고 구현체 교체 시 서비스 코드를 수정해야 함. MailSender 인터페이스를 분리하고 SmtpMailSender 구현체를 추가하는 방식으로 의존성 역전 적용 요청.",
                issueGuideline = "MailSender 인터페이스를 신규 생성하고 send(to, subject, body) 메서드를 정의할 것. SmtpMailSender가 MailSender를 구현하도록 작성하고 JavaMailSender를 주입받아 MIME 메시지를 생성·발송. NotificationService의 생성자 파라미터를 MailService → MailSender로 교체하고 sendMail() 호출을 send()로 변경. JavaMailSender 스프링 빈 설정이 존재하는지 확인 필요.",
                diff = """
                    diff --git a/src/main/kotlin/com/example/service/MailSender.kt b/src/main/kotlin/com/example/service/MailSender.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/service/MailSender.kt
                    @@ -0,0 +1,5 @@
                    +package com.example.service
                    +
                    +interface MailSender {
                    +    fun send(to: String, subject: String, body: String)
                    +}
                    diff --git a/src/main/kotlin/com/example/service/SmtpMailSender.kt b/src/main/kotlin/com/example/service/SmtpMailSender.kt
                    new file mode 100644
                    --- /dev/null
                    +++ b/src/main/kotlin/com/example/service/SmtpMailSender.kt
                    @@ -0,0 +1,18 @@
                    +package com.example.service
                    +
                    +import org.springframework.mail.javamail.JavaMailSender
                    +import org.springframework.mail.javamail.MimeMessageHelper
                    +import org.springframework.stereotype.Component
                    +
                    +@Component
                    +class SmtpMailSender(private val javaMailSender: JavaMailSender) : MailSender {
                    +
                    +    override fun send(to: String, subject: String, body: String) {
                    +        val message = javaMailSender.createMimeMessage()
                    +        MimeMessageHelper(message, false, "UTF-8").apply {
                    +            setTo(to)
                    +            setSubject(subject)
                    +            setText(body, true)
                    +        }
                    +        javaMailSender.send(message)
                    +    }
                    +}
                    diff --git a/src/main/kotlin/com/example/service/NotificationService.kt b/src/main/kotlin/com/example/service/NotificationService.kt
                    index a1b2c3d..e4f5g6h 100644
                    --- a/src/main/kotlin/com/example/service/NotificationService.kt
                    +++ b/src/main/kotlin/com/example/service/NotificationService.kt
                    @@ -1,9 +1,9 @@
                     @Service
                     class NotificationService(
                    -    private val mailService: MailService
                    +    private val mailSender: MailSender
                     ) {
                         fun notifyUser(userId: Long, message: String) {
                             val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
                    -        mailService.sendMail(user.email, "알림", message)
                    +        mailSender.send(user.email, "알림", message)
                         }
                     }
                """.trimIndent()
            )
        )

        // ────────── 카테고리별 서브셋 (집중 평가용) ──────────

        /** 프롬프트가 무너지기 쉬운 엣지케이스 모음 (샘플 13~20) */
        val EDGE_CASE_SAMPLES: List<EvalSample> = EVAL_SAMPLES.drop(12)

        /** 대형 diff 모음 (샘플 8~9) */
        val LARGE_DIFF_SAMPLES: List<EvalSample> = EVAL_SAMPLES.subList(7, 9)

        /**
         * 각 카테고리 대표 7종 — 프롬프트 수정 후 빠르게 회귀 확인할 때 사용.
         * v7부터 guideline이 있는 샘플(1, 2, 8)을 포함해 가이드라인 효과를 함께 측정.
         * 샘플 1(기본 버그 + guideline), 2(신규 기능 + guideline), 6(극소 diff),
         * 8(대형 diff + guideline), 10(애매), 14(빌드 파일), 16(공백만 변경)
         */
        val QUICK_SAMPLES: List<EvalSample> = listOf(
            EVAL_SAMPLES[0].copy(  // 샘플 1: null 안전성 버그 수정 (guideline 포함)
                issueGuideline = "getUserProfile()에서 User.name·User.email이 nullable인데 직접 접근해 NPE 발생. " +
                    "엘비스 연산자(?:)로 기본값 처리하고, orElseThrow 예외를 RuntimeException → UserNotFoundException으로 교체할 것. " +
                    "UserProfileRes 생성자 호출부 파라미터 타입 변경 여부도 확인 필요."
            ),
            EVAL_SAMPLES[1].copy(  // 샘플 2: 페이지네이션 추가 (guideline 포함)
                issueGuideline = "PostController.getPosts()가 전체 목록을 반환해 데이터 증가 시 OOM 위험. " +
                    "컨트롤러 파라미터에 @RequestParam page/size를 추가하고 PageRequest.of()로 Pageable 생성. " +
                    "PostService·PostRepository 반환 타입을 List → Page로 변경하며, " +
                    "응답 타입이 List<PostRes> → Page<PostRes>로 바뀌므로 API 클라이언트 측 totalElements 처리 필요."
            ),
            EVAL_SAMPLES[5],   // 샘플 6: 에러 메시지 한 줄 수정
            EVAL_SAMPLES[7].copy(  // 샘플 8: 알림 서비스 신규 구현 (guideline 포함)
                issueGuideline = "Notification entity에 user(ManyToOne LAZY), type(EnumType.STRING), isRead(기본 false) 필드 필요. " +
                    "NotificationService에 getUnread(읽기 전용 트랜잭션), markAllAsRead(벌크 업데이트), markAsRead(소유권 체크 후 단건 처리) 메서드 구현. " +
                    "NotificationController는 @AuthenticationPrincipal로 userId 추출해 서비스에 위임."
            ),
            EVAL_SAMPLES[9],   // 샘플 10: 정렬 기준 변경
            EVAL_SAMPLES[13],  // 샘플 14: 빌드 파일만 변경
            EVAL_SAMPLES[15],  // 샘플 16: 공백/포맷팅만 변경
        )
    }
}
