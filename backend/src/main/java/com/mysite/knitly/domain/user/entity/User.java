package com.mysite.knitly.domain.user.entity;

import com.mysite.knitly.domain.userstore.entity.UserStore;
import com.mysite.knitly.global.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId; // knitly 서비스 내에서의 키값

    @Column(nullable = false, unique = true)
    private String socialId; // 구글의 고유 ID (sub)

    @Column(nullable = false)
    private String email; // 구글 이메일

    @Column(nullable = false, length = 50)
    private String name; // 구글에서 받아온 이름

    // UserStore와 1:1 양방향 관계
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserStore userStore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider; // GOOGLE

    // 정적 팩토리 메서드
    public static User createGoogleUser(String socialId, String email, String name) {
        return User.builder()
                .socialId(socialId)
                .email(email)
                .name(name)
                .provider(Provider.GOOGLE)
                .build();
    }

    // UserStore 초기화 메서드
    @PostPersist
    public void initializeUserStore() {
        if (this.userStore == null) {
            this.userStore = UserStore.builder()
                    .user(this)
                    .storeDetail("안녕하세요! 제 스토어에 오신 것을 환영합니다.")
                    .build();
        }
    }
}

//CREATE TABLE `users` (
//        `user_id`	BIGINT	NOT NULL,
//        `social_id`	VARCHAR(255)	NOT NULL,
//	`name`	VARCHAR(50)	NOT NULL,
//	`provider`	ENUM('KAKAO', 'GOOGLE')	NOT NULL,
//	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP
//);
