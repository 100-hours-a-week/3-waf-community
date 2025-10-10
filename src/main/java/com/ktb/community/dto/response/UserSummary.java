package com.ktb.community.dto.response;

import com.ktb.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 요약 정보 DTO (Nested)
 * PostResponse, CommentResponse에서 작성자 정보로 사용
 */
@Getter
@Builder
@AllArgsConstructor
public class UserSummary {

    private Long userId;
    private String nickname;
    private String profileImage;  // image_url (nullable)

    /**
     * Entity → DTO 변환
     *
     * @param user User 엔티티
     * @return UserSummary DTO
     */
    public static UserSummary from(User user) {
        return UserSummary.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage() != null ?
                        user.getProfileImage().getImageUrl() : null)
                .build();
    }
}
