package com.mysite.knitly.domain.product.product.entity

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener::class)
open class Product (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val productId: Long? = null,

    @Column(nullable = false, length = 30)
    val title: String,

    description: String,
    productCategory: ProductCategory,
    sizeInfo: String,

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
    val price: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    purchaseCount: Int = 0,
    isDeleted: Boolean = false,
    stockQuantity: Int? = null,
    likeCount: Int = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    val design: Design

) {
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = description
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var productCategory: ProductCategory = productCategory
        private set

    @Column(nullable = false)
    var sizeInfo: String = sizeInfo
        private set

    @Column(nullable = false)
    @CreatedDate
    lateinit var createdAt: LocalDateTime
        private set

    @Column(nullable = false)
    var purchaseCount: Int = purchaseCount
        private set

    @Column(nullable = false)
    var isDeleted: Boolean = isDeleted
        private set

    @Column
    var stockQuantity: Int? = stockQuantity
        private set

    @Column(nullable = false)
    var likeCount: Int = likeCount
        private set

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _productImages: MutableList<ProductImage> = mutableListOf()
    val productImages: List<ProductImage>
        get() = _productImages.toList()

    @Column
    var avgReviewRating: Double? = null

    @Column
    var reviewCount: Int? = null

    //상품 수정하는 로직 추가
    fun update(
        description: String?,
        productCategory: ProductCategory?,
        sizeInfo: String?,
        stockQuantity: Int?
    ) {
        if (description != null) {
            this.description = description
        }
        if (productCategory != null) {
            this.productCategory = productCategory
        }
        if (sizeInfo != null) {
            this.sizeInfo = sizeInfo
        }
        this.stockQuantity = stockQuantity
    }

    fun softDelete() {
        this.isDeleted = true
    }

    fun relist() {
        if (!this.isDeleted) {
            throw ServiceException(ErrorCode.DESIGN_NOT_STOPPED)
        }
        this.isDeleted = false
    }

    //재고 수량 감소 메서드 추가
    fun decreaseStock(quantity: Int) {
        // 1. 상시 판매 상품(재고가 null)인 경우는 로직을 실행하지 않음
        val currentStock = this.stockQuantity ?: return

        // 2. 남은 재고보다 많은 수량을 주문하면 예외 발생
        val restStock = currentStock - quantity
        if (restStock < 0) {
            throw ServiceException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT)
        }
        this.stockQuantity = restStock
    }

    // 상품 이미지 설정 메서드
    fun addProductImages(images: List<ProductImage>?) {
        _productImages.clear()
        images?.forEach { image ->
            _productImages.add(image)
            image.product = this  // setter 필요 없음 (private set 가능)
        }
    }

    fun increaseLikeCount() {
        this.likeCount += 1
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount -= 1
        }
    }

    // 구매 횟수 증가
    fun increasePurchaseCount() {
        this.purchaseCount += 1
    }

    // 구매 횟수 증가 (수량 지정)
    fun increasePurchaseCount(quantity: Int) {
        this.purchaseCount += quantity
    }


} //CREATE TABLE `products` (
//        `product_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `title`	VARCHAR(30)	NOT NULL,
//	`description`	TEXT	NOT NULL,
//        `product_category`	ENUM('TOP', 'BOTTOM', 'OUTER', 'BAG', 'ETC')	NOT NULL	COMMENT '상의, 하의, 아우터, 가방, 기타',
//        `size_info`	VARCHAR(255)	NOT NULL,
//	`price`	DECIMAL(10,2)	NOT NULL	COMMENT '무료 구분',
//        `created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
//        `user_id`	BIGINT	NOT NULL,
//        `purchase_count`	INT	NOT NULL	DEFAULT 0	COMMENT '누적수 분리?',
//        `is_deleted`	BOOLEAN	NOT NULL	DEFAULT FALSE	COMMENT '소프트 딜리트',
//        `stock_quantity`	INT	NULL	COMMENT 'null 이면 상시 판매 / 0~숫자 는 한정판매',
//        `like_count`	INT	NOT NULL	DEFAULT 0,
//        `design_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `avg_review_rating`	DECIMAL(3,2)	NULL
//);
