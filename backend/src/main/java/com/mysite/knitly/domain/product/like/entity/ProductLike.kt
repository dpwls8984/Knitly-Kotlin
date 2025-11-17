package com.mysite.knitly.domain.product.like.entity

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.user.entity.User
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Getter
import lombok.NoArgsConstructor

@Entity
@Table(name = "product_likes")
@IdClass(ProductLikeId::class) // 복합 키 클래스
open class ProductLike(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product
)