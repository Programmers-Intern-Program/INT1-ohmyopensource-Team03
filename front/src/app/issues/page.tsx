"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import ProtectedLayout from "@/components/ProtectedLayout";
import { issueApi, Issue, RecommendIssueRes } from "@/lib/api";

type ViewMode = "all" | "search";

export default function IssuesPage() {
  const [viewMode, setViewMode] = useState<ViewMode>("all");
  const [allIssues, setAllIssues] = useState<Issue[]>([]);
  const [searchResults, setSearchResults] = useState<RecommendIssueRes[]>([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [query, setQuery] = useState("language:kotlin state:open label:\"good first issue\"");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    issueApi
      .getAll()
      .then(setAllIssues)
      .catch((e) => setError(e.message))
      .finally(() => setInitialLoading(false));
  }, []);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    setViewMode("search");
    try {
      const results = await issueApi.crawlSearch(query);
      setSearchResults(results);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <ProtectedLayout>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-github-text mb-1">이슈 탐색</h1>
        <p className="text-github-muted text-sm">
          GitHub 오픈소스 이슈를 검색하고 PR 초안을 생성해보세요.
        </p>
      </div>

      {/* Search Bar */}
      <form onSubmit={handleSearch} className="mb-6">
        <div className="flex gap-2">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="GitHub 검색 쿼리 (예: language:kotlin state:open label:&quot;good first issue&quot;)"
            className="flex-1 bg-github-surface border border-github-border rounded-lg px-4 py-2.5 text-github-text text-sm placeholder:text-github-muted focus:outline-none focus:border-github-purple transition-colors"
          />
          <button
            type="submit"
            disabled={loading}
            className="px-5 py-2.5 bg-github-purple hover:bg-github-purple/90 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
          >
            {loading ? "검색 중..." : "GitHub 검색"}
          </button>
        </div>
        <p className="text-github-muted text-xs mt-2">
          검색하면 GitHub에서 이슈를 크롤링하고 벡터 DB에 저장합니다.
        </p>
      </form>

      {error && (
        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {/* Tab */}
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => setViewMode("all")}
          className={`px-3 py-1.5 rounded-md text-sm transition-colors ${
            viewMode === "all"
              ? "bg-github-purple/20 text-github-purple-light"
              : "text-github-muted hover:text-github-text hover:bg-white/5"
          }`}
        >
          전체 이슈 ({allIssues.length})
        </button>
        <button
          onClick={() => setViewMode("search")}
          className={`px-3 py-1.5 rounded-md text-sm transition-colors ${
            viewMode === "search"
              ? "bg-github-purple/20 text-github-purple-light"
              : "text-github-muted hover:text-github-text hover:bg-white/5"
          }`}
        >
          검색 결과 ({searchResults.length})
        </button>
      </div>

      {/* Issue List */}
      {viewMode === "all" ? (
        <AllIssuesList issues={allIssues} loading={initialLoading} />
      ) : (
        <SearchResultsList results={searchResults} loading={loading} />
      )}
    </ProtectedLayout>
  );
}

function AllIssuesList({
  issues,
  loading,
}: {
  issues: Issue[];
  loading: boolean;
}) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="h-24 bg-github-surface rounded-xl animate-pulse"
          />
        ))}
      </div>
    );
  }

  if (issues.length === 0) {
    return (
      <div className="text-center py-16 text-github-muted">
        <p className="text-4xl mb-3">📭</p>
        <p>이슈가 없습니다. GitHub 검색으로 이슈를 추가해보세요.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {issues.map((issue) => (
        <IssueCard key={issue.id} issue={issue} />
      ))}
    </div>
  );
}

function SearchResultsList({
  results,
  loading,
}: {
  results: RecommendIssueRes[];
  loading: boolean;
}) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="h-24 bg-github-surface rounded-xl animate-pulse"
          />
        ))}
      </div>
    );
  }

  if (results.length === 0) {
    return (
      <div className="text-center py-16 text-github-muted">
        <p className="text-4xl mb-3">🔍</p>
        <p>위 검색창에서 GitHub 이슈를 검색해보세요.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {results.map((result) => (
        <RecommendIssueCard key={result.id} result={result} />
      ))}
    </div>
  );
}

function IssueCard({ issue }: { issue: Issue }) {
  const githubUrl = `https://github.com/${issue.repoFullName}/issues/${issue.issueNumber}`;

  return (
    <div className="bg-github-surface border border-github-border rounded-xl p-5 hover:border-github-purple/50 transition-colors">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <StatusBadge status={issue.status} />
            <span className="text-github-muted text-xs">{issue.repoFullName} #{issue.issueNumber}</span>
          </div>
          <h3 className="text-github-text font-medium truncate mb-1">
            {issue.title}
          </h3>
          {issue.content && (
            <p className="text-github-muted text-xs line-clamp-2">
              {issue.content.slice(0, 150)}
            </p>
          )}
          {issue.labels && issue.labels.length > 0 && (
            <div className="flex gap-1.5 mt-2 flex-wrap">
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
        </div>
        <div className="flex flex-col gap-2 shrink-0">
          <a
            href={githubUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-github-muted hover:text-github-accent text-xs transition-colors"
          >
            GitHub ↗
          </a>
          <Link
            href={`/pr-draft?issueId=${issue.id}&repo=${encodeURIComponent(issue.repoFullName)}&title=${encodeURIComponent(issue.title)}`}
            className="px-3 py-1.5 bg-github-purple/20 hover:bg-github-purple/40 text-github-purple-light text-xs font-medium rounded-lg transition-colors whitespace-nowrap"
          >
            PR 초안 생성
          </Link>
        </div>
      </div>
    </div>
  );
}

function RecommendIssueCard({ result }: { result: RecommendIssueRes }) {
  const githubUrl = `https://github.com/${result.repoFullName}/issues/${result.issueNumber}`;
  const scorePercent = Math.round(result.score * 100);

  return (
    <div className="bg-github-surface border border-github-border rounded-xl p-5 hover:border-github-purple/50 transition-colors">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <StatusBadge status={result.status} />
            <span className="text-github-muted text-xs">
              {result.repoFullName} #{result.issueNumber}
            </span>
            <span className="ml-auto text-xs px-2 py-0.5 bg-green-500/10 text-green-400 rounded-full border border-green-500/20">
              유사도 {scorePercent}%
            </span>
          </div>
          <h3 className="text-github-text font-medium truncate mb-1">
            {result.title}
          </h3>
          {result.summary && (
            <p className="text-github-muted text-xs line-clamp-2">
              {result.summary}
            </p>
          )}
          {result.labels && result.labels.length > 0 && (
            <div className="flex gap-1.5 mt-2 flex-wrap">
              {result.labels.map((label) => (
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
        <div className="flex flex-col gap-2 shrink-0">
          <a
            href={githubUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-github-muted hover:text-github-accent text-xs transition-colors"
          >
            GitHub ↗
          </a>
          <Link
            href={`/pr-draft?issueId=${result.id}&repo=${encodeURIComponent(result.repoFullName)}&title=${encodeURIComponent(result.title)}`}
            className="px-3 py-1.5 bg-github-purple/20 hover:bg-github-purple/40 text-github-purple-light text-xs font-medium rounded-lg transition-colors whitespace-nowrap"
          >
            PR 초안 생성
          </Link>
        </div>
      </div>
    </div>
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
