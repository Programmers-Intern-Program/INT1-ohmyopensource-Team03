"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { auth } from "@/lib/auth";

const BACKEND_OAUTH_URL =
  process.env.NEXT_PUBLIC_BACKEND_OAUTH_URL ||
  "http://localhost:8080/oauth2/authorization/github";

export default function LoginPage() {
  const router = useRouter();

  useEffect(() => {
    if (auth.isLoggedIn()) router.replace("/dashboard");
  }, [router]);

  return (
    <div className="min-h-screen bg-github-bg flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-github-purple-light mb-2">
            OMOS
          </h1>
          <p className="text-github-muted text-sm">Oh My OpenSource</p>
        </div>

        {/* Card */}
        <div className="bg-github-surface border border-github-border rounded-xl p-8 shadow-xl">
          <h2 className="text-github-text text-xl font-semibold text-center mb-2">
            로그인
          </h2>
          <p className="text-github-muted text-sm text-center mb-8">
            GitHub 계정으로 로그인하여 오픈소스 이슈를 탐색하세요
          </p>

          <a
            href={BACKEND_OAUTH_URL}
            className="flex items-center justify-center gap-3 w-full bg-github-purple hover:bg-github-purple/90 text-white font-medium py-3 px-4 rounded-lg transition-colors"
          >
            <GitHubIcon />
            GitHub으로 로그인
          </a>

          <p className="text-github-muted text-xs text-center mt-6">
            로그인하면 GitHub 프로필 정보를 사용에 동의하는 것으로 간주합니다.
          </p>
        </div>

        <p className="text-github-muted text-xs text-center mt-4">
          백엔드 서버:{" "}
          <span className="text-github-accent">localhost:8080</span>
        </p>
      </div>
    </div>
  );
}

function GitHubIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0 1 12 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z" />
    </svg>
  );
}
