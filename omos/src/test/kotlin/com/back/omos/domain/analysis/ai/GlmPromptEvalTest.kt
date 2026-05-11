package com.back.omos.domain.analysis.ai

import com.back.omos.domain.analysis.github.GitHubClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.File

/**
 * 프롬프트 버전별 성능을 비교하기 위한 평가 테스트입니다.
 *
 * 고정된 이슈 샘플 5종으로 selectFiles → analyze 파이프라인을 실행하고
 * 결과를 Langfuse에 자동 기록합니다.
 * 프롬프트를 수정할 때마다 이 테스트를 실행하여 버전 간 score/latency를 비교하세요.
 *
 * 실행 후 localhost:3001 → Traces에서 결과를 확인합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-05-06
 */
@Disabled("수동 프롬프트 평가 전용 — 실행 시 @Disabled 제거")
@SpringBootTest
@ActiveProfiles("test")
class GlmPromptEvalTest {

    @Autowired
    private lateinit var glmClient: GlmClient

    @Autowired
    @Qualifier("analysisGitHubClientImpl")
    private lateinit var gitHubClient: GitHubClient

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

        data class EvalSample(
            val description: String,
            val owner: String,
            val repo: String,
            val issueNumber: Int,
            val issueTitle: String,
            val issueBody: String?,
            val labels: List<String>
        )

        val EVAL_SAMPLES = listOf(
            EvalSample(
                description = "JabRef - setStyle CSS 교체 리팩토링",
                owner = "JabRef",
                repo = "jabref",
                issueNumber = 15686,
                issueTitle = "Replace setStyle(..) calls with CSS styleClass",
                issueBody = "There are about 35 calls to setStyle(...) in the codebase. This should be replaced by proper CSS styleClass usage.",
                labels = listOf("component: css", "component: ui", "good first issue")
            ),
            EvalSample(
                description = "JabRef - PDF 인덱싱 오류 로깅 개선",
                owner = "JabRef",
                repo = "jabref",
                issueNumber = 15680,
                issueTitle = "Log file name or citation key when PDF indexing fails",
                issueBody = "When an error occurs during PDF full-text indexing, the log does not include which file caused the error. The file name or citation key should be included in the error log.",
                labels = listOf("component: external-files", "good first issue")
            ),
            EvalSample(
                description = "Tadreeb - Dashboard Reset 버튼 버그",
                owner = "Tadreeb-LMS",
                repo = "tadreeblms",
                issueNumber = 546,
                issueTitle = "Reset Button Not Clearing Filter Fields on Dashboard",
                issueBody = "The Reset button in the Dashboard filter section is not functioning as expected. After entering values in the filter fields and clicking the Reset button, the input fields remain populated instead of being cleared.",
                labels = listOf("bug", "good first issue")
            ),
            EvalSample(
                description = "Camunda - RocksDB 메트릭 추가",
                owner = "camunda",
                repo = "camunda",
                issueNumber = 52365,
                issueTitle = "Expose more RocksDB metrics",
                issueBody = "RocksDbMetricsDoc defines which RocksDB properties we expose as metrics. Some useful ones such as rocksdb.num-immutable-mem-table-flushed are missing though.",
                labels = listOf("area/observability", "good first issue")
            ),
            EvalSample(
                description = "super-productivity - 텍스트 필드 리사이즈 버그",
                owner = "super-productivity",
                repo = "super-productivity",
                issueNumber = 7482,
                issueTitle = "text field in the task editing view not resizing properly",
                issueBody = "Especially when entering longer words, the text field of the task editing view is not resizing properly, which results in the disability to see and edit the entire text body.",
                labels = listOf("good first issue", "bug")
            )
        )
    }

    @Test
    fun `고정 샘플 5종으로 프롬프트 평가`() {
        EVAL_SAMPLES.forEachIndexed { index, sample ->
            println("\n===== 샘플 ${index + 1}: ${sample.description} =====")

            try {
                // 1단계: GitHub Tree API로 파일 목록 가져오기
                val allFilePaths = gitHubClient.fetchTree(sample.owner, sample.repo)
                    .filter { path ->
                        path.endsWith(".ts") || path.endsWith(".js") ||
                        path.endsWith(".kt") || path.endsWith(".java") ||
                        path.endsWith(".py") || path.endsWith(".go") ||
                        path.endsWith(".rs") || path.endsWith(".cpp") ||
                        path.endsWith(".json") || path.endsWith(".yaml") ||
                        path.endsWith(".yml") || path.endsWith(".md") ||
                        path.endsWith(".html") || path.endsWith(".css") ||
                        path.endsWith(".scss") || path.endsWith(".xml") ||
                        path.endsWith(".toml") || path.endsWith(".gradle") ||
                        path.endsWith(".properties")
                    }
                println("필터링 후 파일 수: ${allFilePaths.size}")

// 2단계: GLM selectFiles 호출 (동적 배치)
                val batchSizes = listOf(3000, 2000, 1000, 700, 300, 100)
                var selectedPaths: List<String> = emptyList()
                for (size in batchSizes) {
                    try {
                        selectedPaths = glmClient.selectFiles(
                            issueTitle = sample.issueTitle,
                            issueBody = sample.issueBody,
                            filePaths = allFilePaths.take(size)
                        )
                        println("selectFiles 성공: 배치=$size, 선별=${selectedPaths.size}개")
                        break
                    } catch (e: Exception) {
                        println("배치 $size 실패, 축소: ${e.message?.take(50)}")
                    }
                }

                if (selectedPaths.isEmpty()) {
                    println("샘플 ${index + 1} selectFiles 전체 실패, 건너뜀")
                    return@forEachIndexed
                }
                println("선별된 파일: $selectedPaths")

                // 3단계: 선별된 파일 내용 fetch
                val fileContents = gitHubClient.fetchFileContents(
                    sample.owner, sample.repo, selectedPaths
                ).mapValues { (_, content) ->
                    content.lines()
                        .filter { it.isNotBlank() }
                        .filter { line ->
                            val t = line.trimStart()
                            !t.startsWith("//") && !t.startsWith("*") &&
                            !t.startsWith("/*") && !t.startsWith("#")
                        }
                        .joinToString("\n")
                        .take(3000)
                }

                // 4단계: GLM analyze 호출
                val result = glmClient.analyze(
                    issueTitle = sample.issueTitle,
                    issueBody = sample.issueBody,
                    labels = sample.labels,
                    fileContents = fileContents
                )
                println("guideline: ${result.guideline}")
                println("pseudoCode: ${result.pseudoCode.take(100)}...")

            } catch (e: Exception) {
                println("샘플 ${index + 1} 실패 (건너뜀): ${e.message}")
            }

            // rate limit 방지
            if (index < EVAL_SAMPLES.lastIndex) Thread.sleep(15_000)
        }

        // Langfuse fire-and-forget + judge 채점 완료 대기
        Thread.sleep(120_000)
    }
}