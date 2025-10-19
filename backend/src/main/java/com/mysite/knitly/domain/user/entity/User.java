package com.mysite.knitly.domain.user.entity;

import com.mysite.knitly.domain.user.entity.enums.Provider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID userId; // knitly 서비스 내에서의 키값

    @Column(nullable = false, unique = true)
    private String socialId; // 구글의 고유 ID (sub)

    @Column(nullable = false)
    private String email; // 구글 이메일

    @Column(nullable = false, length = 50)
    private String name; // 구글에서 받아온 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider; // GOOGLE

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드
    public static User createGoogleUser(String socialId, String email, String name) {
        return User.builder()
                .socialId(socialId)
                .email(email)
                .name(name)
                .provider(Provider.GOOGLE)
                .build();
    }
}

//CREATE TABLE `users` (
//        `user_id`	BIGINT	NOT NULL,
//        `social_id`	VARCHAR(255)	NOT NULL,
//	`name`	VARCHAR(50)	NOT NULL,
//	`provider`	ENUM('KAKAO', 'GOOGLE')	NOT NULL,
//	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP
//);
