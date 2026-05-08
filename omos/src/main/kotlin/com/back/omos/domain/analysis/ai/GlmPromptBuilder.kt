package com.back.omos.domain.analysis.ai

import org.springframework.stereotype.Component

/**
 * GLM API 호출을 위한 프롬프트를 구성하는 클래스입니다.
 *
 * <p>
 * 이슈 정보와 파일 목록/내용을 기반으로 GLM에 전달할 프롬프트 문자열을 생성합니다.
 * 프롬프트를 수정할 때 [PROMPT_VERSION_SELECT_FILES] 또는 [PROMPT_VERSION_ANALYZE]를
 * 함께 올려야 Langfuse에서 버전별 성능 비교가 가능합니다.
 * </p>
 *
 * @author Jaewon Ryu
 * @since 2026-05-06
 */

@Component
class GlmPromptBuilder {

    companion object {
        const val PROMPT_VERSION_SELECT_FILES = "v4"
        const val PROMPT_VERSION_ANALYZE = "v4"
    }
    /**
     * 이슈 해결에 필요한 파일 선별을 위한 프롬프트를 구성합니다.
     *
     * GLM이 반드시 `{ "files": [...] }` 형태의 JSON만 반환하도록 지시합니다.
     *
     * @param issueTitle 이슈 제목
     * @param issueBody 이슈 본문 (없으면 null)
     * @param filePaths 선별 대상 파일 경로 목록
     * @return GLM API에 전달할 프롬프트 문자열
     */
    fun buildSelectFilesPrompt(
        issueTitle: String,
        issueBody: String?,
        filePaths: List<String>
    ): String {
        return """
    당신은 오픈소스 기여를 돕는 코드 분석 전문가입니다.
    아래 이슈를 해결하기 위해 수정이 필요한 파일을 선별해주세요.

    [사고 순서]
    1. 이슈의 핵심 키워드와 변경 범위를 파악하세요.
    2. 키워드와 관련된 파일을 후보 목록에서 찾으세요.
    3. 확실히 관련 없는 파일은 제외하세요.

    [예시]
    이슈: "Button color doesn't change with custom theme"
    선별 파일: ["src/gui/theme/ThemeManager.java", "src/gui/MainWindow.java"]

    이슈: "NPE when opening entry editor with empty field"
    선별 파일: ["src/gui/entryeditor/EntryEditor.java", "src/model/entry/BibEntry.java"]

    [규칙]
    - 반드시 아래 JSON 형식으로만 응답하세요. 설명이나 마크다운 없이 JSON만 출력하세요.
    - 이슈 해결에 필요한 파일을 빠짐없이 선택하세요.
    - 최대 10개까지 선택하세요.

    [이슈 내용]
    제목: $issueTitle
    본문: ${issueBody ?: "내용 없음"}

    [후보 파일 목록]
    ${filePaths.joinToString("\n")}

    [출력 형식]
    {
        "files": ["파일경로1", "파일경로2"]
    }
""".trimIndent()
    }
    /**
     * 이슈와 관련 코드를 기반으로 수정 가이드 생성을 위한 프롬프트를 구성합니다.
     *
     * 이슈 제목·본문·라벨과 관련 소스코드를 포함하며,
     * GLM이 반드시 `guideline`, `pseudoCode`, `sideEffects` 필드를 가진
     * JSON만 반환하도록 지시합니다.
     *
     * @param issueTitle 이슈 제목
     * @param issueBody 이슈 본문 (없으면 null)
     * @param labels 이슈 라벨 목록
     * @param fileContents 관련 파일 경로와 내용의 맵 (key: 파일 경로, value: 파일 내용)
     * @return GLM API에 전달할 프롬프트 문자열
     */
    fun buildAnalyzePrompt(
        issueTitle: String,
        issueBody: String?,
        labels: List<String>,
        fileContents: Map<String, String>
    ): String {
        val filesSection = fileContents.entries.joinToString("\n\n") { (path, content) ->
            "### $path\n```\n$content\n```"
        }
        val labelsSection = if (labels.isEmpty()) "없음" else labels.joinToString(", ")
        return """
    당신은 오픈소스 기여를 돕는 코드 분석 전문가입니다.
    아래 이슈와 관련 코드를 분석하여 수정 가이드를 작성해주세요.

[사고 순서]
1. 이슈에서 요구하는 변경 유형을 파악하세요 (버그수정/기능추가/리팩토링).
2. 이슈 본문에 명시된 내용만 근거로 사용하세요. 없는 내용(Priority 3 등)을 임의로 추가하지 마세요.
3. 제공된 코드에서 관련 부분을 찾으세요. 없더라도 이슈 내용을 기반으로 수정 방향을 추론하세요.
4. pseudoCode는 파일경로 > 클래스/함수 단위로 구체적으로 서술하세요.

    [규칙]
    - 반드시 아래 JSON 형식으로만 응답하세요. 설명이나 마크다운 없이 JSON만 출력하세요.
    - 코드와 파일 경로, 함수명은 원문 그대로 사용하세요 (번역 금지).
    - guideline, sideEffects는 한국어로 작성하세요.
    - pseudoCode는 완전한 코드가 아닌 변경 방향 설명이어야 합니다.

    [이슈 내용]
    제목: $issueTitle
    라벨: $labelsSection
    본문: ${issueBody ?: "내용 없음"}

    [관련 코드]
    $filesSection

    [출력 형식]
    {
        "guideline": "이슈의 원인과 전체적인 수정 방향 설명 (한국어, 하나의 문자열로)",
        "pseudoCode": "수정이 필요한 파일경로 > 클래스/함수명: 변경 방향 설명. 완전한 코드 작성 금지.배열이 아닌 하나의 문자열로 작성",
        "sideEffects": "이 수정으로 인해 발생할 수 있는 부작용 또는 주의사항 (한국어, 하나의 문자열로)"
    }
""".trimIndent()
    }
}