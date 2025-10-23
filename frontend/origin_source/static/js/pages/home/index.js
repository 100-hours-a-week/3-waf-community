/**
 * Home Page Script
 * 소개 페이지 로직
 */

(function(window, document) {
    'use strict';

    /**
     * 초기화
     */
    function init() {
        loadStats();
    }

    /**
     * 통계 데이터 로드
     */
    async function loadStats() {
        try {
            const stats = await fetchStats();
            updateStatsDisplay(stats);
        } catch (error) {
            console.error('Failed to load stats:', error);
            updateStatsDisplay({
                posts: 150,
                members: 89,
                comments: 420
            });
        }
    }

    /**
     * 통계 API 호출
     */
    async function fetchStats() {
        const response = await fetch('http://localhost:8080/stats', {
            credentials: 'include'
        });
        
        if (response.ok) {
            const data = await response.json();
            return data.data;
        }
        
        throw new Error('Failed to fetch stats');
    }

    /**
     * 통계 표시 업데이트
     */
    function updateStatsDisplay(stats) {
        const elements = {
            posts: document.querySelector('[data-stat="posts"]'),
            members: document.querySelector('[data-stat="members"]'),
            comments: document.querySelector('[data-stat="comments"]')
        };

        animateNumber(elements.posts, stats.posts || 0);
        animateNumber(elements.members, stats.members || 0);
        animateNumber(elements.comments, stats.comments || 0);
    }

    /**
     * 숫자 카운트업 애니메이션
     */
    function animateNumber(element, target) {
        if (!element) return;

        const duration = 2000;
        const start = 0;
        const startTime = performance.now();

        function update(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            const current = Math.floor(start + (target - start) * progress);
            element.textContent = current.toLocaleString();

            if (progress < 1) {
                requestAnimationFrame(update);
            }
        }

        requestAnimationFrame(update);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})(window, document);