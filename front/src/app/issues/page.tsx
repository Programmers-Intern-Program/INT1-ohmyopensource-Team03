"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import ProtectedLayout from "@/components/ProtectedLayout";
import { userApi, issueApi, RecommendIssueHistoryRes } from "@/lib/api";

export default function IssuesPage() {
  const router = useRouter();
  const [history, setHistory] = useState<RecommendIssueHistoryRes[]>([]);
  const [loading, setLoading] = useState(true);
  const [recommending, setRecommending] = useState(false);
  const [recommendStep, setRecommendStep] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadHistory();
  }, []);

  async function loadHistory() {
    setLoading(true);
    try {
      const res = await issueApi.getHistory();
      if (res.status === "success" && res.data) setHistory(res.data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function handleRecommend() {
    setRecommending(true);
    setError(null);
    setRecommendStep(null);
    try {
      setRecommendStep("사용자 정보 확인 중...");
      const userRes = await userApi.getMe();
      if (userRes.status !== "success" || !userRes.data) {
        throw new Error("사용자 정보를 불러올 수 없습니다.");
      }

      const langs = userRes.data.primaryLanguages;
      if (!langs || langs.length === 0) {
        throw new Error(
          "프로필 분석이 필요합니다. 마이페이지에서 '벡터 업데이트'를 먼저 실행해주세요."
        );
      }

      setRecommendStep("GitHub에서 이슈 수집 중...");
      await Promise.all(
        langs.slice(0, 3).map((lang) =>
          issueApi.crawlSearch(
            `language:${lang.toLowerCase()} label:"good first issue" state:open`
          )
        )
      );

      setRecommendStep("나에게 맞는 이슈 분석 중...");
      await issueApi.recommend();

      setRecommendStep(null);
      await loadHistory();
    } catch (e) {
      setError((e as Error).message);
      setRecommendStep(null);
    } finally {
      setRecommending(false);
    }
  }

  return (
    <ProtectedLayout>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-github-text mb-1">추천 이슈</h1>
          <p className="text-github-muted text-sm">
            나의 기술 스택과 GitHub 활동 기반으로 맞춤 오픈소스 이슈를 추천합니다.
          </p>
        </div>
        <button
          onClick={handleRecommend}
          disabled={recommending}
          className="shrink-0 flex items-center gap-2 px-5 py-2.5 bg-github-purple hover:bg-github-purple/90 disabled:opacity-60 text-white text-sm font-medium rounded-lg transition-colors"
        >
          {recommending ? (
            <>
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              {recommendStep ?? "분석 중..."}
            </>
          ) : (
            "이슈 추천받기"
          )}
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="space-y-3">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-28 bg-github-surface rounded-xl animate-pulse" />
          ))}
        </div>
      ) : history.length === 0 ? (
        <div className="text-center py-24 text-github-muted">
          <p className="text-5xl mb-4">🔍</p>
          <p className="mb-1">아직 추천받은 이슈가 없습니다.</p>
          <p className="text-xs mt-1">
            프로필 분석 완료 후 "이슈 추천받기" 버튼을 눌러보세요.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {history.map((item) => (
            <IssueHistoryCard
              key={item.id}
              item={item}
              onClick={() => router.push(`/issues/${item.id}`)}
            />
          ))}
        </div>
      )}
    </ProtectedLayout>
  );
}

function IssueHistoryCard({
  item,
  onClick,
}: {
  item: RecommendIssueHistoryRes;
  onClick: () => void;
}) {
  const scorePercent = Math.round(item.score * 100);

  return (
    <button
      onClick={onClick}
      className="w-full text-left bg-github-surface border border-github-border rounded-xl p-5 hover:border-github-purple/50 transition-colors"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2 flex-wrap">
            <StatusBadge status={item.status} />
            <span className="text-github-muted text-xs">
              {item.repoFullName} #{item.issueNumber}
            </span>
            {scorePercent > 0 && (
              <span className="text-xs px-2 py-0.5 bg-green-500/10 text-green-400 rounded-full border border-green-500/20">
                매칭 {scorePercent}%
              </span>
            )}
            {item.isAnalyzed && (
              <span className="text-xs px-2 py-0.5 bg-github-accent/10 text-github-accent rounded-full border border-github-accent/20">
                분석 완료
              </span>
            )}
          </div>
          <h3 className="text-github-text font-medium mb-2 line-clamp-1">
            {item.title}
          </h3>
          {item.summary && (
            <p className="text-github-muted text-xs line-clamp-2">{item.summary}</p>
          )}
          {item.labels && item.labels.length > 0 && (
            <div className="flex gap-1.5 mt-2 flex-wrap">
              {item.labels.map((label) => (
                <span
                  key={label}
                  className="px-2 py-0.5 bg-github-accent/10 text-github-accent text-xs rounded-full border border-github-accent/20"
                >
                  {label}
                </span>
              ))}
            </div>
          )}
        </div>
        <span className="text-github-muted text-xs shrink-0 mt-1">자세히 →</span>
      </div>
    </button>
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
