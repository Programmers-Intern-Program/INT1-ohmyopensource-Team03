package com.back.omos.domain.prdraft.ai

import com.back.omos.domain.prdraft.service.PrDraftPromptBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.File
import org.springframework.beans.factory.annotation.Qualifier

/**
 * 프롬프트 버전별 성능을 비교하기 위한 평가 테스트입니다.
 *
 * <p>
 * 고정된 diff 샘플 5종으로 AI를 호출하고, 결과를 Langfuse에 자동 기록합니다.
 * 프롬프트를 수정할 때마다 이 테스트를 실행하여 버전 간 score/latency를 비교하세요.
 *
 * <p><b>실행 방법:</b><br>
 * {@code @Disabled}를 제거하거나 IntelliJ에서 해당 테스트를 직접 실행합니다.
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
@Disabled("수동 프롬프트 평가 전용 — 실행 시 @Disabled 제거")
@SpringBootTest
@ActiveProfiles("test")
class PrDraftPromptEvalTest {

    @Autowired
    private lateinit var aiClient: SpringAiClient

    @Autowired
    private lateinit var promptBuilder: PrDraftPromptBuilder

    /**
     * 5종의 고정 diff 샘플로 프롬프트를 평가합니다.
     *
     * <p>
     * 각 샘플은 실제 오픈소스 기여 시나리오를 반영합니다.
     * 결과는 Langfuse에 자동 기록되며, score·latency·토큰 수를 확인할 수 있습니다.
     */
    @Test
    fun `고정 샘플 5종으로 프롬프트 평가`() {
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
        // judge 호출은 GLM 응답까지 최대 30초 소요될 수 있고 샘플 5개가 동시 진행되므로 60초 대기
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
        data class EvalSample(val description: String, val diff: String)

        val EVAL_SAMPLES = listOf(

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
            )
        )
    }
}
