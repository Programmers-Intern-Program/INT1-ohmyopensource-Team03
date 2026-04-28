"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { auth } from "@/lib/auth";

export default function RootPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace(auth.isLoggedIn() ? "/dashboard" : "/login");
  }, [router]);

  // 서버/클라이언트 렌더 일치를 위해 항상 null 반환 (리다이렉트는 useEffect에서)
  return null;
}
