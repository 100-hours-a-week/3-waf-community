package com.ktb.community.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글-이미지 브릿지 엔티티 (M:N 관계)
 * DDL: post_images 테이블
 */
@Entity
@Table(name = "post_images",
       uniqueConstraints = @UniqueConstraint(name = "uq_post_images_order", columnNames = {"post_id", "display_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"post", "image"})
public class PostImage {

    @EmbeddedId
    private PostImageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("imageId")
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    @Builder
    public PostImage(Post post, Image image, Integer displayOrder) {
        this.id = new PostImageId(post.getPostId(), image.getImageId());
        this.post = post;
        this.image = image;
        this.displayOrder = displayOrder != null ? displayOrder : 1;
    }
}
