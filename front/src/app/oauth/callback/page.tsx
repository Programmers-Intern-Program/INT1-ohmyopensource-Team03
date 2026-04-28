"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { auth } from "@/lib/auth";
import { Suspense } from "react";

function OAuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get("token");

    if (token) {
      auth.setToken(token);
      router.replace("/dashboard");
    } else {
      setError(
        "토큰을 받지 못했습니다. 백엔드 로그를 확인하거나 다시 로그인해 주세요."
      );
    }
  }, [searchParams, router]);

  if (error) {
    return (
      <div className="min-h-screen bg-github-bg flex items-center justify-center px-4">
        <div className="bg-github-surface border border-red-500/40 rounded-xl p-8 max-w-md w-full text-center">
          <div className="text-red-400 text-4xl mb-4">!</div>
          <h2 className="text-github-text font-semibold text-lg mb-2">
            로그인 실패
          </h2>
          <p className="text-github-muted text-sm mb-6">{error}</p>
          <a
            href="/login"
            className="inline-block px-4 py-2 bg-github-purple hover:bg-github-purple/90 text-white rounded-lg text-sm transition-colors"
          >
            다시 로그인
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-github-bg flex items-center justify-center">
      <div className="text-center">
        <div className="w-10 h-10 border-2 border-github-purple-light border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-github-muted text-sm">로그인 처리 중...</p>
      </div>
    </div>
  );
}

export default function OAuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-github-bg flex items-center justify-center">
          <div className="w-10 h-10 border-2 border-github-purple-light border-t-transparent rounded-full animate-spin" />
        </div>
      }
    >
      <OAuthCallbackContent />
    </Suspense>
  );
}
