package com.mysite.knitly.domain.product.like.entity

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.user.entity.User
import jakarta.persistence.*

@Entity
@Table(name = "product_likes")
@IdClass(ProductLikeId::class) // 복합 키 클래스
open class ProductLike(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Id
    @Column(name = "product_id")
    val productId: Long,

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: User,

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    val product: Product
)