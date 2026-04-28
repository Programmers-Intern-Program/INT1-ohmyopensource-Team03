import { auth } from "./auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ─── Types ────────────────────────────────────────────────

export interface CommonResponse<T> {
  status: "success" | "fail";
  data: T | null;
  message: string | null;
}

export interface UserInfoRes {
  id: number | null;
  githubId: string;
  name: string | null;
  email: string | null;
  primaryLanguages: string[] | null;
  vectorUpdatedAt: string | null;
}

export interface RecommendIssueRes {
  id: number;
  repoFullName: string;
  issueNumber: number;
  title: string;
  summary: string;
  score: number;
  labels: string[] | null;
  status: "OPEN" | "CLOSED";
}

export interface Issue {
  id: number;
  repoFullName: string;
  issueNumber: number;
  title: string;
  content: string | null;
  labels: string[] | null;
  status: "OPEN" | "CLOSED";
  createdAt: string;
  updatedAt: string;
}

export interface CreatePrReq {
  issueId: number;
  diffContent: string;
}

export interface PrInfoRes {
  title: string;
  body: string;
  githubUrl: string;
}

// ─── Fetch Helper ────────────────────────────────────────

async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = auth.getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (!res.ok) {
    const errorBody = await res.json().catch(() => null);
    const message = errorBody?.message ?? `HTTP ${res.status}`;
    throw new Error(message);
  }

  return res.json();
}

// ─── User API ────────────────────────────────────────────

export const userApi = {
  getMe(): Promise<CommonResponse<UserInfoRes>> {
    return apiFetch("/api/v1/users/me");
  },

  getByGithubId(githubId: string): Promise<CommonResponse<UserInfoRes>> {
    return apiFetch(`/api/v1/users/${githubId}`);
  },

  updateMe(params: {
    name?: string;
    email?: string;
  }): Promise<CommonResponse<UserInfoRes>> {
    const query = new URLSearchParams();
    if (params.name !== undefined) query.set("name", params.name);
    if (params.email !== undefined) query.set("email", params.email);
    return apiFetch(`/api/v1/users/me?${query.toString()}`, {
      method: "PATCH",
    });
  },

  updateVector(): Promise<CommonResponse<UserInfoRes>> {
    return apiFetch("/api/v1/users/me/vector", { method: "POST" });
  },
};

// ─── Issue API ───────────────────────────────────────────

export const issueApi = {
  getAll(): Promise<Issue[]> {
    return apiFetch("/api/v1/issues/");
  },

  crawlSearch(q: string): Promise<RecommendIssueRes[]> {
    return apiFetch(
      `/api/v1/issues/crawl/search?q=${encodeURIComponent(q)}`,
      { method: "POST" }
    );
  },
};

// ─── PR Draft API ─────────────────────────────────────────

export const prApi = {
  create(req: CreatePrReq): Promise<CommonResponse<PrInfoRes>> {
    return apiFetch("/api/v1/pr/", {
      method: "POST",
      body: JSON.stringify(req),
    });
  },
};
