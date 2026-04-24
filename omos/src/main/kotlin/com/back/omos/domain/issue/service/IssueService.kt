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

    /**
     * 지정된 레포지토리의 GitHub 이슈를 수집하고 벡터 임베딩을 생성하여 저장합니다.
     *
     * GitHub REST API를 호출하여 최신 이슈 데이터를 가져오며,
     * 수집된 텍스트 데이터(제목, 본문)는 AI 모델을 통해 고차원 벡터로 변환됩니다.
     * * [설계 특징]
     * 1. 도메인 간 결합도를 낮추기 위해 객체 참조 대신 [repoId]를 직접 저장합니다.
     * 2. 중복 수집 방지를 위해 기존에 저장된 이슈 번호([issueNumber])와 비교 로직이 포함될 수 있습니다.
     * 3. 저장된 벡터 데이터는 추후 [recommendIssues]에서 유사도 기반 추천에 활용됩니다.
     *
     * @param repoId 이슈를 수집할 대상 레포지토리의 고유 식별자(ID)
     * @throws IllegalArgumentException 존재하지 않는 [repoId]이거나 API 호출에 실패한 경우 발생
     */
    fun crawlAndSave(repoId: Long)
}