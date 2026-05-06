package com.back.omos.domain.prdraft.ai

import com.back.omos.domain.prdraft.github.GitHubPrRes
import com.back.omos.domain.prdraft.service.PrDraftPromptBuilder
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
                    prs = emptyList()
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
                    prs = emptyList()
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
                    prs = emptyList()
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
                    prs = emptyList()
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
        try {
            val prompt = promptBuilder.build(
                diffContent = EVAL_SAMPLES[0].diff,
                contributing = SAMPLE_CONTRIBUTING,
                prs = emptyList()
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
                    prs = emptyList()
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
        try {
            val prompt = promptBuilder.build(
                diffContent = EVAL_SAMPLES[1].diff,
                contributing = null,
                prs = SAMPLE_PRS
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

        data class EvalSample(val description: String, val diff: String)

        val EVAL_SAMPLES = listOf(

            // ────────── 샘플 1~5: 기본 케이스 (v1부터의 고정 기준선, 수정 금지) ──────────

            // 샘플 1: 단순 버그 수정 (null 안전성)
            EvalSample(
                description = "null 안전성 버그 수정",
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
         * 샘플 1(기본 버그), 2(신규 기능), 6(극소 diff), 8(대형 diff),
         * 10(애매), 14(빌드 파일), 16(공백만 변경)
         */
        val QUICK_SAMPLES: List<EvalSample> = listOf(
            EVAL_SAMPLES[0],   // 샘플 1: null 안전성 버그 수정
            EVAL_SAMPLES[1],   // 샘플 2: 페이지네이션 추가
            EVAL_SAMPLES[5],   // 샘플 6: 에러 메시지 한 줄 수정
            EVAL_SAMPLES[7],   // 샘플 8: 알림 서비스 신규 구현
            EVAL_SAMPLES[9],   // 샘플 10: 정렬 기준 변경
            EVAL_SAMPLES[13],  // 샘플 14: 빌드 파일만 변경
            EVAL_SAMPLES[15],  // 샘플 16: 공백/포맷팅만 변경
        )
    }
}
