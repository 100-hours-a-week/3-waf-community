package com.ktb.community.enums;

/**
 * 사용자 상태 열거형
 * DDL: user_status ENUM('ACTIVE', 'INACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE'
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}
