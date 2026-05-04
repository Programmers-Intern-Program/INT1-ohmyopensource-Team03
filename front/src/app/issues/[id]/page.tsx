"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import ProtectedLayout from "@/components/ProtectedLayout";
import {
  issueApi,
  RecommendIssueHistoryRes,
  GuideResponseDto,
  PseudoCodeResponseDto,
} from "@/lib/api";

export default function IssueDetailPage() {
  const params = useParams();
  const issueId = Number(params.id);

  const [issue, setIssue] = useState<RecommendIssueHistoryRes | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [guide, setGuide] = useState<GuideResponseDto | null>(null);
  const [guideLoading, setGuideLoading] = useState(false);
  const [guideError, setGuideError] = useState<string | null>(null);

  const [pseudo, setPseudo] = useState<PseudoCodeResponseDto | null>(null);
  const [pseudoLoading, setPseudoLoading] = useState(false);
  const [pseudoError, setPseudoError] = useState<string | null>(null);

  useEffect(() => {
    issueApi
      .getHistory()
      .then((res) => {
        if (res.status === "success" && res.data) {
          const found = res.data.find((item) => item.id === issueId);
          if (found) setIssue(found);
          else setNotFound(true);
        } else {
          setNotFound(true);
        }
      })
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [issueId]);

  async function handleGetGuide() {
    setGuideLoading(true);
    setGuideError(null);
    try {
      const res = await issueApi.getGuide(issueId);
      if (res.status === "success" && res.data) setGuide(res.data);
      else setGuideError(res.message ?? "가이드를 불러오지 못했습니다.");
    } catch (e) {
      setGuideError((e as Error).message);
    } finally {
      setGuideLoading(false);
    }
  }

  async function handleGetPseudo() {
    setPseudoLoading(true);
    setPseudoError(null);
    try {
      const res = await issueApi.getPseudoCode(issueId);
      if (res.status === "success" && res.data) setPseudo(res.data);
      else setPseudoError(res.message ?? "의사 코드를 불러오지 못했습니다.");
    } catch (e) {
      setPseudoError((e as Error).message);
    } finally {
      setPseudoLoading(false);
    }
  }

  if (loading) {
    return (
      <ProtectedLayout>
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-24 bg-github-surface rounded-xl animate-pulse" />
          ))}
        </div>
      </ProtectedLayout>
    );
  }

  if (notFound || !issue) {
    return (
      <ProtectedLayout>
        <div className="text-center py-24 text-github-muted">
          <p className="text-5xl mb-4">😕</p>
          <p className="mb-4">이슈를 찾을 수 없습니다.</p>
          <Link
            href="/issues"
            className="text-github-accent hover:underline text-sm"
          >
            ← 추천 이슈 목록으로
          </Link>
        </div>
      </ProtectedLayout>
    );
  }

  const githubUrl = `https://github.com/${issue.repoFullName}/issues/${issue.issueNumber}`;

  return (
    <ProtectedLayout>
      <div className="mb-6">
        <Link
          href="/issues"
          className="text-github-muted hover:text-github-text text-sm transition-colors"
        >
          ← 추천 이슈 목록
        </Link>
      </div>

      {/* Issue Info Card */}
      <div className="bg-github-surface border border-github-border rounded-xl p-6 mb-5">
        <div className="flex items-center gap-2 mb-3 flex-wrap">
          <StatusBadge status={issue.status} />
          <a
            href={githubUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-github-muted text-xs hover:text-github-accent transition-colors"
          >
            {issue.repoFullName} #{issue.issueNumber} ↗
          </a>
          {issue.score > 0 && (
            <span className="text-xs px-2 py-0.5 bg-green-500/10 text-green-400 rounded-full border border-green-500/20">
              매칭 {Math.round(issue.score * 100)}%
            </span>
          )}
          {issue.isAnalyzed && (
            <span className="text-xs px-2 py-0.5 bg-github-accent/10 text-github-accent rounded-full border border-github-accent/20">
              분석 완료
            </span>
          )}
        </div>

        <h1 className="text-xl font-bold text-github-text mb-4">{issue.title}</h1>

        {issue.labels && issue.labels.length > 0 && (
          <div className="flex gap-1.5 flex-wrap mb-4">
            {issue.labels.map((label) => (
              <span
                key={label}
                className="px-2 py-0.5 bg-github-accent/10 text-github-accent text-xs rounded-full border border-github-accent/20"
              >
                {label}
              </span>
            ))}
          </div>
        )}

        {issue.summary && (
          <div className="bg-github-bg rounded-lg p-4">
            <p className="text-github-muted text-xs mb-1.5">추천 이유</p>
            <p className="text-github-text text-sm leading-relaxed">{issue.summary}</p>
          </div>
        )}
      </div>

      {/* 1차 힌트 */}
      <div className="bg-github-surface border border-github-border rounded-xl p-6 mb-4">
        <div className="flex items-start justify-between gap-4 mb-3">
          <div>
            <h2 className="text-github-text font-semibold">1차 힌트 — 수정 가이드</h2>
            <p className="text-github-muted text-xs mt-0.5">
              수정 대상 파일과 구현 방향을 안내합니다.
            </p>
          </div>
          {!guide && (
            <button
              onClick={handleGetGuide}
              disabled={guideLoading}
              className="shrink-0 flex items-center gap-2 px-4 py-2 bg-github-purple/20 hover:bg-github-purple/40 disabled:opacity-50 text-github-purple-light text-sm font-medium rounded-lg transition-colors"
            >
              {guideLoading ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-github-purple-light border-t-transparent rounded-full animate-spin" />
                  분석 중...
                </>
              ) : (
                "힌트 보기"
              )}
            </button>
          )}
        </div>

        {guideError && (
          <p className="text-red-400 text-sm">{guideError}</p>
        )}

        {guide && (
          <div className="space-y-4">
            <div>
              <p className="text-github-muted text-xs mb-2">수정 대상 파일</p>
              <div className="space-y-1">
                {guide.filePaths.map((fp) => (
                  <code
                    key={fp}
                    className="block text-github-accent text-xs bg-github-bg px-3 py-1.5 rounded"
                  >
                    {fp}
                  </code>
                ))}
              </div>
            </div>
            <div>
              <p className="text-github-muted text-xs mb-2">가이드라인</p>
              <p className="text-github-text text-sm leading-relaxed whitespace-pre-wrap">
                {guide.guideline}
              </p>
            </div>
            <div>
              <p className="text-github-muted text-xs mb-2">주의 사항 (Side Effects)</p>
              <p className="text-github-text text-sm leading-relaxed whitespace-pre-wrap">
                {guide.sideEffects}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* 2차 힌트 */}
      <div className="bg-github-surface border border-github-border rounded-xl p-6 mb-5">
        <div className="flex items-start justify-between gap-4 mb-3">
          <div>
            <h2 className="text-github-text font-semibold">2차 힌트 — 의사 코드</h2>
            <p className="text-github-muted text-xs mt-0.5">
              구체적인 코드 수정 제안을 제공합니다.
            </p>
          </div>
          {!pseudo && (
            <button
              onClick={handleGetPseudo}
              disabled={pseudoLoading}
              className="shrink-0 flex items-center gap-2 px-4 py-2 bg-github-accent/10 hover:bg-github-accent/20 disabled:opacity-50 text-github-accent text-sm font-medium rounded-lg border border-github-accent/30 transition-colors"
            >
              {pseudoLoading ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-github-accent border-t-transparent rounded-full animate-spin" />
                  분석 중...
                </>
              ) : (
                "코드 힌트 보기"
              )}
            </button>
          )}
        </div>

        {pseudoError && (
          <p className="text-red-400 text-sm">{pseudoError}</p>
        )}

        {pseudo && (
          <div className="space-y-4">
            <div>
              <p className="text-github-muted text-xs mb-2">수정 대상 파일</p>
              <div className="space-y-1">
                {pseudo.filePaths.map((fp) => (
                  <code
                    key={fp}
                    className="block text-github-accent text-xs bg-github-bg px-3 py-1.5 rounded"
                  >
                    {fp}
                  </code>
                ))}
              </div>
            </div>
            <div>
              <p className="text-github-muted text-xs mb-2">의사 코드</p>
              <pre className="text-github-text text-xs font-mono bg-github-bg rounded-lg p-4 overflow-x-auto leading-relaxed whitespace-pre-wrap max-h-96 overflow-y-auto">
                {pseudo.pseudoCode}
              </pre>
            </div>
          </div>
        )}
      </div>

      {/* PR 초안 생성 */}
      <div className="bg-github-surface border border-github-purple/30 rounded-xl p-6">
        <h2 className="text-github-text font-semibold mb-1">PR 초안 생성</h2>
        <p className="text-github-muted text-sm mb-4">
          코드 작업이 완료되었다면 PR 초안을 작성하세요.
        </p>
        <Link
          href={`/pr-draft?repo=${encodeURIComponent(issue.repoFullName)}&issueNumber=${issue.issueNumber}&issueId=${issue.id}&issueTitle=${encodeURIComponent(issue.title)}`}
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-github-purple hover:bg-github-purple/90 text-white text-sm font-medium rounded-lg transition-colors"
        >
          PR 초안 만들기 →
        </Link>
      </div>
    </ProtectedLayout>
  );
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={`text-xs px-1.5 py-0.5 rounded-full ${
        status === "OPEN"
          ? "bg-green-500/20 text-green-400"
          : "bg-github-muted/20 text-github-muted"
      }`}
    >
      {status === "OPEN" ? "● OPEN" : "○ CLOSED"}
    </span>
  );
}
