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

/**
 * 번역 프롬프트 버전별 성능을 비교하기 위한 평가 테스트입니다.
 *
 * <p>
 * 고정된 한국어 PR 샘플 14종으로 번역을 호출하고, 결과를 Langfuse에 자동 기록합니다.
 * 번역 프롬프트를 수정할 때마다 이 테스트를 실행하여 버전 간 score/latency를 비교하세요.
 *
 * <p><b>샘플 분류 (총 14종):</b><br>
 * - 샘플 1~5: 기본 케이스 (feat/fix/refactor/chore/docs 타입)<br>
 * - 샘플 6~8: 기술 용어 포함 (클래스명·어노테이션·메서드 시그니처)<br>
 * - 샘플 9~10: 복잡한 마크다운 형식 (헤더·불릿·<!-- --> 주석 placeholder)<br>
 * - 샘플 11~14: 엣지케이스 (짧은 PR / (작성 필요) 포함 / 한영 혼재 / 긴 PR)<br>
 *
 * <p><b>채점 기준:</b><br>
 * - 자연스러움 (0~4점): 영어 표현이 자연스러운가<br>
 * - 기술 용어 보존 (0~3점): 클래스명·메서드명·어노테이션을 번역하지 않았는가<br>
 * - 형식 보존 (0~3점): 커밋 컨벤션 prefix, ## 헤더, 불릿, <!-- --> 주석을 유지했는가<br>
 *
 * <p><b>실행 방법:</b><br>
 * IntelliJ에서 실행하거나 {@code @Disabled}를 제거 후 실행합니다.
 * 실행 후 localhost:3001 → Traces에서 결과를 확인합니다.
 *
 * <p><b>주의:</b><br>
 * 실제 GLM API와 Langfuse를 호출하므로 dev 환경에서만 실행해야 합니다.
 * 번역 프롬프트 버전을 바꾸면 {@link PrDraftPromptBuilder#PROMPT_VERSION_TRANSLATE}과
 * {@link SpringAiClient}의 {@code GENERATION_TRANSLATE} 상수도 함께 올려야 합니다.
 *
 * @author 5h6vm
 * @since 2026-05-08
 */
@Disabled("수동 번역 프롬프트 평가 전용 — 실행 시 @Disabled 제거")
@SpringBootTest
@ActiveProfiles("test")
class TranslatePromptEvalTest {

    @Autowired
    private lateinit var aiClient: SpringAiClient

    /**
     * 카테고리 대표 5종으로 빠르게 번역 품질을 평가합니다.
     * 번역 프롬프트 수정할 때마다 사용. 총 소요 시간: 약 2~3분
     */
    @Test
    fun `빠른 평가 — 카테고리 대표 5종`() {
        QUICK_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 빠른 평가 ${index + 1}: ${sample.description} =====")

            try {
                val result = aiClient.translate(sample.title, sample.body)
                println("원문 제목: ${sample.title}")
                println("번역 제목: ${result.title}")
                println("번역 본문 미리보기: ${result.body.take(150)}...")
            } catch (e: Exception) {
                println("빠른 평가 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            if (index < QUICK_SAMPLES.lastIndex) Thread.sleep(10_000)
        }

        Thread.sleep(60_000)
    }

    /**
     * 14종의 고정 샘플로 번역 프롬프트를 평가합니다.
     * 총 소요 시간: 약 5분 (샘플 간 10초 대기 + judge 60초)
     */
    @Test
    fun `고정 샘플 전체로 번역 평가`() {
        TRANSLATE_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 샘플 ${index + 1}: ${sample.description} =====")

            try {
                val result = aiClient.translate(sample.title, sample.body)
                println("원문 제목: ${sample.title}")
                println("번역 제목: ${result.title}")
                println("번역 본문 미리보기: ${result.body.take(150)}...")
            } catch (e: Exception) {
                println("샘플 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            if (index < TRANSLATE_SAMPLES.lastIndex) Thread.sleep(10_000)
        }

        Thread.sleep(120_000)
    }

    /**
     * 기술 용어가 많이 포함된 샘플로 집중 평가합니다.
     * 클래스명·어노테이션·메서드 시그니처를 번역하지 않고 그대로 유지하는지 확인합니다.
     */
    @Test
    fun `기술 용어 보존 집중 평가`() {
        TECH_TERM_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 기술용어 ${index + 1}: ${sample.description} =====")

            try {
                val result = aiClient.translate(sample.title, sample.body)
                println("원문 제목: ${sample.title}")
                println("번역 제목: ${result.title}")
                println("번역 본문 미리보기: ${result.body.take(200)}...")
            } catch (e: Exception) {
                println("기술용어 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            if (index < TECH_TERM_SAMPLES.lastIndex) Thread.sleep(10_000)
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

        data class TranslateSample(
            val description: String,
            val title: String,
            val body: String
        )

        val TRANSLATE_SAMPLES = listOf(

            // ────────── 샘플 1~5: 기본 케이스 ──────────

            // 샘플 1: feat — 신규 기능
            TranslateSample(
                description = "feat: 페이지네이션 추가 (기본 기능 추가)",
                title = "feat: 게시글 목록 조회 API에 페이지네이션 추가",
                body = """
                    ## 변경 이유
                    게시글 수 증가로 응답 시간이 느려지고 있어 페이지 단위 조회로 개선합니다.

                    ## 수정 내용
                    - `PostController.getPosts()`에 `page`, `size` 파라미터 추가
                    - 반환 타입 `List<PostRes>` → `Page<PostRes>` 변경
                    - `PostService.getPosts(pageable: Pageable)` 시그니처 변경

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 2: fix — 버그 수정
            TranslateSample(
                description = "fix: NPE 버그 수정 (기본 버그 수정)",
                title = "fix: getUserProfile() null 사용자 조회 시 NPE 수정",
                body = """
                    ## 변경 이유
                    `name` 또는 `email`이 null인 사용자 조회 시 `UserProfileRes` 생성 단계에서 NPE가 발생합니다.

                    ## 수정 내용
                    - `UserService.getUserProfile()`에서 null 안전 처리 추가
                    - 예외 타입 `RuntimeException` → `UserNotFoundException`으로 교체
                    - `UserProfileRes` 생성 시 nullable 필드에 기본값 적용

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 3: refactor — 리팩토링
            TranslateSample(
                description = "refactor: 공통 검증 로직 추출 (리팩토링)",
                title = "refactor: OrderService 중복 주문 조회·권한 검증 로직 추출",
                body = """
                    ## 변경 이유
                    `cancelOrder`와 `getOrderDetail`에 동일한 주문 조회 및 소유자 확인 로직이 중복되어 있어 유지보수가 어렵습니다.

                    ## 수정 내용
                    - `getOrderOrThrow(userId: Long, orderId: Long): Order` private 메서드 추출
                    - `cancelOrder`, `getOrderDetail` 두 메서드에서 공통 로직 제거

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 4: chore — 빌드/설정
            TranslateSample(
                description = "chore: 의존성 추가 (빌드 파일 변경)",
                title = "chore: Prometheus 메트릭 수집을 위한 의존성 추가",
                body = """
                    ## 변경 이유
                    운영 모니터링 대시보드 구축을 위해 Spring Actuator와 Micrometer Prometheus 의존성이 필요합니다.

                    ## 수정 내용
                    - `build.gradle.kts`에 `spring-boot-starter-actuator` 추가
                    - `micrometer-registry-prometheus` 추가

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 5: docs — 문서
            TranslateSample(
                description = "docs: README 업데이트 (문서 변경)",
                title = "docs: 로컬 개발 환경 설정 방법 README 업데이트",
                body = """
                    ## 변경 이유
                    새로운 팀원이 로컬 환경 설정 시 `.env` 파일 구성 방법이 문서에 없어 혼란이 있었습니다.

                    ## 수정 내용
                    - `.env.example` 항목별 설명 추가
                    - Docker Compose 실행 순서 명시
                    - 자주 묻는 오류(JDK 버전 불일치) 해결 방법 추가

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // ────────── 샘플 6~8: 기술 용어 포함 ──────────

            // 샘플 6: 어노테이션 집중
            TranslateSample(
                description = "@Cacheable·@CacheEvict 어노테이션 포함 (기술 용어 집중)",
                title = "perf: RepoService 레포지토리 조회에 @Cacheable 적용",
                body = """
                    ## 변경 이유
                    대시보드 렌더링 시 동일 레포지토리 정보를 반복 조회하여 DB 부하가 발생합니다.

                    ## 수정 내용
                    - `RepoService.getRepoInfo()`에 `@Cacheable(value = ["repoInfo"], key = "#repoId")` 추가
                    - 캐시 무효화용 `invalidateCache()` 메서드에 `@CacheEvict` 적용
                    - `@EnableCaching` 클래스 레벨 어노테이션 추가

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 7: JPA 기술 용어
            TranslateSample(
                description = "JPA FetchType·@EntityGraph 변경 (기술 용어 집중)",
                title = "perf: IssueRepository N+1 쿼리 개선 — @EntityGraph 적용",
                body = """
                    ## 변경 이유
                    이슈 목록 조회 시 `participants` 관계를 LAZY로 로드하여 N+1 쿼리가 발생합니다.
                    100건 기준 120개 쿼리 → 2개로 감소합니다.

                    ## 수정 내용
                    - `IssueRepository.findAll()`에 `@EntityGraph(attributePaths = ["participants"])` 적용
                    - `participants` 페치 전략 `FetchType.LAZY` → `FetchType.EAGER` 변경

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 8: 트랜잭션 전파 + 메서드 시그니처
            TranslateSample(
                description = "트랜잭션 전파 전략 변경 (메서드 시그니처 기술 용어)",
                title = "fix: PaymentService 감사 로그 트랜잭션 전파 전략 REQUIRES_NEW로 변경",
                body = """
                    ## 변경 이유
                    결제 트랜잭션 롤백 시 동일 트랜잭션의 감사 로그도 삭제되어 감사 추적이 불가능합니다.
                    감사 로그는 결제 성공/실패와 무관하게 항상 커밋되어야 합니다.

                    ## 수정 내용
                    - `recordAuditLog(paymentId: Long, action: String)`의 `@Transactional` 전파 전략을
                      기본값(`REQUIRED`) → `Propagation.REQUIRES_NEW`으로 변경

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // ────────── 샘플 9~10: 복잡한 마크다운 형식 ──────────

            // 샘플 9: 3섹션 + 중첩 불릿
            TranslateSample(
                description = "3섹션 구조 + 중첩 불릿 (마크다운 형식 집중)",
                title = "feat: 알림 기능 구현 — 읽지 않은 알림 조회 및 읽음 처리",
                body = """
                    ## 변경 이유
                    사용자가 읽지 않은 알림을 확인하고 읽음 처리할 수 있는 기능이 없어 UX 개선이 필요합니다.

                    ## 수정 내용
                    - `Notification` entity 신규 추가 (`isRead: Boolean`, `type: NotificationType`, `payload: String`)
                    - `NotificationService` 구현:
                      - `getUnread(userId: Long): List<NotificationRes>`
                      - `markAllAsRead(userId: Long)`
                      - `markAsRead(userId: Long, notificationId: Long)`
                    - `NotificationController`에 엔드포인트 3개 추가:
                      - `GET /api/v1/notifications/unread`
                      - `PATCH /api/v1/notifications/read-all`
                      - `PATCH /api/v1/notifications/{id}/read`
                    - `@Transactional(readOnly = true)` / `@Transactional` 분리 적용

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 10: Jakarta 어노테이션 + @Valid
            TranslateSample(
                description = "Jakarta Validation 어노테이션 본문 포함 (마크다운 형식 집중)",
                title = "feat: 댓글 생성 API 입력값 검증 추가",
                body = """
                    ## 변경 이유
                    댓글 내용에 빈 문자열이나 500자 초과 텍스트가 저장 가능해 데이터 품질 문제가 있습니다.

                    ## 수정 내용
                    - `CreateCommentReq`에 `@field:NotBlank`, `@field:Size(min = 1, max = 500)` 추가
                    - `postId` 필드에 `@field:Positive` 추가
                    - `CommentController.createComment()`에 `@Valid` 어노테이션 추가

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // ────────── 샘플 11~14: 엣지케이스 ──────────

            // 샘플 11: 매우 짧은 PR
            TranslateSample(
                description = "매우 짧은 PR (한 줄 변경 설명)",
                title = "fix: USER_NOT_FOUND 에러 메시지 한국어로 변경",
                body = """
                    ## 변경 이유
                    영문 에러 메시지가 클라이언트에 그대로 노출됩니다.

                    ## 수정 내용
                    - `ErrorCode.USER_NOT_FOUND` 메시지 `"User not found"` → `"요청한 사용자를 찾을 수 없습니다"` 변경

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 12: (작성 필요) placeholder 포함
            TranslateSample(
                description = "(작성 필요) placeholder가 변경 이유에 포함된 PR",
                title = "refactor: 메일 발송 인터페이스 분리 및 SmtpMailSender 구현체 추가",
                body = """
                    ## 변경 이유
                    (작성 필요)

                    ## 수정 내용
                    - `MailSender` 인터페이스 신규 추가: `send(to: String, subject: String, body: String)`
                    - `SmtpMailSender : MailSender` 구현체 추가 — `JavaMailSender` 기반 SMTP 발송
                    - `NotificationService` 의존성 `MailService` → `MailSender`로 교체
                    - `sendMail()` → `send()` 메서드명 변경

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 13: 한국어·영어 기술 용어 혼재
            TranslateSample(
                description = "한국어·영어 기술 용어 혼재 PR",
                title = "feat: UserController에 프로필 이미지 업로드 엔드포인트 추가",
                body = """
                    ## 변경 이유
                    프로필 이미지 설정 기능이 없어 사용자 피드백이 있었습니다.

                    ## 수정 내용
                    - `POST /api/users/profile-image` 엔드포인트 추가 (`multipart/form-data`)
                    - `MultipartFile` → S3 업로드 후 URL을 `User.profileImageUrl`에 저장
                    - 파일 크기 5MB, 확장자 jpg/png/webp 제한 (`FileSizeLimitExceededException`)
                    - `UserService.updateProfileImage(userId: Long, file: MultipartFile): String` 추가

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            ),

            // 샘플 14: 긴 PR (### 하위 헤더 포함)
            TranslateSample(
                description = "긴 PR — 댓글 CRUD 전체 구현 (### 하위 헤더 포함)",
                title = "feat: 댓글 CRUD 기능 전체 구현 — entity, DTO, Repository, Service, Controller",
                body = """
                    ## 변경 이유
                    게시글에 댓글을 작성하고 조회할 수 있는 기능이 없어 커뮤니티 기능 확장이 어렵습니다.

                    ## 수정 내용
                    ### 신규 파일
                    - `Comment` entity: `post: Post`, `author: User`, `content: String`, `createdAt: Instant`
                    - `CreateCommentReq`: `content: String`, `postId: Long`
                    - `CommentRes`: `id`, `content`, `authorName`, `createdAt` — `from(c: Comment)` factory 메서드 포함
                    - `CommentRepository : JpaRepository<Comment, Long>`: `findAllByPostIdOrderByCreatedAtDesc()` 추가
                    - `CommentService`: `create(userId, req)`, `list(postId)` 구현
                    - `CommentController`: `POST /api/v1/comments`, `GET /api/v1/comments/post/{postId}`

                    ### 의존 관계
                    - `CommentService` → `CommentRepository`, `PostRepository`, `UserRepository`
                    - `@Transactional` (write) / `@Transactional(readOnly = true)` (read) 분리

                    ## 테스트 방법
                    <!-- 직접 작성 필요 -->
                """.trimIndent()
            )
        )

        // ────────── 카테고리별 서브셋 (집중 평가용) ──────────

        /** 기술 용어가 많이 포함된 샘플 (샘플 6~8) */
        val TECH_TERM_SAMPLES: List<TranslateSample> = TRANSLATE_SAMPLES.subList(5, 8)

        /**
         * 카테고리 대표 5종 — 번역 프롬프트 수정 후 빠르게 회귀 확인할 때 사용.
         * 샘플 1(feat 기본), 2(fix 버그), 6(@Cacheable 어노테이션), 9(마크다운 형식), 12((작성 필요))
         */
        val QUICK_SAMPLES: List<TranslateSample> = listOf(
            TRANSLATE_SAMPLES[0],   // 샘플 1: feat 페이지네이션
            TRANSLATE_SAMPLES[1],   // 샘플 2: fix NPE
            TRANSLATE_SAMPLES[5],   // 샘플 6: @Cacheable 어노테이션
            TRANSLATE_SAMPLES[8],   // 샘플 9: 3섹션 마크다운
            TRANSLATE_SAMPLES[11],  // 샘플 12: (작성 필요) placeholder
        )
    }
}
