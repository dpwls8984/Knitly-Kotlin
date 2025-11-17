package com.mysite.knitly.domain.product.review.entity

import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.user.entity.User
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Getter
import lombok.NoArgsConstructor
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.function.Consumer

@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener::class)
open class Review (
    @Column(nullable = false)
    var rating: Int,

    @Column(nullable = false, length = 300)
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    val orderItem: OrderItem,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val reviewId: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        private set

    @Column(nullable = false)
    var isDeleted: Boolean = false
        private set

    @OneToMany(mappedBy = "review", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reviewImages: MutableList<ReviewImage> = mutableListOf()

    fun softDelete() {
        this.isDeleted = true
    }

    fun updateReviewImages(images: List<ReviewImage>) {
        this.reviewImages.clear()
        this.reviewImages.addAll(images)
        images.forEach { it.review = this }
    }
}