package com.ktb.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * PostImage 복합키 클래스
 * @EmbeddedId 방식 사용
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostImageId implements Serializable {

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "image_id")
    private Long imageId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostImageId that = (PostImageId) o;
        return Objects.equals(postId, that.postId) && Objects.equals(imageId, that.imageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, imageId);
    }
}
