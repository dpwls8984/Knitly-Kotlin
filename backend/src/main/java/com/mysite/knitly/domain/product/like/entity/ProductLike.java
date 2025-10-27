package com.mysite.knitly.domain.product.like.entity;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_likes")
@IdClass(ProductLikeId.class) // 복합 키 클래스
public class ProductLike {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}