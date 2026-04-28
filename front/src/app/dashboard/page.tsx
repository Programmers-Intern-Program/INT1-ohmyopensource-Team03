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

  return (
    <ProtectedLayout>
      {/* Welcome section */}
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

      {/* User Info Card */}
      {user && (
        <div className="bg-github-surface border border-github-border rounded-xl p-6 mb-6">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-github-text font-semibold mb-1">
                내 계정 정보
              </h2>
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

      {/* Feature Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <FeatureCard
          href="/issues"
          title="이슈 탐색"
          description="GitHub 오픈소스 이슈를 검색하고 나에게 맞는 이슈를 찾아보세요."
          icon="🔍"
          color="blue"
        />
        <FeatureCard
          href="/pr-draft"
          title="PR 초안 생성"
          description="선택한 이슈와 diff를 기반으로 AI가 PR 초안을 자동 생성합니다."
          icon="✨"
          color="purple"
        />
        <FeatureCard
          href="/profile"
          title="프로필 관리"
          description="이름, 이메일을 수정하고 AI 분석을 위한 벡터를 업데이트하세요."
          icon="👤"
          color="green"
        />
      </div>
    </ProtectedLayout>
  );
}

function FeatureCard({
  href,
  title,
  description,
  icon,
  color,
}: {
  href: string;
  title: string;
  description: string;
  icon: string;
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
      <div className="text-3xl mb-3">{icon}</div>
      <h3 className="text-github-text font-semibold mb-2">{title}</h3>
      <p className="text-github-muted text-sm leading-relaxed">{description}</p>
    </Link>
  );
}
