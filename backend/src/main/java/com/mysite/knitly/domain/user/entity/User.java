package com.mysite.knitly.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false)
    private String socialId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "ENUM('KAKAO', 'GOOGLE')")
    private String provider; // 'GOOGLE'

    @Column(nullable = false)
    @CreatedDate
    private String createdAt;
}

//CREATE TABLE `users` (
//        `user_id`	BIGINT	NOT NULL,
//        `social_id`	VARCHAR(255)	NOT NULL,
//	`name`	VARCHAR(50)	NOT NULL,
//	`provider`	ENUM('KAKAO', 'GOOGLE')	NOT NULL,
//	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP
//);
