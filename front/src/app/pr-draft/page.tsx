"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import ProtectedLayout from "@/components/ProtectedLayout";
import {
  prApi,
  PrInfoRes,
  PrTranslateRes,
  PrHistoryRes,
} from "@/lib/api";

function PrDraftContent() {
  const searchParams = useSearchParams();
  const repo = searchParams.get("repo") ?? "";
  const issueNumber = Number(searchParams.get("issueNumber") ?? "0");
  const issueId = searchParams.get("issueId") ?? "";
  const issueTitle = searchParams.get("issueTitle") ?? "";

  const [baseBranch, setBaseBranch] = useState("main");
  const [headBranch, setHeadBranch] = useState("");

  const [createdPr, setCreatedPr] = useState<PrInfoRes | null>(null);
  const [translated, setTranslated] = useState<PrTranslateRes | null>(null);
  const [loading, setLoading] = useState(false);
  const [translating, setTranslating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState<"title" | "body" | null>(null);

  const [history, setHistory] = useState<PrHistoryRes[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);

  useEffect(() => {
    loadHistory(0);
  }, []);

  async function loadHistory(page: number) {
    setHistoryLoading(true);
    try {
      const res = await prApi.getHistory(page, 5);
      if (res.status === "success" && res.data) {
        setHistory(res.data.content);
        setHistoryTotalPages(res.data.totalPages);
        setHistoryPage(page);
      }
    } catch {
      // non-critical
    } finally {
      setHistoryLoading(false);
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!repo || !issueNumber || !baseBranch.trim() || !headBranch.trim()) return;
    setLoading(true);
    setError(null);
    setCreatedPr(null);
    setTranslated(null);
    try {
      const res = await prApi.create({
        upstreamRepo: repo,
        githubIssueNumber: issueNumber,
        baseBranch: baseBranch.trim(),
        headBranch: headBranch.trim(),
      });
      if (res.status === "success" && res.data) {
        setCreatedPr(res.data);
        loadHistory(0);
      } else {
        setError(res.message ?? "PR 초안 생성에 실패했습니다.");
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function handleTranslate() {
    if (!createdPr) return;
    setTranslating(true);
    setError(null);
    try {
      const res = await prApi.translate(createdPr.id);
      if (res.status === "success" && res.data) setTranslated(res.data);
      else setError(res.message ?? "번역에 실패했습니다.");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setTranslating(false);
    }
  }

  async function copyToClipboard(text: string, type: "title" | "body") {
    await navigator.clipboard.writeText(text);
    setCopied(type);
    setTimeout(() => setCopied(null), 2000);
  }

  const hasIssueParam = !!repo && !!issueNumber;
  const displayTitle = translated?.titleEn ?? createdPr?.title ?? "";
  const displayBody = translated?.bodyEn ?? createdPr?.body ?? "";

  return (
    <ProtectedLayout>
      <div className="mb-6 flex items-center gap-3">
        {hasIssueParam && issueId ? (
          <Link
            href={`/issues/${issueId}`}
            className="text-github-muted hover:text-github-text text-sm transition-colors"
          >
            ← 이슈로 돌아가기
          </Link>
        ) : (
          <Link
            href="/issues"
            className="text-github-muted hover:text-github-text text-sm transition-colors"
          >
            ← 추천 이슈
          </Link>
        )}
        <h1 className="text-2xl font-bold text-github-text">PR 초안 생성</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-10">
        {/* Left: Input */}
        <div className="space-y-4">
          {hasIssueParam ? (
            <div className="bg-github-surface border border-github-border rounded-xl p-5">
              <p className="text-github-muted text-xs mb-1">선택된 이슈</p>
              <p className="text-github-muted text-xs mb-1">{repo}</p>
              <p className="text-github-text font-medium">
                {issueTitle || `Issue #${issueNumber}`}
              </p>
              <p className="text-github-muted text-xs mt-1">#{issueNumber}</p>
            </div>
          ) : (
            <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-xl p-5">
              <p className="text-yellow-400 text-sm">
                이슈 상세 페이지에서 "PR 초안 만들기"를 통해 접근해주세요.{" "}
                <Link href="/issues" className="underline">
                  이슈 목록으로
                </Link>
              </p>
            </div>
          )}

          {hasIssueParam && (
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-github-muted text-xs mb-1.5">
                  Base 브랜치 <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={baseBranch}
                  onChange={(e) => setBaseBranch(e.target.value)}
                  placeholder="main"
                  className="w-full bg-github-bg border border-github-border rounded-lg px-3 py-2 text-github-text text-sm font-mono focus:outline-none focus:border-github-purple transition-colors"
                />
                <p className="text-github-muted text-xs mt-1">
                  PR의 대상 브랜치 (예: main, develop)
                </p>
              </div>

              <div>
                <label className="block text-github-muted text-xs mb-1.5">
                  Head 브랜치 <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={headBranch}
                  onChange={(e) => setHeadBranch(e.target.value)}
                  placeholder="feat/my-feature"
                  className="w-full bg-github-bg border border-github-border rounded-lg px-3 py-2 text-github-text text-sm font-mono focus:outline-none focus:border-github-purple transition-colors"
                />
                <p className="text-github-muted text-xs mt-1">
                  내 작업 브랜치 (예: feat/fix-issue-123)
                </p>
              </div>

              <button
                type="submit"
                disabled={loading || !baseBranch.trim() || !headBranch.trim()}
                className="w-full py-3 bg-github-purple hover:bg-github-purple/90 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    AI가 PR 초안 생성 중...
                  </>
                ) : (
                  "PR 초안 생성"
                )}
              </button>
            </form>
          )}

          {error && (
            <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}
        </div>

        {/* Right: Result */}
        <div>
          {createdPr ? (
            <div className="space-y-4">
              <h2 className="text-github-text font-semibold">
                생성된 PR 초안
                {translated && (
                  <span className="ml-2 text-github-accent text-sm font-normal">
                    (영어 번역 완료)
                  </span>
                )}
              </h2>

              <div className="bg-github-surface border border-github-border rounded-xl p-5">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-github-muted text-xs">PR 제목</p>
                  <button
                    onClick={() => copyToClipboard(displayTitle, "title")}
                    className="text-github-muted hover:text-github-accent text-xs transition-colors"
                  >
                    {copied === "title" ? "복사됨 ✓" : "복사"}
                  </button>
                </div>
                <p className="text-github-text font-medium">{displayTitle}</p>
              </div>

              <div className="bg-github-surface border border-github-border rounded-xl p-5">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-github-muted text-xs">PR 본문</p>
                  <button
                    onClick={() => copyToClipboard(displayBody, "body")}
                    className="text-github-muted hover:text-github-accent text-xs transition-colors"
                  >
                    {copied === "body" ? "복사됨 ✓" : "복사"}
                  </button>
                </div>
                <pre className="text-github-text text-xs font-mono whitespace-pre-wrap leading-relaxed max-h-64 overflow-y-auto">
                  {displayBody}
                </pre>
              </div>

              {!translated ? (
                <button
                  onClick={handleTranslate}
                  disabled={translating}
                  className="w-full py-2.5 bg-github-accent/10 hover:bg-github-accent/20 disabled:opacity-50 text-github-accent font-medium rounded-lg border border-github-accent/30 transition-colors text-sm flex items-center justify-center gap-2"
                >
                  {translating ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-github-accent border-t-transparent rounded-full animate-spin" />
                      영어로 번역 중...
                    </>
                  ) : (
                    "영어로 번역하기"
                  )}
                </button>
              ) : (
                <a
                  href={translated.githubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-2 w-full py-3 bg-github-accent/10 hover:bg-github-accent/20 text-github-accent font-medium rounded-xl border border-github-accent/30 transition-colors text-sm"
                >
                  GitHub에서 PR 올리기 ↗
                </a>
              )}

              <Link
                href={`/pr/${createdPr.id}`}
                className="block text-center text-github-muted hover:text-github-text text-xs transition-colors"
              >
                상세 보기 (수정·삭제) →
              </Link>
            </div>
          ) : (
            <div className="bg-github-surface border border-github-border rounded-xl p-8 h-full flex items-center justify-center">
              <div className="text-center text-github-muted">
                <p className="text-4xl mb-3">✨</p>
                <p className="text-sm">
                  브랜치 정보를 입력하고 생성 버튼을 누르면
                  <br />
                  AI가 PR 초안을 자동 작성합니다.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* PR History */}
      <div>
        <h2 className="text-github-text font-semibold mb-4">PR 초안 이력</h2>
        {historyLoading ? (
          <div className="space-y-2">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-16 bg-github-surface rounded-xl animate-pulse" />
            ))}
          </div>
        ) : history.length === 0 ? (
          <div className="text-center py-8 text-github-muted text-sm">
            아직 생성된 PR 초안이 없습니다.
          </div>
        ) : (
          <>
            <div className="space-y-2">
              {history.map((pr) => (
                <Link
                  key={pr.id}
                  href={`/pr/${pr.id}`}
                  className="flex items-center justify-between bg-github-surface border border-github-border rounded-xl px-5 py-4 hover:border-github-purple/50 transition-colors"
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-github-text text-sm font-medium truncate">
                      {pr.title}
                    </p>
                    <p className="text-github-muted text-xs mt-0.5">
                      {pr.repoFullName} ·{" "}
                      {new Date(pr.createdAt).toLocaleDateString("ko-KR")}
                    </p>
                  </div>
                  <span className="text-github-muted text-xs shrink-0 ml-4">
                    보기 →
                  </span>
                </Link>
              ))}
            </div>

            {historyTotalPages > 1 && (
              <div className="flex justify-center items-center gap-3 mt-4">
                <button
                  onClick={() => loadHistory(historyPage - 1)}
                  disabled={historyPage === 0}
                  className="px-3 py-1 text-xs text-github-muted hover:text-github-text disabled:opacity-30 transition-colors"
                >
                  ← 이전
                </button>
                <span className="text-xs text-github-muted">
                  {historyPage + 1} / {historyTotalPages}
                </span>
                <button
                  onClick={() => loadHistory(historyPage + 1)}
                  disabled={historyPage >= historyTotalPages - 1}
                  className="px-3 py-1 text-xs text-github-muted hover:text-github-text disabled:opacity-30 transition-colors"
                >
                  다음 →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </ProtectedLayout>
  );
}

export default function PrDraftPage() {
  return (
    <Suspense
      fallback={
        <ProtectedLayout>
          <div className="h-64 bg-github-surface rounded-xl animate-pulse" />
        </ProtectedLayout>
      }
    >
      <PrDraftContent />
    </Suspense>
  );
}
