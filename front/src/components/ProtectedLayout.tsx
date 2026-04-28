"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { auth } from "@/lib/auth";
import Navbar from "./Navbar";

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    if (!auth.isLoggedIn()) {
      router.replace("/login");
    }
  }, [router]);

  // 서버/클라이언트 초기 렌더 일치를 위해 마운트 전에는 null 반환
  if (!mounted || !auth.isLoggedIn()) return null;

  return (
    <div className="min-h-screen bg-github-bg text-github-text">
      <Navbar />
      <main className="max-w-6xl mx-auto px-4 py-8">{children}</main>
    </div>
  );
}
