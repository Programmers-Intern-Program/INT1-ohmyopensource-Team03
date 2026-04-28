"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { auth } from "@/lib/auth";

const navLinks = [
  { href: "/dashboard", label: "대시보드" },
  { href: "/issues", label: "이슈 탐색" },
  { href: "/pr-draft", label: "PR 초안" },
  { href: "/profile", label: "내 프로필" },
];

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();

  function handleLogout() {
    auth.removeToken();
    router.push("/login");
  }

  return (
    <nav className="border-b border-github-border bg-github-surface sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 flex items-center justify-between h-14">
        <Link
          href="/dashboard"
          className="text-github-purple-light font-bold text-lg tracking-tight hover:opacity-80 transition-opacity"
        >
          OMOS
        </Link>

        <div className="flex items-center gap-1">
          {navLinks.map(({ href, label }) => (
            <Link
              key={href}
              href={href}
              className={`px-3 py-1.5 rounded-md text-sm transition-colors ${
                pathname.startsWith(href)
                  ? "bg-github-purple/20 text-github-purple-light"
                  : "text-github-muted hover:text-github-text hover:bg-white/5"
              }`}
            >
              {label}
            </Link>
          ))}

          <button
            onClick={handleLogout}
            className="ml-3 px-3 py-1.5 rounded-md text-sm text-github-muted hover:text-red-400 hover:bg-red-500/10 transition-colors"
          >
            로그아웃
          </button>
        </div>
      </div>
    </nav>
  );
}
