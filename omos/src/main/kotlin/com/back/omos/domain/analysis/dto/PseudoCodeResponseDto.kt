package com.back.omos.domain.analysis.dto

/**
 * AI가 생성한 의사 코드를 응답하는 DTO입니다.
 * <p>
 * 사용자가 "코드도 보여줘" 버튼을 눌렀을 때,
 * AnalysisResult 엔티티에서 수정 대상 파일 경로와 의사 코드를
 * 추출하여 프론트엔드에 전달합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code PseudoCodeResponseDto(List<String>, String)} <br>
 * filePaths: 수정 대상 파일 경로 목록 <br>
 * pseudoCode: AI가 생성한 구체적인 코드 수정 제안 <br>
 *
 * @author Jaewon Ryu
 * @since 2026-04-22
 * @see com.back.omos.domain.analysis.entity.AnalysisResult
 */
data class PseudoCodeResponseDto(
    val filePaths: List<String>,
    val pseudoCode: String
)