const API_BASE = 'http://localhost:8080';

// app.oauth2.redirect-uri 설정값과 반드시 일치해야 합니다
const REDIRECT_URI_PREFIX = 'http://localhost:3000/oauth/callback';

async function getToken() {
  const { token } = await chrome.storage.local.get('token');
  return token || null;
}

async function setToken(token) {
  await chrome.storage.local.set({ token });
}

async function clearToken() {
  await chrome.storage.local.remove('token');
}

// OAuth 로그인: 새 탭을 열고 리다이렉트에서 JWT를 캡처합니다
async function startLogin() {
  const tab = await chrome.tabs.create({ url: `${API_BASE}/oauth2/authorization/github` });

  return new Promise((resolve) => {
    function listener(tabId, changeInfo, tabInfo) {
      if (tabId !== tab.id || changeInfo.status !== 'complete') return;

      const url = tabInfo.url || '';
      if (!url.startsWith(REDIRECT_URI_PREFIX)) return;

      const tokenParam = new URL(url).searchParams.get('token');
      if (tokenParam) {
        setToken(tokenParam).then(() => {
          chrome.tabs.remove(tabId);
          chrome.tabs.onUpdated.removeListener(listener);
          notifyGitHubTabs({ type: 'AUTH_SUCCESS' });
          resolve();
        });
      }
    }

    chrome.tabs.onUpdated.addListener(listener);
  });
}

async function notifyGitHubTabs(message) {
  const tabs = await chrome.tabs.query({ url: 'https://github.com/*' });
  tabs.forEach((tab) =>
    chrome.tabs.sendMessage(tab.id, message).catch(() => {})
  );
}

// API 호출 공통 헬퍼
async function apiFetch(path, options = {}) {
  const token = await getToken();
  if (!token) throw new Error('NOT_AUTHENTICATED');

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });

  if (res.status === 401) {
    await clearToken();
    throw new Error('TOKEN_EXPIRED');
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body?.message || `HTTP_${res.status}`);
  }

  return res.json();
}

// GitHub repoFullName + issueNumber → OMOS DB 내부 ID 조회
async function lookupOmosIssueId(repoFullName, issueNumber) {
  const data = await apiFetch(
    `/api/v1/issues/lookup?repo=${encodeURIComponent(repoFullName)}&number=${issueNumber}`
  );
  const id = data?.data?.id;
  if (!id) throw new Error('ISSUE_NOT_IN_DB');
  return id;
}

// content.js / popup.js 메시지 라우팅
chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  switch (message.type) {
    case 'LOGIN':
      startLogin()
        .then(() => sendResponse({ ok: true }))
        .catch((e) => sendResponse({ ok: false, error: e.message }));
      return true;

    case 'LOGOUT':
      clearToken().then(() => sendResponse({ ok: true }));
      return true;

    case 'GET_AUTH_STATUS':
      getToken().then((token) => sendResponse({ isLoggedIn: !!token }));
      return true;

    // 새 추천 생성 (AI 호출, 느림) → 결과를 storage에 캐싱
    case 'GET_RECOMMEND_ISSUES':
      apiFetch('/api/v1/issues/recommend')
        .then((data) => {
          chrome.storage.local.set({ cachedRecommendations: data });
          sendResponse({ ok: true, data });
        })
        .catch((e) => sendResponse({ ok: false, error: e.message }));
      return true;

    // 추천 이력 조회 (DB 조회, 빠름) → popup에서 사용
    case 'GET_RECOMMEND_HISTORY':
      apiFetch('/api/v1/issues/recommend/history')
        .then((data) => {
          chrome.storage.local.set({ cachedRecommendations: data });
          sendResponse({ ok: true, data });
        })
        .catch((e) => sendResponse({ ok: false, error: e.message }));
      return true;

    case 'GET_CACHED_RECOMMENDATIONS':
      chrome.storage.local.get('cachedRecommendations').then(({ cachedRecommendations }) => {
        sendResponse({ ok: true, data: cachedRecommendations ?? null });
      });
      return true;

    case 'GET_ISSUE_GUIDE':
      lookupOmosIssueId(message.repoFullName, message.issueNumber)
        .then((id) => apiFetch(`/api/v1/issues/${id}/guide`))
        .then((data) => sendResponse({ ok: true, data }))
        .catch((e) => sendResponse({ ok: false, error: e.message }));
      return true;

    case 'GET_ISSUE_PSEUDO':
      lookupOmosIssueId(message.repoFullName, message.issueNumber)
        .then((id) => apiFetch(`/api/v1/issues/${id}/pseudo`))
        .then((data) => sendResponse({ ok: true, data }))
        .catch((e) => sendResponse({ ok: false, error: e.message }));
      return true;

    case 'CREATE_PR_DRAFT':
      apiFetch('/api/v1/pr', {
        method: 'POST',
        body: JSON.stringify(message.payload),
      })
        .then((data) => sendResponse({ ok: true, data }))
        .catch((e) => sendResponse({ ok: false, error: e.message }));
      return true;
  }
});
