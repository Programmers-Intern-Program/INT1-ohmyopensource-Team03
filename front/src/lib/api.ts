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
  status: string;
}

export interface RecommendIssueHistoryRes {
  id: number;
  repoFullName: string;
  issueNumber: number;
  title: string;
  summary: string;
  score: number;
  labels: string[] | null;
  status: string;
  isAnalyzed: boolean;
  analysisResultId: number | null;
}

export interface GuideResponseDto {
  filePaths: string[];
  guideline: string;
  sideEffects: string;
}

export interface PseudoCodeResponseDto {
  filePaths: string[];
  pseudoCode: string;
}

export interface CreatePrReq {
  upstreamRepo: string;
  githubIssueNumber: number;
  baseBranch: string;
  headBranch: string;
}

export interface UpdatePrReq {
  title?: string;
  body?: string;
}

export interface PrInfoRes {
  id: number;
  title: string;
  body: string;
}

export interface PrDetailRes {
  id: number;
  repoFullName: string;
  issueTitle: string;
  title: string;
  body: string;
  diffContent: string;
  createdAt: string;
}

export interface PrHistoryRes {
  id: number;
  repoFullName: string;
  issueTitle: string;
  title: string;
  createdAt: string;
}

export interface PrPageRes<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface PrTranslateRes {
  titleEn: string;
  bodyEn: string;
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
  crawlSearch(q: string): Promise<CommonResponse<RecommendIssueRes[]>> {
    return apiFetch(`/api/v1/issues/crawl/search?q=${encodeURIComponent(q)}`, {
      method: "POST",
    });
  },

  recommend(): Promise<CommonResponse<RecommendIssueRes[]>> {
    return apiFetch("/api/v1/issues/recommend");
  },

  getHistory(): Promise<CommonResponse<RecommendIssueHistoryRes[]>> {
    return apiFetch("/api/v1/issues/recommend/history");
  },

  getGuide(issueId: number): Promise<CommonResponse<GuideResponseDto>> {
    return apiFetch(`/api/v1/issues/${issueId}/guide`);
  },

  getPseudoCode(issueId: number): Promise<CommonResponse<PseudoCodeResponseDto>> {
    return apiFetch(`/api/v1/issues/${issueId}/pseudo`);
  },
};

// ─── PR Draft API ─────────────────────────────────────────

export const prApi = {
  create(req: CreatePrReq): Promise<CommonResponse<PrInfoRes>> {
    return apiFetch("/api/v1/pr", {
      method: "POST",
      body: JSON.stringify(req),
    });
  },

  getOne(id: number): Promise<CommonResponse<PrDetailRes>> {
    return apiFetch(`/api/v1/pr/${id}`);
  },

  getHistory(page = 0, size = 5): Promise<CommonResponse<PrPageRes<PrHistoryRes>>> {
    return apiFetch(`/api/v1/pr/history?page=${page}&size=${size}`);
  },

  update(id: number, req: UpdatePrReq): Promise<CommonResponse<PrDetailRes>> {
    return apiFetch(`/api/v1/pr/${id}`, {
      method: "PATCH",
      body: JSON.stringify(req),
    });
  },

  translate(id: number): Promise<CommonResponse<PrTranslateRes>> {
    return apiFetch(`/api/v1/pr/${id}/translate`, { method: "POST" });
  },

  delete(id: number): Promise<CommonResponse<null>> {
    return apiFetch(`/api/v1/pr/${id}`, { method: "DELETE" });
  },
};
