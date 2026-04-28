"use client";

import { useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import ProtectedLayout from "@/components/ProtectedLayout";
import { prApi, PrInfoRes } from "@/lib/api";

const DIFF_PLACEHOLDER = `diff --git a/src/main/kotlin/Example.kt b/src/main/kotlin/Example.kt
index abc1234..def5678 100644
--- a/src/main/kotlin/Example.kt
+++ b/src/main/kotlin/Example.kt
@@ -10,6 +10,10 @@ class Example {
     fun existingMethod() {
         // existing code
     }
+
+    fun newMethod() {
+        // your implementation
+    }
 }`;

function PrDraftContent() {
  const searchParams = useSearchParams();
  const issueId = searchParams.get("issueId");
  const repoName = searchParams.get("repo");
  const issueTitle = searchParams.get("title");

  const [diffContent, setDiffContent] = useState("");
  const [result, setResult] = useState<PrInfoRes | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState<"title" | "body" | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!issueId || !diffContent.trim()) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await prApi.create({
        issueId: Number(issueId),
        diffContent,
      });
      if (res.status === "success" && res.data) {
        setResult(res.data);
      } else {
        setError(res.message ?? "PR 초안 생성에 실패했습니다.");
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function copyToClipboard(text: string, type: "title" | "body") {
    await navigator.clipboard.writeText(text);
    setCopied(type);
    setTimeout(() => setCopied(null), 2000);
  }

  return (
    <ProtectedLayout>
      <div className="mb-6 flex items-center gap-3">
        <Link
          href="/issues"
          className="text-github-muted hover:text-github-text text-sm transition-colors"
        >
          ← 이슈 목록
        </Link>
        <h1 className="text-2xl font-bold text-github-text">PR 초안 생성</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left: Input */}
        <div className="space-y-4">
          {/* Issue Info */}
          {issueId ? (
            <div className="bg-github-surface border border-github-border rounded-xl p-5">
              <p className="text-github-muted text-xs mb-1">선택된 이슈</p>
              {repoName && (
                <p className="text-github-muted text-xs mb-1">{repoName}</p>
              )}
              <p className="text-github-text font-medium">
                {issueTitle ?? `Issue #${issueId}`}
              </p>
              <p className="text-github-muted text-xs mt-1">ID: {issueId}</p>
            </div>
          ) : (
            <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-xl p-5">
              <p className="text-yellow-400 text-sm">
                이슈를 선택하지 않았습니다.{" "}
                <Link href="/issues" className="underline">
                  이슈 목록
                </Link>
                에서 이슈를 선택하세요.
              </p>
              <div className="mt-3">
                <label className="block text-github-muted text-xs mb-1.5">
                  또는 Issue ID를 직접 입력
                </label>
                <input
                  type="number"
                  placeholder="Issue ID"
                  onChange={(e) => {
                    const params = new URLSearchParams(window.location.search);
                    params.set("issueId", e.target.value);
                    window.history.replaceState(
                      {},
                      "",
                      `?${params.toString()}`
                    );
                  }}
                  className="w-full bg-github-bg border border-github-border rounded-lg px-3 py-2 text-github-text text-sm focus:outline-none focus:border-github-purple transition-colors"
                />
              </div>
            </div>
          )}

          {/* Diff Input */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-github-muted text-xs mb-1.5">
                Git Diff 내용
                <span className="text-red-400 ml-1">*</span>
              </label>
              <textarea
                value={diffContent}
                onChange={(e) => setDiffContent(e.target.value)}
                placeholder={DIFF_PLACEHOLDER}
                rows={16}
                className="w-full bg-github-bg border border-github-border rounded-lg px-4 py-3 text-github-text text-xs font-mono focus:outline-none focus:border-github-purple transition-colors resize-none leading-relaxed"
              />
              <p className="text-github-muted text-xs mt-1">
                터미널에서{" "}
                <code className="bg-github-bg px-1 py-0.5 rounded text-github-accent">
                  git diff HEAD
                </code>{" "}
                또는{" "}
                <code className="bg-github-bg px-1 py-0.5 rounded text-github-accent">
                  git diff main...feat
                </code>{" "}
                출력을 붙여넣으세요.
              </p>
            </div>

            <button
              type="submit"
              disabled={loading || !diffContent.trim()}
              className="w-full py-3 bg-github-purple hover:bg-github-purple/90 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  AI가 PR 초안을 생성 중... (10~30초 소요)
                </>
              ) : (
                "PR 초안 생성"
              )}
            </button>
          </form>

          {error && (
            <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}
        </div>

        {/* Right: Result */}
        <div>
          {result ? (
            <div className="space-y-4">
              <h2 className="text-github-text font-semibold">생성된 PR 초안</h2>

              {/* PR Title */}
              <div className="bg-github-surface border border-github-border rounded-xl p-5">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-github-muted text-xs">PR 제목</p>
                  <button
                    onClick={() => copyToClipboard(result.title, "title")}
                    className="text-github-muted hover:text-github-accent text-xs transition-colors"
                  >
                    {copied === "title" ? "복사됨 ✓" : "복사"}
                  </button>
                </div>
                <p className="text-github-text font-medium">{result.title}</p>
              </div>

              {/* PR Body */}
              <div className="bg-github-surface border border-github-border rounded-xl p-5">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-github-muted text-xs">PR 본문</p>
                  <button
                    onClick={() => copyToClipboard(result.body, "body")}
                    className="text-github-muted hover:text-github-accent text-xs transition-colors"
                  >
                    {copied === "body" ? "복사됨 ✓" : "복사"}
                  </button>
                </div>
                <pre className="text-github-text text-xs font-mono whitespace-pre-wrap leading-relaxed max-h-80 overflow-y-auto">
                  {result.body}
                </pre>
              </div>

              {/* GitHub Link */}
              {result.githubUrl && (
                <a
                  href={result.githubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-2 w-full py-3 bg-github-accent/10 hover:bg-github-accent/20 text-github-accent font-medium rounded-xl border border-github-accent/30 transition-colors text-sm"
                >
                  GitHub에서 PR 열기 ↗
                </a>
              )}
            </div>
          ) : (
            <div className="bg-github-surface border border-github-border rounded-xl p-8 h-full flex items-center justify-center">
              <div className="text-center text-github-muted">
                <p className="text-4xl mb-3">✨</p>
                <p className="text-sm">
                  왼쪽에서 diff를 입력하고 생성 버튼을 누르면
                  <br />
                  AI가 PR 초안을 자동 작성합니다.
                </p>
              </div>
            </div>
          )}
        </div>
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
