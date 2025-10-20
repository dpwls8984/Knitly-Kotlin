package com.mysite.knitly.domain.design.entity;

import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "designs")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long designId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DesignState designState;

    @Column(nullable = false, length = 30)
    private String designName;

    @Column(name = "grid_data", columnDefinition = "JSON", nullable = false)
    private String gridData;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 삭제 가능 여부 확인 - BEFORE_SALE 상태인 경우에만 삭제 가능
    public boolean isDeletable() {
        return this.designState == DesignState.BEFORE_SALE;
    }

    // 도안 작성자 확인 - userId 비교
    public boolean isOwnedBy(Long userId) {
        return this.user.getUserId().equals(userId);
    }
}


//CREATE TABLE `designs` (
//        `design_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `pdf_url`	VARCHAR(255)	NULL,
//	`design_state`	ENUM('ON_SALE', 'STOPPED', 'BEFORE_SALE')	NOT NULL	DEFAULT BEFORE_SALE,
//	`design_name`	VARCHAR(30)	NOT NULL
//);