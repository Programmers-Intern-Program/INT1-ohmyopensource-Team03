"use client";

import { useEffect, useState } from "react";
import ProtectedLayout from "@/components/ProtectedLayout";
import { userApi, UserInfoRes } from "@/lib/api";

export default function ProfilePage() {
  const [user, setUser] = useState<UserInfoRes | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [vectorUpdating, setVectorUpdating] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  useEffect(() => {
    userApi
      .getMe()
      .then((res) => {
        if (res.status === "success" && res.data) {
          setUser(res.data);
          setName(res.data.name ?? "");
          setEmail(res.data.email ?? "");
        }
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const res = await userApi.updateMe({ name, email });
      if (res.status === "success" && res.data) {
        setUser(res.data);
        setMessage({ type: "success", text: "프로필이 업데이트되었습니다." });
      } else {
        setMessage({ type: "error", text: res.message ?? "업데이트 실패" });
      }
    } catch (e) {
      setMessage({ type: "error", text: (e as Error).message });
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdateVector() {
    setVectorUpdating(true);
    setMessage(null);
    try {
      const res = await userApi.updateVector();
      if (res.status === "success" && res.data) {
        setUser(res.data);
        setMessage({
          type: "success",
          text: "벡터 업데이트가 완료되었습니다. GitHub 활동이 분석되었어요.",
        });
      } else {
        setMessage({ type: "error", text: res.message ?? "벡터 업데이트 실패" });
      }
    } catch (e) {
      setMessage({ type: "error", text: (e as Error).message });
    } finally {
      setVectorUpdating(false);
    }
  }

  if (loading) {
    return (
      <ProtectedLayout>
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <div
              key={i}
              className="h-16 bg-github-surface rounded-xl animate-pulse"
            />
          ))}
        </div>
      </ProtectedLayout>
    );
  }

  return (
    <ProtectedLayout>
      <div className="max-w-2xl">
        <h1 className="text-2xl font-bold text-github-text mb-6">내 프로필</h1>

        {message && (
          <div
            className={`mb-6 p-4 rounded-lg text-sm border ${
              message.type === "success"
                ? "bg-green-500/10 border-green-500/30 text-green-400"
                : "bg-red-500/10 border-red-500/30 text-red-400"
            }`}
          >
            {message.text}
          </div>
        )}

        {/* Profile Info */}
        <div className="bg-github-surface border border-github-border rounded-xl p-6 mb-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-12 h-12 rounded-full bg-github-purple/30 flex items-center justify-center text-github-purple-light font-bold text-lg">
              {(user?.name ?? user?.githubId ?? "?")[0]?.toUpperCase()}
            </div>
            <div>
              <p className="text-github-text font-medium">{user?.githubId}</p>
              <p className="text-github-muted text-xs">GitHub ID</p>
            </div>
          </div>

          <form onSubmit={handleSave} className="space-y-4">
            <div>
              <label className="block text-github-muted text-xs mb-1.5">
                이름
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="이름을 입력하세요"
                className="w-full bg-github-bg border border-github-border rounded-lg px-3 py-2 text-github-text text-sm focus:outline-none focus:border-github-purple transition-colors"
              />
            </div>

            <div>
              <label className="block text-github-muted text-xs mb-1.5">
                이메일
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="이메일을 입력하세요"
                className="w-full bg-github-bg border border-github-border rounded-lg px-3 py-2 text-github-text text-sm focus:outline-none focus:border-github-purple transition-colors"
              />
            </div>

            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 bg-github-purple hover:bg-github-purple/90 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors"
            >
              {saving ? "저장 중..." : "프로필 저장"}
            </button>
          </form>
        </div>

        {/* Vector Section */}
        <div className="bg-github-surface border border-github-border rounded-xl p-6">
          <h2 className="text-github-text font-semibold mb-1">AI 프로필 벡터</h2>
          <p className="text-github-muted text-sm mb-4">
            GitHub 활동을 분석하여 나에게 맞는 이슈를 추천받기 위한 임베딩
            벡터를 생성합니다.
          </p>

          {user?.vectorUpdatedAt && (
            <p className="text-github-muted text-xs mb-4">
              마지막 업데이트:{" "}
              <span className="text-github-accent">
                {new Date(user.vectorUpdatedAt).toLocaleString("ko-KR")}
              </span>
            </p>
          )}

          {user?.primaryLanguages && user.primaryLanguages.length > 0 && (
            <div className="mb-4">
              <p className="text-github-muted text-xs mb-2">감지된 주요 언어</p>
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

          <button
            onClick={handleUpdateVector}
            disabled={vectorUpdating}
            className="flex items-center gap-2 px-4 py-2 bg-github-accent/10 hover:bg-github-accent/20 disabled:opacity-50 text-github-accent text-sm font-medium rounded-lg border border-github-accent/30 transition-colors"
          >
            {vectorUpdating ? (
              <>
                <div className="w-4 h-4 border-2 border-github-accent border-t-transparent rounded-full animate-spin" />
                분석 중... (시간이 걸릴 수 있습니다)
              </>
            ) : (
              "벡터 업데이트"
            )}
          </button>
        </div>
      </div>
    </ProtectedLayout>
  );
}
