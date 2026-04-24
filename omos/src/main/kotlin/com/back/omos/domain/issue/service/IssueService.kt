package com.back.omos.domain.issue.service

import com.back.omos.domain.issue.dto.CreateIssueReq
import com.back.omos.domain.issue.dto.IssueInfoRes
import com.back.omos.domain.issue.dto.RecommendIssueRes
import com.back.omos.domain.issue.dto.UpdateIssueReq

/**
 * 이슈 관련 비즈니스 로직을 처리하는 서비스 인터페이스입니다.
 *
 * <p>
 * 이슈 CRUD 및 사용자 맞춤 추천 등의 기능을 제공합니다.
 *
 * @author 유재원
 * @since 2026-04-22
 */
interface IssueService {

    /**
     * 새로운 이슈를 생성합니다.
     *
     * @param createIssueReq 이슈 생성 요청 정보
     * @return 생성된 이슈 정보
     */
    fun createIssue(request: CreateIssueReq): IssueInfoRes

    /**
     * 특정 이슈의 상세 정보를 조회합니다.
     *
     * @param issueId 조회할 이슈의 시스템 고유 식별자(PK)
     * @return 조회된 이슈의 상세 정보
     */
    fun getIssue(issueId: Long): IssueInfoRes

    /**
     * 기존 이슈의 내용을 수정합니다.
     *
     * @param issueId 수정할 이슈의 시스템 고유 식별자(PK)
     * @param request 이슈 수정 요청 정보 (제목, 본문, 라벨, 상태 등 변경할 데이터)
     */
    fun updateIssue(issueId: Long, request: UpdateIssueReq)

    /**
     * 특정 이슈를 삭제(제거)합니다.
     *
     * @param issueId 삭제할 이슈의 시스템 고유 식별자(PK)
     */
    fun deleteIssue(issueId: Long)

    /**
     * 사용자 맞춤 이슈 추천 목록을 반환합니다.
     *
     * @param userId 사용자 고유 식별자
     * @param repositoryId 이슈를 추천받을 레포지토리 ID
     * @param limit 추천받을 이슈 개수
     * @return 추천된 이슈 목록
     */
    fun recommendIssues(userId: Long, repositoryId: Long, limit: Int): List<RecommendIssueRes>
}