-- =============================================
-- 더미 데이터 검증 스크립트
-- =============================================

-- 1. 전체 데이터 개수 확인
SELECT '=== 전체 데이터 개수 ===' AS info;
SELECT
    'users' AS table_name,
    COUNT(*) AS count,
    '1200 예상' AS expected
FROM users
UNION ALL
SELECT
    'posts',
    COUNT(*),
    '3000 예상'
FROM posts
UNION ALL
SELECT
    'post_stats',
    COUNT(*),
    '3000 예상'
FROM post_stats
UNION ALL
SELECT
    'comments',
    COUNT(*),
    '30000~45000 예상'
FROM comments
UNION ALL
SELECT
    'post_likes',
    COUNT(*),
    '1200 예상'
FROM post_likes;

-- 2. post_id=3000 게시글 확인
SELECT '\n=== post_id=3000 게시글 정보 ===' AS info;
SELECT
    p.post_id,
    p.post_title,
    p.post_status,
    u.nickname AS author,
    ps.like_count,
    ps.comment_count,
    ps.view_count,
    p.created_at
FROM posts p
LEFT JOIN users u ON p.user_id = u.user_id
LEFT JOIN post_stats ps ON p.post_id = ps.post_id
WHERE p.post_id = 3000;

-- 3. post_id=3000의 좋아요 개수 확인
SELECT '\n=== post_id=3000 좋아요 개수 ===' AS info;
SELECT
    post_id,
    COUNT(*) AS actual_likes,
    '1200 예상' AS expected
FROM post_likes
WHERE post_id = 3000
GROUP BY post_id;

-- 4. 샘플 좋아요 데이터 확인 (처음 10개)
SELECT '\n=== post_id=3000 좋아요 샘플 (처음 10개) ===' AS info;
SELECT
    pl.user_id,
    u.nickname,
    u.email,
    pl.created_at
FROM post_likes pl
LEFT JOIN users u ON pl.user_id = u.user_id
WHERE pl.post_id = 3000
ORDER BY pl.user_id
LIMIT 10;

-- 5. 좋아요 통계 (게시글별 top 10)
SELECT '\n=== 좋아요가 많은 게시글 TOP 10 ===' AS info;
SELECT
    p.post_id,
    p.post_title,
    ps.like_count,
    ps.comment_count,
    ps.view_count
FROM posts p
LEFT JOIN post_stats ps ON p.post_id = ps.post_id
ORDER BY ps.like_count DESC
LIMIT 10;

-- 6. 데이터 무결성 검증
SELECT '\n=== 데이터 무결성 검증 ===' AS info;

-- post_stats.like_count와 실제 post_likes 개수 비교
SELECT
    ps.post_id,
    ps.like_count AS stats_like_count,
    COUNT(pl.user_id) AS actual_like_count,
    CASE
        WHEN ps.like_count = COUNT(pl.user_id) THEN '✓ 일치'
        ELSE '✗ 불일치'
    END AS status
FROM post_stats ps
LEFT JOIN post_likes pl ON ps.post_id = pl.post_id
WHERE ps.post_id = 3000
GROUP BY ps.post_id, ps.like_count;
