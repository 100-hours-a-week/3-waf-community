/**
 * Board List Page Script
 * 게시글 목록 페이지 로직
 * 참조: @CLAUDE.md Section 4.1, @docs/be/API.md Section 3.1
 */

(function(window, document) {
  'use strict';

  // ============================================
  // Configuration
  // ============================================
  const CONFIG = {
    PAGE_SIZE: 10,
    WRITE_POST_URL: '/pages/board/write.html',
    POST_DETAIL_URL: '/pages/board/detail.html',
    LOGIN_URL: '/pages/user/login.html'
  };

  // ============================================
  // State Management
  // ============================================
  const state = {
    currentCursor: null, // Cursor 페이지네이션
    hasMore: true,
    isLoading: false
  };

  // ============================================
  // DOM Element Caching
  // ============================================
  const elements = {
    postList: null,
    loadingIndicator: null,
    emptyState: null,
    writeButton: null,
    loadMoreButton: null,
    loadMoreContainer: null,
    profileImage: null,
    profileButton: null
  };

  // ============================================
  // Initialization
  // ============================================
  function init() {
    cacheElements();
    bindEvents();
    loadInitialData();
  }

  function cacheElements() {
    elements.postList = document.querySelector('[data-list="posts"]');
    elements.loadingIndicator = document.querySelector('[data-loading="posts"]');
    elements.emptyState = document.querySelector('[data-empty="posts"]');
    elements.writeButton = document.querySelector('[data-action="write-post"]');
    elements.loadMoreButton = document.querySelector('[data-action="load-more"]');
    elements.loadMoreContainer = document.querySelector('.load-more-container');
    elements.profileImage = document.querySelector('[data-profile="image"]');
    elements.profileButton = document.querySelector('[data-action="toggle-menu"]');
  }

  // ============================================
  // Event Binding
  // ============================================
  function bindEvents() {
    // 게시글 작성 버튼
    if (elements.writeButton) {
      elements.writeButton.addEventListener('click', handleWriteClick);
    }

    // 더보기 버튼
    if (elements.loadMoreButton) {
      elements.loadMoreButton.addEventListener('click', handleLoadMore);
    }

    // 게시글 카드 클릭 (이벤트 위임)
    if (elements.postList) {
      elements.postList.addEventListener('click', handlePostClick);
    }

    // 프로필 메뉴 (추후 구현)
    if (elements.profileButton) {
      elements.profileButton.addEventListener('click', handleProfileMenu);
    }
  }

  // ============================================
  // Initial Data Load
  // ============================================
  async function loadInitialData() {
    // 샘플 게시글 제거
    if (elements.postList) {
      elements.postList.innerHTML = '';
    }

    // 프로필 이미지 로드
    loadUserProfile();

    // 첫 페이지 게시글 로드
    await loadPosts();
  }

  // ============================================
  // Event Handlers
  // ============================================
  function handleWriteClick(e) {
    e.preventDefault();

    // 로그인 확인
    if (!isAuthenticated()) {
      alert('로그인이 필요합니다.');
      window.location.href = CONFIG.LOGIN_URL;
      return;
    }

    // 게시글 작성 페이지로 이동
    window.location.href = CONFIG.WRITE_POST_URL;
  }

  function handleLoadMore(e) {
    e.preventDefault();
    loadPosts();
  }

  function handlePostClick(e) {
    const card = e.target.closest('.post-card');
    if (!card) return;

    const postId = card.dataset.postId;
    if (postId) {
      window.location.href = `${CONFIG.POST_DETAIL_URL}?id=${postId}`;
    }
  }

  function handleProfileMenu(e) {
    e.preventDefault();
    // TODO: Phase 5에서 프로필 메뉴 구현
    alert('프로필 메뉴는 추후 구현 예정입니다.');
  }

  // ============================================
  // API Functions
  // ============================================
  /**
   * 게시글 목록 로드 (Cursor 페이지네이션)
   * GET /posts?cursor={cursor}&limit={limit}&sort=latest
   */
  async function loadPosts() {
    if (state.isLoading || !state.hasMore) return;

    try {
      setLoading(true);

      // API 호출 (cursor 방식)
      const params = new URLSearchParams({
        limit: CONFIG.PAGE_SIZE,
        sort: 'latest'
      });

      if (state.currentCursor) {
        params.append('cursor', state.currentCursor);
      }

      const result = await fetchWithAuth(`/posts?${params}`);

      // 응답 구조: { posts: [...], nextCursor: 100, hasMore: true }
      handlePostsLoaded(result);

    } catch (error) {
      console.error('Failed to load posts:', error);
      showError('게시글을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }

  function handlePostsLoaded(data) {
    const { posts, nextCursor, hasMore } = data;

    // 첫 로드이고 게시글이 없는 경우
    if (posts.length === 0 && state.currentCursor === null) {
      showEmptyState();
      return;
    }

    // 게시글 렌더링
    posts.forEach(post => {
      renderPost(post);
    });

    // 상태 업데이트
    state.currentCursor = nextCursor;
    state.hasMore = hasMore;

    // 더보기 버튼 표시/숨김
    updateLoadMoreButton();

    // Empty state 숨김
    if (elements.emptyState) {
      elements.emptyState.style.display = 'none';
    }
  }

  // ============================================
  // Render Functions
  // ============================================
  function renderPost(post) {
    const card = createPostCard(post);
    if (elements.postList) {
      elements.postList.appendChild(card);
    }
  }

  /**
   * 게시글 카드 생성
   * @param {Object} post - 게시글 객체
   * @returns {HTMLElement} - 게시글 카드 요소
   */
  function createPostCard(post) {
    const article = document.createElement('article');
    article.className = 'post-card';
    article.dataset.postId = post.postId;

    // 작성자 정보
    const author = post.author || {};
    const authorName = escapeHtml(author.nickname || '알 수 없음');
    const authorImage = author.profileImage || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default';

    // 통계 정보
    const stats = post.stats || { likeCount: 0, commentCount: 0, viewCount: 0 };

    // 날짜 포맷
    const formattedDate = formatDate(post.createdAt);

    // 이미지 HTML (있는 경우)
    const imageHtml = post.imageUrl ? `
      <div class="post-card__image-wrapper">
        <img src="${escapeHtml(post.imageUrl)}" alt="게시글 이미지" class="post-card__image">
      </div>
    ` : '';

    article.innerHTML = `
      <div class="post-card__header">
        <img src="${authorImage}" alt="작성자 프로필" class="post-card__avatar">
        <div class="post-card__author">
          <div class="post-card__author-name">${authorName}</div>
          <div class="post-card__date">${formattedDate}</div>
        </div>
      </div>
      ${imageHtml}
      <h3 class="post-card__title">${escapeHtml(post.title)}</h3>
      <div class="post-card__meta">
        <div class="post-card__stat">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
          <span>${formatNumberCompact(stats.likeCount)}</span>
        </div>
        <div class="post-card__stat">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
          </svg>
          <span>${formatNumberCompact(stats.commentCount)}</span>
        </div>
        <div class="post-card__stat">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
          </svg>
          <span>${formatNumberCompact(stats.viewCount)}</span>
        </div>
      </div>
    `;

    return article;
  }

  // ============================================
  // UI Helper Functions
  // ============================================
  function setLoading(loading) {
    state.isLoading = loading;

    if (elements.loadingIndicator) {
      elements.loadingIndicator.style.display = loading ? 'flex' : 'none';
    }
  }

  function showEmptyState() {
    if (elements.emptyState) {
      elements.emptyState.style.display = 'flex';
    }
    if (elements.postList) {
      elements.postList.style.display = 'none';
    }
  }

  function updateLoadMoreButton() {
    if (elements.loadMoreContainer) {
      elements.loadMoreContainer.style.display = state.hasMore ? 'block' : 'none';
    }
  }

  function showError(message) {
    alert(translateErrorCode(message));
  }

  // ============================================
  // User Profile
  // ============================================
  async function loadUserProfile() {
    if (!isAuthenticated()) {
      // 비로그인 상태 - 기본 이미지 표시
      if (elements.profileImage) {
        elements.profileImage.src = 'https://api.dicebear.com/7.x/avataaars/svg?seed=anonymous';
      }
      return;
    }

    try {
      const userId = getCurrentUserId();
      if (!userId) return;

      const user = await fetchWithAuth(`/users/${userId}`);

      // 프로필 이미지 설정
      if (elements.profileImage && user.profileImage) {
        elements.profileImage.src = user.profileImage;
      }
    } catch (error) {
      console.error('Failed to load user profile:', error);
    }
  }

  // ============================================
  // DOMContentLoaded Event
  // ============================================
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})(window, document);
