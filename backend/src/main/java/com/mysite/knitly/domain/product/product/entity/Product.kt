package com.mysite.knitly.domain.product.product.entity

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import jakarta.persistence.*
import lombok.AccessLevel
import lombok.NoArgsConstructor
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "products")
class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val productId: Long? = null

    @Column(nullable = false, length = 30)
    lateinit var title: String

    @Column(nullable = false, columnDefinition = "TEXT")
    lateinit var description: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var productCategory: ProductCategory

    @Column(nullable = false)
    lateinit var sizeInfo: String

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
    var price: Double = 0.0

    @Column(nullable = false)
    @CreatedDate
    lateinit var createdAt: LocalDateTime

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @Column(nullable = false)
    var purchaseCount: Int = 0

    @Column(nullable = false)
    var isDeleted: Boolean = false

    @Column
    var stockQuantity: Int? = null

    @Column(nullable = false)
    var likeCount: Int = 0

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    lateinit var design: Design

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val productImages: MutableList<ProductImage> = mutableListOf()

    @Column
    var avgReviewRating: Double? = null

    @Column
    var reviewCount: Int? = null

    fun update(description: String, productCategory: ProductCategory, sizeInfo: String, stockQuantity: Int?) {
        this.description = description
        this.productCategory = productCategory
        this.sizeInfo = sizeInfo
        this.stockQuantity = stockQuantity
    }

    fun softDelete() {
        this.isDeleted = true
    }

    //재판매를 위한 메서드
    fun relist() {
        if (!this.isDeleted) {
            throw ServiceException(ErrorCode.DESIGN_NOT_STOPPED)
        }
        this.isDeleted = false
    }

    //재고 수량 감소 메서드
    fun decreaseStock(quantity: Int) {
        val currentStock = this.stockQuantity ?: return

        val restStock = currentStock - quantity
        if (restStock < 0) {
            throw ServiceException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT)
        }
        this.stockQuantity = restStock
    }

    // 상품 이미지 설정 메서드
    fun addProductImages(images: List<ProductImage>?) {
        productImages.clear()
        images?.forEach {
            productImages.add(it)
            it.setProduct(this)
        }
    }

    fun increaseLikeCount() {
        this.likeCount += 1
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount -= 1
        } else {
            this.likeCount = 0
        }
    }

    fun setReviewCount(reviewCount: Int?) {
        this.reviewCount = reviewCount
    }

    fun increasePurchaseCount() {
        this.purchaseCount += 1
    }

    fun increasePurchaseCount(quantity: Int) {
        this.purchaseCount += quantity
    }
}
