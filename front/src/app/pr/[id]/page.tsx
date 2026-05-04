"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import ProtectedLayout from "@/components/ProtectedLayout";
import { prApi, PrDetailRes, PrTranslateRes } from "@/lib/api";

export default function PrDetailPage() {
  const params = useParams();
  const router = useRouter();
  const prId = Number(params.id);

  const [pr, setPr] = useState<PrDetailRes | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [translated, setTranslated] = useState<PrTranslateRes | null>(null);
  const [translating, setTranslating] = useState(false);

  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editBody, setEditBody] = useState("");
  const [saving, setSaving] = useState(false);

  const [deleting, setDeleting] = useState(false);
  const [copied, setCopied] = useState<"title" | "body" | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    prApi
      .getOne(prId)
      .then((res) => {
        if (res.status === "success" && res.data) {
          setPr(res.data);
          setEditTitle(res.data.title);
          setEditBody(res.data.body);
        } else {
          setNotFound(true);
        }
      })
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [prId]);

  async function handleTranslate() {
    setTranslating(true);
    setError(null);
    try {
      const res = await prApi.translate(prId);
      if (res.status === "success" && res.data) setTranslated(res.data);
      else setError(res.message ?? "번역에 실패했습니다.");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setTranslating(false);
    }
  }

  async function handleSave() {
    if (!pr) return;
    setSaving(true);
    setError(null);
    try {
      const res = await prApi.update(prId, {
        title: editTitle,
        body: editBody,
      });
      if (res.status === "success" && res.data) {
        setPr(res.data);
        setEditing(false);
        setTranslated(null);
      } else {
        setError(res.message ?? "저장에 실패했습니다.");
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!confirm("PR 초안을 삭제하시겠습니까?")) return;
    setDeleting(true);
    try {
      await prApi.delete(prId);
      router.push("/pr-draft");
    } catch (e) {
      setError((e as Error).message);
      setDeleting(false);
    }
  }

  async function copyToClipboard(text: string, type: "title" | "body") {
    await navigator.clipboard.writeText(text);
    setCopied(type);
    setTimeout(() => setCopied(null), 2000);
  }

  function cancelEdit() {
    if (!pr) return;
    setEditing(false);
    setEditTitle(pr.title);
    setEditBody(pr.body);
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

  if (notFound || !pr) {
    return (
      <ProtectedLayout>
        <div className="text-center py-24 text-github-muted">
          <p className="text-5xl mb-4">😕</p>
          <p className="mb-4">PR 초안을 찾을 수 없습니다.</p>
          <Link
            href="/pr-draft"
            className="text-github-accent hover:underline text-sm"
          >
            ← PR 초안 목록으로
          </Link>
        </div>
      </ProtectedLayout>
    );
  }

  const displayTitle = translated?.titleEn ?? pr.title;
  const displayBody = translated?.bodyEn ?? pr.body;

  return (
    <ProtectedLayout>
      <div className="mb-6 flex items-center justify-between">
        <Link
          href="/pr-draft"
          className="text-github-muted hover:text-github-text text-sm transition-colors"
        >
          ← PR 초안 목록
        </Link>
        <div className="flex items-center gap-2">
          {!editing && !translated && (
            <button
              onClick={() => setEditing(true)}
              className="px-3 py-1.5 text-xs text-github-muted hover:text-github-text bg-github-surface border border-github-border rounded-lg transition-colors"
            >
              편집
            </button>
          )}
          <button
            onClick={handleDelete}
            disabled={deleting}
            className="px-3 py-1.5 text-xs text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 rounded-lg transition-colors disabled:opacity-50"
          >
            {deleting ? "삭제 중..." : "삭제"}
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {/* 연결된 이슈 */}
      <div className="bg-github-surface border border-github-border rounded-xl px-5 py-4 mb-5 flex items-center justify-between">
        <div>
          <p className="text-github-muted text-xs mb-0.5">연결된 이슈</p>
          <p className="text-github-text text-sm font-medium">{pr.issueTitle}</p>
          <p className="text-github-muted text-xs">{pr.repoFullName}</p>
        </div>
        <p className="text-github-muted text-xs shrink-0 ml-4">
          {new Date(pr.createdAt).toLocaleString("ko-KR")}
        </p>
      </div>

      {translated && (
        <div className="mb-4 px-4 py-3 bg-github-accent/10 border border-github-accent/30 rounded-lg text-github-accent text-xs">
          영어 번역이 완료되었습니다. 아래 내용으로 GitHub PR을 작성하세요.
        </div>
      )}

      {/* PR 제목 */}
      <div className="bg-github-surface border border-github-border rounded-xl p-5 mb-4">
        <div className="flex items-center justify-between mb-2">
          <p className="text-github-muted text-xs">
            PR 제목{translated && <span className="text-github-accent ml-1">(영어)</span>}
          </p>
          <button
            onClick={() => copyToClipboard(displayTitle, "title")}
            className="text-github-muted hover:text-github-accent text-xs transition-colors"
          >
            {copied === "title" ? "복사됨 ✓" : "복사"}
          </button>
        </div>
        {editing && !translated ? (
          <input
            type="text"
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            className="w-full bg-github-bg border border-github-border rounded-lg px-3 py-2 text-github-text text-sm focus:outline-none focus:border-github-purple transition-colors"
          />
        ) : (
          <p className="text-github-text font-medium">{displayTitle}</p>
        )}
      </div>

      {/* PR 본문 */}
      <div className="bg-github-surface border border-github-border rounded-xl p-5 mb-4">
        <div className="flex items-center justify-between mb-2">
          <p className="text-github-muted text-xs">
            PR 본문{translated && <span className="text-github-accent ml-1">(영어)</span>}
          </p>
          <button
            onClick={() => copyToClipboard(displayBody, "body")}
            className="text-github-muted hover:text-github-accent text-xs transition-colors"
          >
            {copied === "body" ? "복사됨 ✓" : "복사"}
          </button>
        </div>
        {editing && !translated ? (
          <textarea
            value={editBody}
            onChange={(e) => setEditBody(e.target.value)}
            rows={12}
            className="w-full bg-github-bg border border-github-border rounded-lg px-3 py-2 text-github-text text-xs font-mono focus:outline-none focus:border-github-purple transition-colors resize-none"
          />
        ) : (
          <pre className="text-github-text text-xs font-mono whitespace-pre-wrap leading-relaxed max-h-72 overflow-y-auto">
            {displayBody}
          </pre>
        )}
      </div>

      {/* Diff */}
      {pr.diffContent && (
        <div className="bg-github-surface border border-github-border rounded-xl p-5 mb-5">
          <p className="text-github-muted text-xs mb-2">코드 변경 내용 (diff)</p>
          <pre className="text-github-text text-xs font-mono whitespace-pre-wrap leading-relaxed max-h-48 overflow-y-auto">
            {pr.diffContent.length > 3000
              ? pr.diffContent.slice(0, 3000) + "\n... (생략)"
              : pr.diffContent}
          </pre>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3 flex-wrap">
        {editing && !translated ? (
          <>
            <button
              onClick={handleSave}
              disabled={saving}
              className="px-5 py-2.5 bg-github-purple hover:bg-github-purple/90 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors"
            >
              {saving ? "저장 중..." : "저장"}
            </button>
            <button
              onClick={cancelEdit}
              className="px-5 py-2.5 text-github-muted hover:text-github-text text-sm rounded-lg border border-github-border transition-colors"
            >
              취소
            </button>
          </>
        ) : !translated ? (
          <button
            onClick={handleTranslate}
            disabled={translating}
            className="flex items-center gap-2 px-5 py-2.5 bg-github-accent/10 hover:bg-github-accent/20 disabled:opacity-50 text-github-accent text-sm font-medium rounded-lg border border-github-accent/30 transition-colors"
          >
            {translating ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-github-accent border-t-transparent rounded-full animate-spin" />
                번역 중...
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
            className="flex items-center justify-center gap-2 px-6 py-2.5 bg-github-accent/10 hover:bg-github-accent/20 text-github-accent font-medium rounded-xl border border-github-accent/30 transition-colors text-sm"
          >
            GitHub에서 PR 올리기 ↗
          </a>
        )}
      </div>
    </ProtectedLayout>
  );
}
