const loginBtn = document.getElementById('login-btn');
const logoutBtn = document.getElementById('logout-btn');
const statusDot = document.getElementById('status-dot');
const statusLabel = document.getElementById('status-label');
const recList = document.getElementById('rec-list');
const recHint = document.getElementById('rec-hint');
const recLoading = document.getElementById('rec-loading');

function setLoggedIn(isLoggedIn) {
  if (isLoggedIn) {
    statusDot.className = 'dot on';
    statusLabel.textContent = '로그인됨';
    loginBtn.style.display = 'none';
    logoutBtn.style.display = 'block';
    loadHistory();
  } else {
    statusDot.className = 'dot off';
    statusLabel.textContent = '로그인되지 않음';
    loginBtn.style.display = 'block';
    logoutBtn.style.display = 'none';
    renderEmptyState('로그인 후 추천 이슈를 확인할 수 있습니다.');
  }
}

// 추천 이력 조회 (빠름, DB 기반)
function loadHistory() {
  recLoading.style.display = 'block';
  recList.innerHTML = '';

  chrome.runtime.sendMessage({ type: 'GET_RECOMMEND_HISTORY' }, (res) => {
    recLoading.style.display = 'none';

    if (!res?.ok) {
      renderEmptyState('이력을 불러오지 못했습니다.');
      return;
    }

    const issues = res.data?.data ?? [];
    if (issues.length === 0) {
      renderEmptyState('아직 추천 이력이 없습니다.\n아래 버튼으로 첫 추천을 받아보세요.');
    } else {
      recHint.textContent = `${issues.length}개`;
      renderIssues(issues);
    }

    appendRefreshButton();
  });
}

// 새 추천 생성 (느림, AI 호출)
function refreshRecommendations() {
  recLoading.style.display = 'block';
  recList.innerHTML = '';
  recHint.textContent = '';

  chrome.runtime.sendMessage({ type: 'GET_RECOMMEND_ISSUES' }, (res) => {
    if (!res?.ok) {
      recLoading.style.display = 'none';
      renderEmptyState('추천 생성에 실패했습니다.');
      appendRefreshButton();
      return;
    }

    // 새 추천이 DB에 저장됐으므로 갱신된 이력을 바로 표시
    loadHistory();
  });
}

function renderIssues(issues) {
  const cards = issues
    .map((issue) => {
      const url = `https://github.com/${issue.repoFullName}/issues/${issue.issueNumber}`;
      const labels = (issue.labels ?? [])
        .map((l) => `<span class="rec-label">${l}</span>`)
        .join('');
      const analyzedBadge = issue.isAnalyzed
        ? `<span class="rec-analyzed">분석 완료</span>`
        : '';

      return `
        <a class="rec-card" href="${url}" target="_blank">
          <div class="rec-card-meta">
            <span class="rec-repo">${issue.repoFullName} #${issue.issueNumber}</span>
            ${analyzedBadge}
          </div>
          <p class="rec-title">${issue.title}</p>
          ${issue.summary ? `<p class="rec-summary">${issue.summary}</p>` : ''}
          ${labels ? `<div class="rec-labels">${labels}</div>` : ''}
        </a>
      `;
    })
    .join('');

  recList.innerHTML = cards;
}

function renderEmptyState(message) {
  recList.innerHTML = `
    <div class="empty-state">
      <div>${message.replace('\n', '<br>')}</div>
    </div>
  `;
}

function appendRefreshButton() {
  const btn = document.createElement('button');
  btn.id = 'refresh-btn';
  btn.textContent = '새 추천 받기';
  btn.addEventListener('click', () => {
    btn.remove();
    refreshRecommendations();
  });
  recList.appendChild(btn);
}

// 초기화
chrome.runtime.sendMessage({ type: 'GET_AUTH_STATUS' }, (res) => {
  setLoggedIn(res?.isLoggedIn ?? false);
});

loginBtn.addEventListener('click', () => {
  chrome.runtime.sendMessage({ type: 'LOGIN' });
  window.close();
});

logoutBtn.addEventListener('click', () => {
  chrome.runtime.sendMessage({ type: 'LOGOUT' }, () => setLoggedIn(false));
});
