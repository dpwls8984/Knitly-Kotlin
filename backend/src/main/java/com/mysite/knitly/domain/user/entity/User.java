package com.mysite.knitly.domain.user.entity;

import com.mysite.knitly.global.jpa.BaseTimeEntity;
import com.mysite.knitly.global.jpa.converter.UUIDBinaryConverter;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Convert(converter = UUIDBinaryConverter.class)
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String socialId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('KAKAO','GOOGLE')")
    private UserProvider provider;
}
