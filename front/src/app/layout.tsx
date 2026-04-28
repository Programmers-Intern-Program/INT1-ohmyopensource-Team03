import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "OMOS — Oh My OpenSource",
  description: "오픈소스 이슈를 탐색하고 PR 초안을 AI로 생성하세요",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
