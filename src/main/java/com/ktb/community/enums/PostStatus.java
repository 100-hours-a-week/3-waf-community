package com.ktb.community.enums;

/**
 * 게시글 상태 열거형
 * DDL: post_status ENUM('ACTIVE', 'DELETED', 'DRAFT') NOT NULL DEFAULT 'ACTIVE'
 */
public enum PostStatus {
    ACTIVE,
    DELETED,
    DRAFT
}
