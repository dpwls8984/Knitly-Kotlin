package com.mysite.knitly.domain.product.review.entity

import jakarta.persistence.*

@Entity
@Table(name = "review_images")
open class ReviewImage(
    @Column(name = "review_image_url")
    var reviewImageUrl: String?,

    @Column(name = "sort_order")
    var sortOrder: Int = 0

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val reviewImageId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    lateinit var review: Review
}