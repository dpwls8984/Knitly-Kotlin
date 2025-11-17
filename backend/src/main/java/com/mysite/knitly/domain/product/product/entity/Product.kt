package com.mysite.knitly.domain.product.product.entity

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import jakarta.persistence.*
import lombok.Builder

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.function.Consumer

@Entity
@Table(name = "products")
@EntityListeners(
    AuditingEntityListener::class
)
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var productId: Long? = null

    @Column(nullable = false, length = 30)
     var title: String? = null

    @Column(nullable = false, columnDefinition = "TEXT")
     var description: String? = null


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
     var productCategory: ProductCategory? = null // 'TOP', 'BOTTOM', 'OUTER', 'BAG', 'ETC'

    @Column(nullable = false)
     var sizeInfo: String? = null

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
     var price: Double? = null // DECIMAL(10,2)

    @Column(nullable = false)
    @CreatedDate
     var createdAt: LocalDateTime? = null // DATETIME

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
     var user: User? = null // 판매자

    @Column(nullable = false)
     var purchaseCount: Int? = null // 누적수

    @Column(nullable = false)
     var isDeleted: Boolean? = null // 소프트 딜리트

    @Column
     var stockQuantity: Int? = null // null 이면 상시 판매 / 0~숫자 는 한정판매

    @Column(nullable = false)
     var likeCount: Int? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
     var design: Design? = null

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    @Builder.Default
     val productImages: MutableList<ProductImage> = ArrayList()

    @Column
     var avgReviewRating: Double? = null // DECIMAL(3,2)

    @Column
     var reviewCount: Int? = null

    //상품 수정하는 로직 추가
    fun update(description: String?, productCategory: ProductCategory?, sizeInfo: String?, stockQuantity: Int?) {
        this.description = description
        this.productCategory = productCategory
        this.sizeInfo = sizeInfo
        this.stockQuantity = stockQuantity
    }

    //소프트 딜리트 로직 추가
    fun softDelete() {
        this.isDeleted = true
    }

    //재판매를 위한 메서드 (isDeleted 를 false 로 변경)
    fun relist() {
        if (!isDeleted!!) {
            throw ServiceException(ErrorCode.DESIGN_NOT_STOPPED)
        }
        this.isDeleted = false
    }

    //재고 수량 감소 메서드 추가
    fun decreaseStock(quantity: Int) {
        // 1. 상시 판매 상품(재고가 null)인 경우는 로직을 실행하지 않음
        if (this.stockQuantity == null) {
            return
        }

        // 2. 남은 재고보다 많은 수량을 주문하면 예외 발생
        val restStock = stockQuantity!! - quantity
        if (restStock < 0) {
            throw ServiceException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT)
        }

        // 3. 재고 차감
        this.stockQuantity = restStock
    }

    // 상품 이미지 설정 메서드
    fun addProductImages(images: List<ProductImage>?) {
        productImages.clear()
        if (images != null) {
            productImages.addAll(images)
            images.forEach(Consumer { image: ProductImage -> image.product = this }) // 양방향 연관관계 설정
        }
    }

    fun increaseLikeCount() {
        if (this.likeCount == null) {
            this.likeCount = 0
        }
        this.likeCount = likeCount!! + 1
    }

    fun decreaseLikeCount() {
        if (this.likeCount == null || likeCount!! <= 0) {
            this.likeCount = 0
        } else {
            this.likeCount = likeCount!! - 1
        }
    }

//    // 리뷰 개수 설정 메서드
//    fun setReviewCount(reviewCount: Int?) {
//        this.reviewCount = reviewCount
//    }

    // 구매 횟수 증가
    fun increasePurchaseCount() {
        if (this.purchaseCount == null) {
            this.purchaseCount = 0
        }
        this.purchaseCount = purchaseCount!! + 1
    }

    // 구매 횟수 증가 (수량 지정)
    fun increasePurchaseCount(quantity: Int) {
        if (this.purchaseCount == null) {
            this.purchaseCount = 0
        }
        this.purchaseCount = purchaseCount!! + quantity
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

