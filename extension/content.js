// Extension 새로고침 후 context가 무효화되면 sendMessage가 에러를 던집니다.
// 모든 메시지 전송을 이 헬퍼로 감싸서 일관된 에러 처리를 합니다.
function sendMessage(message, callback) {
  try {
    chrome.runtime.sendMessage(message, callback);
  } catch (e) {
    if (e.message?.includes('Extension context invalidated')) {
      callback({ ok: false, error: 'CONTEXT_INVALIDATED' });
    } else {
      callback({ ok: false, error: e.message });
    }
  }
}

// GitHub 현재 페이지 타입 감지
const GITHUB_SYSTEM_PATHS = new Set([
  'explore', 'trending', 'notifications', 'settings', 'orgs',
  'marketplace', 'issues', 'pulls', 'sponsors', 'login', 'join',
  'new', 'codespaces', 'copilot', 'features', 'about', 'contact',
  'security', 'pricing', 'blog', 'enterprise', 'topics', 'collections',
  'events', 'education', 'stars', 'watching', 'dashboard',
]);

function detectPageType() {
  const path = location.pathname;
  if (/^\/[^/]+\/[^/]+\/issues$/.test(path)) return 'ISSUE_LIST';
  if (/^\/[^/]+\/[^/]+\/issues\/\d+$/.test(path)) return 'ISSUE_DETAIL';
  if (/^\/[^/]+\/[^/]+\/compare/.test(path)) return 'PR_CREATE';
  const profileMatch = path.match(/^\/([^/]+)$/);
  if (profileMatch && !GITHUB_SYSTEM_PATHS.has(profileMatch[1].toLowerCase())) return 'PROFILE';
  return null;
}

function createSidebar() {
  const sidebar = document.createElement('div');
  sidebar.id = 'omos-sidebar';
  sidebar.classList.add('omos-hidden');
  sidebar.innerHTML = `
    <div id="omos-header">
      <span>✦ OMOS</span>
      <button id="omos-close">✕</button>
    </div>
    <div id="omos-content">
      <p class="omos-loading">불러오는 중...</p>
    </div>
  `;
  document.body.appendChild(sidebar);

  document.getElementById('omos-close').addEventListener('click', () => {
    sidebar.classList.add('omos-hidden');
    const btn = document.getElementById('omos-toggle');
    if (btn) btn.textContent = '✦';
  });

  return sidebar;
}

function createToggleButton(sidebar) {
  const btn = document.createElement('button');
  btn.id = 'omos-toggle';
  btn.textContent = '✦';
  btn.title = 'OMOS 열기';
  btn.addEventListener('click', () => {
    const isHidden = sidebar.classList.contains('omos-hidden');
    sidebar.classList.toggle('omos-hidden');
    btn.textContent = isHidden ? '✕' : '✦';
    if (isHidden) loadContent();
  });
  document.body.appendChild(btn);
  return btn;
}

// 페이지 타입에 따라 사이드바 콘텐츠를 로드합니다
function loadContent() {
  const pageType = detectPageType();
  const contentEl = document.getElementById('omos-content');
  if (!contentEl || !pageType) return;

  contentEl.innerHTML = '<p class="omos-loading">불러오는 중...</p>';

  switch (pageType) {
    case 'ISSUE_LIST':
      renderIssueList(contentEl);
      break;
    case 'ISSUE_DETAIL':
      renderIssueDetail(contentEl);
      break;
    case 'PR_CREATE':
      renderPrCreate(contentEl);
      break;
    case 'PROFILE':
      renderProfile(contentEl);
      break;
  }
}

// ── 이슈 목록: 캐시된 전역 추천에서 현재 레포 이슈를 필터링해 뱃지 표시 ───

function renderIssueList(el) {
  const repoFullName = location.pathname.replace(/^\//, '').replace('/issues', '');
  el.innerHTML = '<p class="omos-loading">이슈 분석 중...</p>';

  sendMessage({ type: 'GET_CACHED_RECOMMENDATIONS' }, (res) => {
    const allIssues = res?.data?.data ?? [];
    const repoIssues = allIssues.filter((i) => i.repoFullName === repoFullName);

    highlightRecommendedIssues(repoIssues);

    if (repoIssues.length === 0) {
      el.innerHTML = `
        <p class="omos-empty">이 레포에 해당하는 추천 이슈가 없습니다.</p>
        <p class="omos-desc" style="margin-top:8px;">Extension 아이콘 → 새 추천 받기를 눌러 추천을 업데이트하세요.</p>
      `;
      return;
    }

    el.innerHTML = `
      <h3 class="omos-section-title">이슈 매칭 결과</h3>
      <p class="omos-desc">
        이 레포에서 <strong style="color:#e6edf3">${repoIssues.length}개</strong>의 이슈가 나와 잘 맞습니다.<br>
        <span style="color:#3fb950">✦ OMOS</span> 뱃지가 표시된 이슈를 확인하세요.
      </p>
      <div style="margin-top:12px;">
        ${repoIssues
          .map(
            (issue) => `
          <div class="omos-issue-card">
            <a href="https://github.com/${issue.repoFullName}/issues/${issue.issueNumber}" target="_blank">
              #${issue.issueNumber}
            </a>
            <p>${issue.title}</p>
            <div class="omos-labels">
              ${(issue.labels ?? []).map((l) => `<span class="omos-label">${l}</span>`).join('')}
            </div>
          </div>`
          )
          .join('')}
      </div>
    `;
  });
}

// 추천 이슈 번호와 일치하는 GitHub 이슈 행에 뱃지를 직접 삽입합니다
function highlightRecommendedIssues(issues) {
  const numberSet = new Set(issues.map((i) => String(i.issueNumber)));
  const repoPrefix = `/${location.pathname.split('/').slice(1, 3).join('/')}/issues/`;

  document.querySelectorAll(`a[href^="${repoPrefix}"]`).forEach((link) => {
    const match = link.getAttribute('href')?.match(/\/issues\/(\d+)$/);
    if (!match || !numberSet.has(match[1])) return;
    if (link.querySelector('.omos-badge')) return;

    const badge = document.createElement('span');
    badge.className = 'omos-badge';
    badge.textContent = '✦ OMOS';
    link.appendChild(badge);
  });
}

// ── 이슈 상세: 코드 가이드 + 의사 코드 ─────────────────────────────────────

function requestAnalysis(type, loadingText, repoFullName, issueNumber) {
  const resultEl = document.getElementById('omos-analysis-result');
  const guideBtn = document.getElementById('omos-guide-btn');
  const pseudoBtn = document.getElementById('omos-pseudo-btn');

  // 요청 중 두 버튼 모두 비활성화
  guideBtn.disabled = true;
  pseudoBtn.disabled = true;
  resultEl.innerHTML = `<p class="omos-loading">${loadingText} (수 분이 걸릴 수 있습니다)</p>`;

  sendMessage({ type, repoFullName, issueNumber }, (res) => {
    guideBtn.disabled = false;
    pseudoBtn.disabled = false;
    resultEl.innerHTML = res?.ok
      ? renderAnalysisResult(res.data?.data)
      : renderError(res?.error);
  });
}

function renderIssueDetail(el) {
  const pathParts = location.pathname.split('/');
  const repoFullName = `${pathParts[1]}/${pathParts[2]}`;
  const issueNumber = pathParts[pathParts.length - 1];

  el.innerHTML = `
    <div id="omos-rec-reason"></div>
    <h3 class="omos-section-title">이슈 분석</h3>
    <p class="omos-desc">이슈 #${issueNumber}에 대한 AI 분석을 제공합니다.</p>
    <button class="omos-btn" id="omos-guide-btn">코드 수정 가이드 보기</button>
    <button class="omos-btn omos-btn-secondary" id="omos-pseudo-btn">의사 코드 보기</button>
    <div id="omos-analysis-result"></div>
  `;

  sendMessage({ type: 'GET_CACHED_RECOMMENDATIONS' }, (res) => {
    const allIssues = res?.data?.data ?? [];
    const matched = allIssues.find(
      (i) => i.repoFullName === repoFullName && String(i.issueNumber) === issueNumber
    );
    const recReasonEl = document.getElementById('omos-rec-reason');
    if (recReasonEl && matched?.summary) {
      recReasonEl.innerHTML = `
        <h3 class="omos-section-title">추천 이유</h3>
        <div class="omos-reason-card">
          <p class="omos-reason-text">${matched.summary}</p>
        </div>
        <hr class="omos-divider">
      `;
    }
  });

  document.getElementById('omos-guide-btn').addEventListener('click', () => {
    requestAnalysis('GET_ISSUE_GUIDE', '분석 중...', repoFullName, issueNumber);
  });

  document.getElementById('omos-pseudo-btn').addEventListener('click', () => {
    requestAnalysis('GET_ISSUE_PSEUDO', '의사 코드 생성 중...', repoFullName, issueNumber);
  });
}

function renderAnalysisResult(data) {
  if (!data) return `<p class="omos-empty">결과가 없습니다.</p>`;

  const filePathsHtml = (data.filePaths ?? []).length > 0
    ? `<div class="omos-result-section">
        <p class="omos-result-label">수정 대상 파일</p>
        <ul class="omos-file-list">
          ${data.filePaths.map((p) => `<li class="omos-file-item">${p}</li>`).join('')}
        </ul>
       </div>`
    : '';

  if (data.guideline != null) {
    return `
      <div class="omos-result">
        ${filePathsHtml}
        <div class="omos-result-section">
          <p class="omos-result-label">수정 방향</p>
          <p class="omos-result-text">${data.guideline}</p>
        </div>
        <div class="omos-result-section">
          <p class="omos-result-label">주의 사항</p>
          <p class="omos-result-text omos-result-warning">${data.sideEffects}</p>
        </div>
      </div>`;
  }

  if (data.pseudoCode != null) {
    return `
      <div class="omos-result">
        ${filePathsHtml}
        <div class="omos-result-section">
          <p class="omos-result-label">의사 코드</p>
          <pre class="omos-code-block">${data.pseudoCode}</pre>
        </div>
      </div>`;
  }

  return `<p class="omos-empty">결과가 없습니다.</p>`;
}

// ── PR 생성: PR 초안 자동 생성 ──────────────────────────────────────────────

function renderPrCreate(el) {
  const pathParts = location.pathname.split('/');
  const upstreamRepo = `${pathParts[1]}/${pathParts[2]}`;

  // URL에서 baseBranch, headBranch 파싱
  // 형식: /owner/repo/compare/baseBranch...forkOwner:headBranch
  const comparePart = location.pathname.split('/compare/')[1] ?? '';
  const [rawBase, rawHead] = comparePart.split('...');
  const baseBranch = rawBase ?? '';
  const headBranch = rawHead?.includes(':') ? (rawHead.split(':').at(-1) ?? '') : (rawHead ?? '');

  el.innerHTML = `
    <h3 class="omos-section-title">PR 초안 생성</h3>
    <p class="omos-desc">diff를 분석해 PR 제목과 본문을 자동으로 생성합니다.</p>
    <div class="omos-field-group">
      <label class="omos-field-label">레포지토리</label>
      <p class="omos-field-value">${upstreamRepo}</p>
    </div>
    <div class="omos-field-group">
      <label class="omos-field-label">브랜치</label>
      <p class="omos-field-value">${baseBranch || '—'} → ${headBranch || '—'}</p>
    </div>
    <div class="omos-field-group">
      <label class="omos-field-label" for="omos-issue-number">연결할 이슈 번호 <span class="omos-required">*</span></label>
      <input class="omos-input" id="omos-issue-number" type="number" min="1" placeholder="예: 42" />
    </div>
    <button class="omos-btn" id="omos-draft-btn">초안 생성</button>
    <div id="omos-draft-result"></div>
  `;

  document.getElementById('omos-draft-btn').addEventListener('click', () => {
    const issueNumberInput = document.getElementById('omos-issue-number');
    const githubIssueNumber = parseInt(issueNumberInput?.value ?? '', 10);
    const resultEl = document.getElementById('omos-draft-result');

    if (!githubIssueNumber || githubIssueNumber < 1) {
      resultEl.innerHTML = `<p class="omos-error">이슈 번호를 입력해주세요.</p>`;
      return;
    }
    if (!baseBranch || !headBranch) {
      resultEl.innerHTML = `<p class="omos-error">브랜치 정보를 URL에서 읽을 수 없습니다.<br>GitHub compare 페이지에서 사용해주세요.</p>`;
      return;
    }

    resultEl.innerHTML = '<p class="omos-loading">생성 중... (수 분이 걸릴 수 있습니다)</p>';

    sendMessage(
      {
        type: 'CREATE_PR_DRAFT',
        payload: { upstreamRepo, githubIssueNumber, baseBranch, headBranch },
      },
      (res) => {
        if (!res?.ok) {
          resultEl.innerHTML = renderError(res?.error);
          return;
        }
        const draft = res.data?.data;
        resultEl.innerHTML = `
          <div class="omos-result">
            <div class="omos-result-section">
              <p class="omos-result-label">제목</p>
              <p class="omos-draft-title">${draft?.title ?? ''}</p>
            </div>
            <div class="omos-result-section">
              <p class="omos-result-label">본문</p>
              <pre class="omos-code-block">${draft?.body ?? ''}</pre>
            </div>
            <button class="omos-btn" id="omos-apply-btn">PR 폼에 적용</button>
            <div id="omos-translate-status"></div>
          </div>
        `;
        document.getElementById('omos-apply-btn')?.addEventListener('click', () => {
          translateAndApplyDraft(draft?.id, resultEl.querySelector('#omos-translate-status'));
        });
      }
    );
  });
}

function applyDraftToPrForm(title, body) {
  const titleInput = document.querySelector('#pull_request_title, input[name="pull_request[title]"]');
  const bodyInput = document.querySelector('#pull_request_body, textarea[name="pull_request[body]"]');
  if (titleInput && title) titleInput.value = title;
  if (bodyInput && body) bodyInput.value = body;
}

function translateAndApplyDraft(draftId, statusEl) {
  if (!draftId) {
    if (statusEl) statusEl.innerHTML = renderError('초안 ID가 없습니다.');
    return;
  }

  const applyBtn = document.getElementById('omos-apply-btn');
  if (applyBtn) applyBtn.disabled = true;
  if (statusEl) statusEl.innerHTML = '<p class="omos-loading">번역 중...</p>';

  sendMessage({ type: 'TRANSLATE_PR_DRAFT', draftId }, (res) => {
    if (applyBtn) applyBtn.disabled = false;
    if (!res?.ok) {
      if (statusEl) statusEl.innerHTML = renderError(res?.error);
      return;
    }
    const translated = res.data?.data;
    applyDraftToPrForm(translated?.titleEn, translated?.bodyEn);
    if (statusEl) statusEl.innerHTML = '<p class="omos-desc" style="color:#3fb950">번역 후 PR 폼에 적용됐습니다.</p>';
  });
}

// ── 프로필: 내 계정 정보 + 프로필 벡터 갱신 ─────────────────────────────────

function formatDateTime(value) {
  if (!value) return null;
  // Jackson이 배열([year,month,day,hour,minute,second])로 직렬화하는 경우 대응
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value;
    return new Date(year, month - 1, day, hour, minute).toLocaleString('ko-KR');
  }
  return new Date(value).toLocaleString('ko-KR');
}

function renderProfile(el) {
  el.innerHTML = '<p class="omos-loading">프로필 불러오는 중...</p>';

  sendMessage({ type: 'GET_MY_PROFILE' }, (res) => {
    if (!res?.ok) {
      el.innerHTML = renderError(res?.error);
      return;
    }

    const user = res.data?.data;
    if (!user) {
      el.innerHTML = renderError('프로필 정보를 불러올 수 없습니다.');
      return;
    }

    const languages = user.primaryLanguages ?? [];
    const languagesHtml = languages.length > 0
      ? `<div class="omos-lang-tags">${languages.map((l) => `<span class="omos-lang-tag">${l}</span>`).join('')}</div>`
      : `<p class="omos-empty">언어 정보 없음 (벡터 갱신 시 업데이트됩니다)</p>`;

    const vectorUpdatedAt = formatDateTime(user.vectorUpdatedAt);

    el.innerHTML = `
      <h3 class="omos-section-title">내 계정</h3>
      <div class="omos-profile-info">
        <div class="omos-info-row">
          <span class="omos-info-label">GitHub ID</span>
          <span class="omos-info-value">${user.githubId}</span>
        </div>
        ${user.name ? `
        <div class="omos-info-row">
          <span class="omos-info-label">이름</span>
          <span class="omos-info-value">${user.name}</span>
        </div>` : ''}
        ${user.email ? `
        <div class="omos-info-row">
          <span class="omos-info-label">이메일</span>
          <span class="omos-info-value">${user.email}</span>
        </div>` : ''}
      </div>
      <hr class="omos-divider">
      <h3 class="omos-section-title">주요 언어</h3>
      ${languagesHtml}
      <hr class="omos-divider">
      <h3 class="omos-section-title">프로필 벡터</h3>
      <div class="omos-info-row" style="margin-bottom:12px;">
        <span class="omos-info-label">마지막 갱신</span>
        <span class="omos-info-value" id="omos-vector-date">${vectorUpdatedAt ?? '아직 갱신되지 않음'}</span>
      </div>
      <button class="omos-btn" id="omos-update-vector-btn">프로필 벡터 갱신</button>
      <div id="omos-vector-status"></div>
    `;

    document.getElementById('omos-update-vector-btn').addEventListener('click', () => {
      const btn = document.getElementById('omos-update-vector-btn');
      const statusEl = document.getElementById('omos-vector-status');
      btn.disabled = true;
      statusEl.innerHTML = '<p class="omos-loading">갱신 중... (수 분이 걸릴 수 있습니다)</p>';

      sendMessage({ type: 'UPDATE_PROFILE_VECTOR' }, (res) => {
        btn.disabled = false;
        if (!res?.ok) {
          statusEl.innerHTML = renderError(res?.error);
          return;
        }
        const updated = res.data?.data;
        const updatedAt = formatDateTime(updated?.vectorUpdatedAt);
        const dateEl = document.getElementById('omos-vector-date');
        if (dateEl && updatedAt) dateEl.textContent = updatedAt;
        statusEl.innerHTML = `<p class="omos-success">갱신 완료${updatedAt ? ` (${updatedAt})` : ''}</p>`;
      });
    });
  });
}

// ── 공통 ─────────────────────────────────────────────────────────────────────

function renderError(error) {
  if (error === 'NOT_AUTHENTICATED') {
    return `<p class="omos-error">로그인이 필요합니다.<br>Extension 아이콘을 클릭해 로그인해주세요.</p>`;
  }
  if (error === 'TOKEN_EXPIRED') {
    return `<p class="omos-error">세션이 만료됐습니다.<br>Extension 아이콘을 클릭해 다시 로그인해주세요.</p>`;
  }
  if (error === 'ISSUE_NOT_IN_DB') {
    return `<p class="omos-error">이 이슈는 OMOS에 등록되지 않았습니다.<br>추천 이슈 목록에서 접근해주세요.</p>`;
  }
  if (error === 'CONTEXT_INVALIDATED') {
    return `<p class="omos-error">Extension이 업데이트됐습니다.<br>페이지를 새로고침해주세요. (F5)</p>`;
  }
  return `<p class="omos-error">오류가 발생했습니다.<br>${error ?? '알 수 없는 오류'}</p>`;
}

// ── 진입점 ───────────────────────────────────────────────────────────────────

// GitHub은 Turbo SPA를 사용하므로 페이지 이동 시 content script가 재실행되지 않습니다.
// turbo:load + popstate 이벤트로 URL 변경을 감지해 사이드바를 재초기화합니다.

function init() {
  document.getElementById('omos-sidebar')?.remove();
  document.getElementById('omos-toggle')?.remove();

  const pageType = detectPageType();
  if (pageType) {
    const sidebar = createSidebar();
    createToggleButton(sidebar);
  }
}

init();
document.addEventListener('turbo:load', init);
window.addEventListener('popstate', init);

chrome.runtime.onMessage.addListener((message) => {
  if (message.type === 'AUTH_SUCCESS') {
    loadContent();
  }
});
