package com.mysite.knitly.domain.product.product.entity

import jakarta.persistence.*
import lombok.AccessLevel
import lombok.NoArgsConstructor
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "product_images")
class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val productImageId: Long? = null

    lateinit var productImageUrl: String

    var sortOrder: Long = 0L
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    lateinit var product: Product
        private set

    fun setProduct(product: Product) {
        this.product = product
    }
}