package com.back.omos.domain.analysis.dto

/**
 * AI가 생성한 코드 수정 가이드를 응답하는 DTO입니다.
 * <p>
 * 사용자가 이슈를 선택하면 Context Analyzer가 분석한 결과 중
 * 수정 파일 위치, 가이드라인, 부작용 정보를 프론트엔드에 전달합니다.
 * AnalysisResult 엔티티에서 pseudoCode를 제외한 가이드 정보만 담습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code GuideResponseDto(List<String>, String, String)} <br>
 * filePaths: 수정 대상 파일 경로 목록 <br>
 * guideline: AI가 생성한 수정 방향 안내 <br>
 * sideEffects: 수정 시 발생 가능한 부작용 <br>
 *
 * @author Jaewon Ryu
 * @since 2026-04-22
 * @see com.back.omos.domain.analysis.entity.AnalysisResult
 */
data class GuideResponseDto(
    val filePaths: List<String>,
    val guideline: String,
    val sideEffects: String
)