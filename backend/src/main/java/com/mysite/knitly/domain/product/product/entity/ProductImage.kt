package com.mysite.knitly.domain.product.product.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_images")
open class ProductImage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val productImageId: Long? = null,

    var productImageUrl: String = "",

    var sortOrder: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,
)