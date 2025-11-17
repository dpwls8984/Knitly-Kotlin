package com.mysite.knitly.domain.product.product.entity

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Getter
import lombok.NoArgsConstructor
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Getter
@NoArgsConstructor
@Table(name = "product_images")
@AllArgsConstructor
@Builder
@EntityListeners(
    AuditingEntityListener::class
)
class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var productImageId: Long? = null

    var productImageUrl: String? = null

    var sortOrder: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null

//    fun setProduct(product: Product?) {
//        this.product = product
//    }
}