package com.ktb.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티의 공통 시간 필드를 관리하는 추상 클래스
 * Spring Data JPA Auditing을 통해 생성/수정 시각 자동 관리
 *
 * @see AuditingEntityListener
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /**
     * 엔티티 생성 시각
     * 감사 리스너가 저장 시점에 자동 설정함
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 최종 수정 시각
     * 감사 리스너가 갱신 시점에 자동 갱신함
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
