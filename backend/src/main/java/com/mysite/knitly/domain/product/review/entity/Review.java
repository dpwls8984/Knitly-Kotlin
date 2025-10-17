package com.mysite.knitly.domain.product.review.entity;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "reviews")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(nullable = false)
    private Integer rating; // TINYINT, 1~5 범위

    @Column(nullable = false, length = 300)
    private String content;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}

//CREATE TABLE `reviews` (
//        `review_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `rating`	TINYINT	NOT NULL	COMMENT 'CHECK(1 <= rating AND rating <= 5)',
//        `content`	VARCHAR(300)	NOT NULL,
//	      `created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
//        `product_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `user_id`	BIGINT	NOT NULL,
//        `is_deleted`	BOOLEAN	NOT NULL	DEFAULT FALSE	COMMENT '소프트 딜리트'
//);