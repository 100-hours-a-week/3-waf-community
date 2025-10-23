package com.ktb.community.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 더미 데이터 SQL 생성 유틸리티
 *
 * 실행 방법:
 * 1. IntelliJ에서 이 파일을 열고 main() 메서드 실행 (Run 버튼 클릭)
 * 2. 또는 터미널에서:
 *    ./gradlew compileJava
 *    java -cp "build/classes/java/main:$HOME/.gradle/caches/modules-2/files-2.1/org.springframework.security/spring-security-crypto/6.*//*.jar" \
 *         com.ktb.community.util.DummyDataSqlGenerator
 *
 * 생성 데이터:
 * - 유저: 1200명
 * - 게시글: 3000개
 * - 댓글: 30000~45000개 (게시글당 10~15개)
 * - 좋아요: 1200개 (post_id=3000에 모든 유저)
 *
 * 출력: scripts/dummy_data.sql
 */
public class DummyDataSqlGenerator {

    // 데이터 개수 상수
    private static final int USER_COUNT = 1200;
    private static final int POST_COUNT = 3000;
    private static final int SPECIAL_POST_ID = POST_COUNT; // 모든 유저가 좋아요한 게시글
    private static final int MIN_COMMENTS_PER_POST = 10;
    private static final int MAX_COMMENTS_PER_POST = 15;

    // 파일 경로
    private static final String OUTPUT_FILE = "scripts/dummy_data.sql";

    // 날짜 형식
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // BCrypt (비밀번호 해시)
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String COMMON_PASSWORD = "Test1234!";
    private static String passwordHash; // 한 번만 생성하여 재사용

    // 랜덤 생성기
    private static final Random random = new Random();

    // 한글 이름 샘플 데이터
    private static final String[] KOREAN_FIRST_NAMES = {
            "민준", "서준", "예준", "도윤", "시우", "주원", "하준", "지호", "지우", "준서",
            "준우", "현우", "도현", "건우", "우진", "선우", "서진", "민재", "현준", "연우",
            "서현", "민서", "하은", "서윤", "지우", "서연", "지유", "수아", "예은", "지민",
            "채원", "다은", "수빈", "소율", "예린", "지안", "윤서", "하린", "시은", "아인"
    };

    private static final String[] KOREAN_LAST_NAMES = {
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
            "한", "오", "서", "신", "권", "황", "안", "송", "류", "전"
    };

    // 한글 게시글 제목 템플릿
    private static final String[] POST_TITLE_TEMPLATES = {
            "오늘의 일상 공유합니다",
            "처음 써보는 글이에요",
            "질문 있습니다",
            "추천 부탁드려요",
            "이거 어떻게 생각하세요",
            "공감되시나요",
            "같이 이야기 나눠요",
            "궁금한게 있어요",
            "도움이 필요해요",
            "의견 들려주세요",
            "새로운 소식 전해드려요",
            "재미있는 이야기",
            "일상 속 작은 행복",
            "생각해볼 문제",
            "함께 고민해봐요",
            "이런 경험 있으신가요",
            "오늘 하루 어땠나요",
            "주말 계획 있으세요",
            "좋은 정보 공유",
            "알아두면 좋은 팁"
    };

    // 한글 게시글 내용 템플릿
    private static final String[] POST_CONTENT_TEMPLATES = {
            "안녕하세요! 오늘은 정말 좋은 하루였습니다. 여러분은 어떠셨나요? 댓글로 하루 일과를 공유해주세요.",
            "처음으로 글을 작성해봅니다. 많은 관심과 조언 부탁드립니다. 잘 부탁드려요!",
            "이 주제에 대해 여러분의 생각이 궁금합니다. 자유롭게 의견을 남겨주세요.",
            "최근에 알게 된 유용한 정보를 공유하고 싶어서 글을 작성합니다. 도움이 되셨으면 좋겠습니다.",
            "비슷한 경험을 하신 분들이 계실까요? 저만 그런 건지 궁금해서 여쭤봅니다.",
            "주말에 시간이 나서 이것저것 해봤는데, 생각보다 재미있더라구요. 추천합니다!",
            "요즘 고민이 있어서 글을 올립니다. 조언 부탁드려요.",
            "이런 상황에서 어떻게 대처하는 게 좋을까요? 경험담을 듣고 싶습니다.",
            "오늘 새로 알게 된 사실을 공유합니다. 알아두면 유용할 것 같아요.",
            "일상에서 느낀 소소한 행복을 나누고 싶어서 글을 작성했습니다."
    };

    // 한글 댓글 템플릿
    private static final String[] COMMENT_TEMPLATES = {
            "좋은 글 감사합니다!",
            "공감이 많이 되네요.",
            "저도 비슷한 경험이 있어요.",
            "유용한 정보 감사합니다.",
            "도움이 많이 됐어요.",
            "궁금했던 내용인데 감사합니다.",
            "저도 그렇게 생각합니다.",
            "좋은 의견이네요.",
            "응원합니다!",
            "멋진 글이에요.",
            "재미있게 읽었습니다.",
            "다음 글도 기대할게요.",
            "정말 유익한 정보네요.",
            "공유해주셔서 감사해요.",
            "저도 한번 시도해볼게요.",
            "추천 누르고 갑니다.",
            "잘 읽었습니다.",
            "좋은 하루 보내세요!",
            "감사합니다.",
            "계속 좋은 글 부탁드려요."
    };

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("더미 데이터 SQL 생성 시작");
        System.out.println("=".repeat(60));
        System.out.println("생성 데이터: 유저 " + USER_COUNT + "명, 게시글 " + POST_COUNT + "개");
        System.out.println("=".repeat(60));

        long startTime = System.currentTimeMillis();

        try {
            // 비밀번호 해시 생성 (한 번만)
            System.out.println("\n[1/6] 비밀번호 해시 생성 중...");
            passwordHash = passwordEncoder.encode(COMMON_PASSWORD);
            System.out.println("✓ 완료: " + passwordHash);

            // 파일 생성
            System.out.println("\n[2/6] SQL 파일 생성 중: " + OUTPUT_FILE);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
                writeHeader(writer);

                System.out.println("\n[3/6] 유저 데이터 생성 중... (" + USER_COUNT + "명)");
                generateUsers(writer);

                System.out.println("\n[4/6] 게시글 및 통계 생성 중... (" + POST_COUNT + "개)");
                generatePostsAndStats(writer);

                System.out.println("\n[5/6] 댓글 생성 중... (게시글당 " + MIN_COMMENTS_PER_POST + "~" + MAX_COMMENTS_PER_POST + "개)");
                generateComments(writer);

                System.out.println("\n[6/6] 좋아요 생성 중... (" + USER_COUNT + "개, post_id=" + SPECIAL_POST_ID + ")");
                generateLikes(writer);

                writeFooter(writer);
            }

            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            System.out.println("\n" + "=".repeat(60));
            System.out.println("✓ SQL 파일 생성 완료!");
            System.out.println("=".repeat(60));
            System.out.println("파일 경로: " + OUTPUT_FILE);
            System.out.println("실행 시간: " + String.format("%.2f", durationSeconds) + "초");
            System.out.println("\n실행 방법:");
            System.out.println("  mysql -u root -p community");
            System.out.println("  source " + OUTPUT_FILE + ";");
            System.out.println("=".repeat(60));

        } catch (IOException e) {
            System.err.println("파일 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * SQL 파일 헤더 작성
     */
    private static void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("-- =============================================\n");
        writer.write("-- 더미 데이터 생성 스크립트\n");
        writer.write("-- 생성일: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
        writer.write("-- =============================================\n");
        writer.write("-- 유저: " + USER_COUNT + "명\n");
        writer.write("-- 게시글: " + POST_COUNT + "개\n");
        writer.write("-- 댓글: 게시글당 " + MIN_COMMENTS_PER_POST + "~" + MAX_COMMENTS_PER_POST + "개\n");
        writer.write("-- 좋아요: " + USER_COUNT + "개 (post_id=" + SPECIAL_POST_ID + ")\n");
        writer.write("-- =============================================\n\n");
        writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
    }

    /**
     * SQL 파일 푸터 작성
     */
    private static void writeFooter(BufferedWriter writer) throws IOException {
        writer.write("\nSET FOREIGN_KEY_CHECKS = 1;\n");
        writer.write("\n-- =============================================\n");
        writer.write("-- 데이터 생성 완료\n");
        writer.write("-- =============================================\n");
    }

    /**
     * 유저 데이터 생성
     */
    private static void generateUsers(BufferedWriter writer) throws IOException {
        writer.write("-- =============================================\n");
        writer.write("-- 1. Users (" + USER_COUNT + " rows)\n");
        writer.write("-- =============================================\n");
        writer.write("INSERT INTO users (email, password_hash, nickname, role, user_status, created_at, updated_at, image_id) VALUES\n");

        Set<String> usedEmails = new HashSet<>();
        Set<String> usedNicknames = new HashSet<>();

        for (int i = 1; i <= USER_COUNT; i++) {
            // 이메일 생성 (중복 방지)
            String email;
            do {
                String lastName = KOREAN_LAST_NAMES[random.nextInt(KOREAN_LAST_NAMES.length)];
                String firstName = KOREAN_FIRST_NAMES[random.nextInt(KOREAN_FIRST_NAMES.length)];
                int number = random.nextInt(1000);
                email = lastName.toLowerCase() + firstName.toLowerCase() + number + "@test.com";
            } while (usedEmails.contains(email));
            usedEmails.add(email);

            // 닉네임 생성 (중복 방지, 10자 제한)
            String nickname;
            do {
                String lastName = KOREAN_LAST_NAMES[random.nextInt(KOREAN_LAST_NAMES.length)];
                String firstName = KOREAN_FIRST_NAMES[random.nextInt(KOREAN_FIRST_NAMES.length)];
                nickname = lastName + firstName;

                // 중복 시 숫자 추가
                if (usedNicknames.contains(nickname)) {
                    nickname = lastName + firstName + random.nextInt(100);
                }

                // 10자 제한
                if (nickname.length() > 10) {
                    nickname = nickname.substring(0, 10);
                }
            } while (usedNicknames.contains(nickname));
            usedNicknames.add(nickname);

            // 생성 날짜 (최근 1년 내 랜덤)
            LocalDateTime createdAt = randomDateTime(365);

            // SQL 생성
            String values = String.format("('%s', '%s', '%s', 'USER', 'ACTIVE', '%s', '%s', NULL)",
                    escape(email),
                    escape(passwordHash),
                    escape(nickname),
                    createdAt.format(TIMESTAMP_FORMAT),
                    createdAt.format(TIMESTAMP_FORMAT)
            );

            if (i < USER_COUNT) {
                writer.write(values + ",\n");
            } else {
                writer.write(values + ";\n\n");
            }

            // 진행률 출력
            if (i % 100 == 0) {
                System.out.print("\r  진행률: " + i + "/" + USER_COUNT);
            }
        }
        System.out.print("\r  진행률: " + USER_COUNT + "/" + USER_COUNT + " ✓\n");
    }

    /**
     * 게시글 및 통계 데이터 생성
     */
    private static void generatePostsAndStats(BufferedWriter writer) throws IOException {
        // 게시글 데이터
        writer.write("-- =============================================\n");
        writer.write("-- 2. Posts (" + POST_COUNT + " rows)\n");
        writer.write("-- =============================================\n");
        writer.write("INSERT INTO posts (post_title, post_content, created_at, updated_at, post_status, user_id) VALUES\n");

        List<LocalDateTime> postCreatedDates = new ArrayList<>();

        for (int i = 1; i <= POST_COUNT; i++) {
            // 제목 생성 (27자 제한)
            String titleTemplate = POST_TITLE_TEMPLATES[random.nextInt(POST_TITLE_TEMPLATES.length)];
            String title = titleTemplate + " #" + i;
            if (title.length() > 27) {
                title = title.substring(0, 27);
            }

            // 내용 생성
            String content = POST_CONTENT_TEMPLATES[random.nextInt(POST_CONTENT_TEMPLATES.length)];

            // 작성자 (랜덤 user_id)
            int userId = random.nextInt(USER_COUNT) + 1;

            // 생성 날짜 (최근 6개월 내 랜덤)
            LocalDateTime createdAt = randomDateTime(180);
            postCreatedDates.add(createdAt);

            // SQL 생성
            String values = String.format("('%s', '%s', '%s', '%s', 'ACTIVE', %d)",
                    escape(title),
                    escape(content),
                    createdAt.format(TIMESTAMP_FORMAT),
                    createdAt.format(TIMESTAMP_FORMAT),
                    userId
            );

            if (i < POST_COUNT) {
                writer.write(values + ",\n");
            } else {
                writer.write(values + ";\n\n");
            }

            // 진행률 출력
            if (i % 100 == 0) {
                System.out.print("\r  게시글 진행률: " + i + "/" + POST_COUNT);
            }
        }
        System.out.print("\r  게시글 진행률: " + POST_COUNT + "/" + POST_COUNT + " ✓\n");

        // 게시글 통계 데이터
        writer.write("-- =============================================\n");
        writer.write("-- 3. Post Stats (" + POST_COUNT + " rows)\n");
        writer.write("-- =============================================\n");
        writer.write("INSERT INTO post_stats (post_id, like_count, comment_count, view_count, last_updated) VALUES\n");

        for (int i = 1; i <= POST_COUNT; i++) {
            int likeCount;
            int commentCount;
            int viewCount;

            // 마지막 게시글 (post_id=3000)은 특별 처리
            if (i == SPECIAL_POST_ID) {
                likeCount = USER_COUNT; // 모든 유저가 좋아요
                commentCount = 15; // 댓글 많이
                viewCount = random.nextInt(1000) + 500; // 높은 조회수
            } else {
                likeCount = random.nextInt(50); // 0~49
                commentCount = random.nextInt(MAX_COMMENTS_PER_POST - MIN_COMMENTS_PER_POST + 1) + MIN_COMMENTS_PER_POST;
                viewCount = random.nextInt(500); // 0~499
            }

            LocalDateTime lastUpdated = postCreatedDates.get(i - 1);

            String values = String.format("(%d, %d, %d, %d, '%s')",
                    i,
                    likeCount,
                    commentCount,
                    viewCount,
                    lastUpdated.format(TIMESTAMP_FORMAT)
            );

            if (i < POST_COUNT) {
                writer.write(values + ",\n");
            } else {
                writer.write(values + ";\n\n");
            }

            // 진행률 출력
            if (i % 100 == 0) {
                System.out.print("\r  통계 진행률: " + i + "/" + POST_COUNT);
            }
        }
        System.out.print("\r  통계 진행률: " + POST_COUNT + "/" + POST_COUNT + " ✓\n");
    }

    /**
     * 댓글 데이터 생성
     */
    private static void generateComments(BufferedWriter writer) throws IOException {
        writer.write("-- =============================================\n");
        writer.write("-- 4. Comments (게시글당 " + MIN_COMMENTS_PER_POST + "~" + MAX_COMMENTS_PER_POST + "개)\n");
        writer.write("-- =============================================\n");
        writer.write("INSERT INTO comments (comment_content, created_at, updated_at, comment_status, post_id, user_id) VALUES\n");

        int totalComments = 0;
        boolean isFirst = true;

        for (int postId = 1; postId <= POST_COUNT; postId++) {
            // 게시글당 댓글 개수 (10~15개 랜덤)
            int commentCount = random.nextInt(MAX_COMMENTS_PER_POST - MIN_COMMENTS_PER_POST + 1) + MIN_COMMENTS_PER_POST;

            for (int j = 0; j < commentCount; j++) {
                // 댓글 내용 (200자 제한)
                String content = COMMENT_TEMPLATES[random.nextInt(COMMENT_TEMPLATES.length)];

                // 가끔 더 긴 댓글 생성
                if (random.nextInt(5) == 0) {
                    content = content + " " + COMMENT_TEMPLATES[random.nextInt(COMMENT_TEMPLATES.length)];
                }

                if (content.length() > 200) {
                    content = content.substring(0, 200);
                }

                // 작성자 (랜덤 user_id)
                int userId = random.nextInt(USER_COUNT) + 1;

                // 생성 날짜 (최근 90일 내 랜덤)
                LocalDateTime createdAt = randomDateTime(90);

                // SQL 생성
                String values = String.format("('%s', '%s', '%s', 'ACTIVE', %d, %d)",
                        escape(content),
                        createdAt.format(TIMESTAMP_FORMAT),
                        createdAt.format(TIMESTAMP_FORMAT),
                        postId,
                        userId
                );

                if (!isFirst) {
                    writer.write(",\n");
                }
                writer.write(values);
                isFirst = false;

                totalComments++;

                // 진행률 출력 (1000개마다)
                if (totalComments % 1000 == 0) {
                    System.out.print("\r  진행률: " + totalComments + "개 생성됨...");
                }
            }
        }

        writer.write(";\n\n");
        System.out.print("\r  진행률: " + totalComments + "개 생성됨 ✓\n");
    }

    /**
     * 좋아요 데이터 생성 (post_id=3000에 모든 유저)
     */
    private static void generateLikes(BufferedWriter writer) throws IOException {
        writer.write("-- =============================================\n");
        writer.write("-- 5. Post Likes (" + USER_COUNT + " rows - post_id=" + SPECIAL_POST_ID + ")\n");
        writer.write("-- =============================================\n");
        writer.write("INSERT INTO post_likes (user_id, post_id, created_at) VALUES\n");

        for (int userId = 1; userId <= USER_COUNT; userId++) {
            // 생성 날짜 (최근 30일 내 랜덤)
            LocalDateTime createdAt = randomDateTime(30);

            String values = String.format("(%d, %d, '%s')",
                    userId,
                    SPECIAL_POST_ID,
                    createdAt.format(TIMESTAMP_FORMAT)
            );

            if (userId < USER_COUNT) {
                writer.write(values + ",\n");
            } else {
                writer.write(values + ";\n\n");
            }

            // 진행률 출력
            if (userId % 100 == 0) {
                System.out.print("\r  진행률: " + userId + "/" + USER_COUNT);
            }
        }
        System.out.print("\r  진행률: " + USER_COUNT + "/" + USER_COUNT + " ✓\n");
    }

    /**
     * SQL 특수문자 이스케이프
     */
    private static String escape(String str) {
        if (str == null) {
            return "";
        }
        return str
                .replace("\\", "\\\\")  // \ -> \\
                .replace("'", "\\'")    // ' -> \'
                .replace("\"", "\\\"")  // " -> \"
                .replace("\n", "\\n")   // 개행 -> \n
                .replace("\r", "\\r");  // 캐리지 리턴 -> \r
    }

    /**
     * 랜덤 날짜/시간 생성 (과거 N일 이내)
     */
    private static LocalDateTime randomDateTime(int daysAgo) {
        long now = System.currentTimeMillis();
        long daysAgoMillis = (long) daysAgo * 24 * 60 * 60 * 1000;
        long randomTime = ThreadLocalRandom.current().nextLong(now - daysAgoMillis, now);

        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(randomTime),
                java.time.ZoneId.systemDefault()
        );
    }
}
