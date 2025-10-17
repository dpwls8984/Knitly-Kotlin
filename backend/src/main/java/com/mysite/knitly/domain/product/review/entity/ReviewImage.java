package com.mysite.knitly.domain.product.review.entity;

import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "review_images")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewImageId;

    private String reviewImageUrl;

    @Builder.Default
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    public void setReview(Review review) {
        this.review = review;
    }
}

//CREATE TABLE `review_images` (
//        `review_image_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `review_image_url`	VARCHAR(255)	NULL,
//	      `sort_order`	INT	NULL	DEFAULT 0,
//        `review_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT
//);