"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import ProtectedLayout from "@/components/ProtectedLayout";
import { userApi, UserInfoRes } from "@/lib/api";

export default function DashboardPage() {
  const [user, setUser] = useState<UserInfoRes | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    userApi
      .getMe()
      .then((res) => {
        if (res.status === "success") setUser(res.data);
        else setError(res.message ?? "사용자 정보를 불러오지 못했습니다.");
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const hasVector = !!user?.vectorUpdatedAt;

  return (
    <ProtectedLayout>
      <div className="mb-8">
        {loading ? (
          <div className="h-8 w-48 bg-github-surface rounded animate-pulse" />
        ) : (
          <h1 className="text-2xl font-bold text-github-text">
            안녕하세요, {user?.name ?? user?.githubId ?? ""}님
          </h1>
        )}
        <p className="text-github-muted mt-1 text-sm">
          OMOS — Oh My OpenSource 대시보드
        </p>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {/* 프로필 분석 안내 배너 */}
      {!loading && !hasVector && (
        <div className="mb-6 p-4 bg-yellow-500/10 border border-yellow-500/30 rounded-xl flex items-center justify-between gap-4">
          <div>
            <p className="text-yellow-400 text-sm font-medium">프로필 분석이 필요합니다</p>
            <p className="text-yellow-400/70 text-xs mt-0.5">
              이슈 추천을 받으려면 먼저 마이페이지에서 벡터 업데이트를 완료해주세요.
            </p>
          </div>
          <Link
            href="/profile"
            className="shrink-0 px-4 py-2 bg-yellow-500/20 hover:bg-yellow-500/30 text-yellow-400 text-xs font-medium rounded-lg transition-colors"
          >
            프로필로 이동
          </Link>
        </div>
      )}

      {/* 사용자 정보 */}
      {user && (
        <div className="bg-github-surface border border-github-border rounded-xl p-6 mb-6">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-github-text font-semibold mb-1">내 계정 정보</h2>
              <p className="text-github-muted text-sm">GitHub ID: {user.githubId}</p>
              {user.email && (
                <p className="text-github-muted text-sm">{user.email}</p>
              )}
            </div>
            <Link
              href="/profile"
              className="text-github-accent hover:underline text-sm"
            >
              프로필 편집
            </Link>
          </div>

          {user.primaryLanguages && user.primaryLanguages.length > 0 && (
            <div className="mt-4">
              <p className="text-github-muted text-xs mb-2">주요 언어</p>
              <div className="flex gap-2 flex-wrap">
                {user.primaryLanguages.map((lang) => (
                  <span
                    key={lang}
                    className="px-2 py-0.5 bg-github-purple/20 text-github-purple-light text-xs rounded-full border border-github-purple/30"
                  >
                    {lang}
                  </span>
                ))}
              </div>
            </div>
          )}

          {user.vectorUpdatedAt && (
            <p className="text-github-muted text-xs mt-3">
              벡터 업데이트:{" "}
              {new Date(user.vectorUpdatedAt).toLocaleString("ko-KR")}
            </p>
          )}
        </div>
      )}

      {/* 서비스 플로우 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <StepCard
          step={1}
          href="/profile"
          title="프로필 분석"
          description="GitHub 활동을 분석하여 AI 추천을 위한 벡터를 생성합니다."
          done={hasVector}
          color="green"
        />
        <StepCard
          step={2}
          href="/issues"
          title="이슈 추천"
          description="나의 기술 스택에 맞는 오픈소스 이슈를 추천받고 분석 힌트를 확인합니다."
          done={false}
          color="blue"
        />
        <StepCard
          step={3}
          href="/pr-draft"
          title="PR 초안 생성"
          description="코드 작업 후 브랜치 정보를 입력하면 AI가 PR을 자동 작성합니다."
          done={false}
          color="purple"
        />
      </div>
    </ProtectedLayout>
  );
}

function StepCard({
  step,
  href,
  title,
  description,
  done,
  color,
}: {
  step: number;
  href: string;
  title: string;
  description: string;
  done: boolean;
  color: "blue" | "purple" | "green";
}) {
  const colorMap = {
    blue: "border-github-accent/30 hover:border-github-accent/60 hover:bg-github-accent/5",
    purple:
      "border-github-purple/30 hover:border-github-purple/60 hover:bg-github-purple/5",
    green:
      "border-green-500/30 hover:border-green-500/60 hover:bg-green-500/5",
  };

  return (
    <Link
      href={href}
      className={`bg-github-surface border rounded-xl p-6 transition-colors block ${colorMap[color]}`}
    >
      <div className="flex items-center gap-2 mb-3">
        <span className="text-xs text-github-muted font-medium">STEP {step}</span>
        {done && (
          <span className="text-xs px-1.5 py-0.5 bg-green-500/20 text-green-400 rounded-full">
            완료
          </span>
        )}
      </div>
      <h3 className="text-github-text font-semibold mb-2">{title}</h3>
      <p className="text-github-muted text-sm leading-relaxed">{description}</p>
    </Link>
  );
}
