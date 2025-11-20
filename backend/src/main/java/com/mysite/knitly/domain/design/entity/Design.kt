package com.mysite.knitly.domain.design.entity

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "designs")
@EntityListeners(AuditingEntityListener::class)
class Design(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val designId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column
    var pdfUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var designState: DesignState,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val designType: DesignType? = null,

    @Column(nullable = false, length = 30)
    val designName: String,

    @Column(name = "grid_data", columnDefinition = "JSON", nullable = false)
    val gridData: String,
    ) {

        @OneToOne(mappedBy = "design", cascade = [CascadeType.REMOVE])
        var product: Product? = null
          private set

        @CreatedDate
        @Column(nullable = false, updatable = false)
        var createdAt: LocalDateTime? = null
            private set

        fun assignProduct(product: Product) {
            this.product = product
        }

        // 삭제 가능 여부 확인 - BEFORE_SALE 상태인 경우에만 삭제 가능
        fun isDeletable(): Boolean = designState == DesignState.BEFORE_SALE

        // 도안 작성자 확인 - userId 비교
        fun isOwnedBy(userId: Long): Boolean = user.userId == userId

        // 최초 판매 시작 메서드
        // 오직 BEFORE_SALE 상태에서만 호출 가능
        // 판매 전 -> 판매 중으로 변경
        fun startSale() {
            if (designState != DesignState.BEFORE_SALE) {
                throw ServiceException(ErrorCode.DESIGN_ALREADY_ON_SALE)
            }
            designState = DesignState.ON_SALE
        }

        // 판매 중 -> 판매 중지 메서드
        // 오직 ON_SALE 상태에서만 호출 가능
        fun stopSale() {
            if (designState != DesignState.ON_SALE) {
                throw ServiceException(ErrorCode.DESIGN_NOT_ON_SALE)
            }
            designState = DesignState.STOPPED
        }

        // 판매 중지 -> 판매 재개 메서드
        // 오직 STOPPED 상태에서만 호출 가능
        fun relist() {
            if (designState != DesignState.STOPPED) {
                throw ServiceException(ErrorCode.DESIGN_NOT_STOPPED)
            }
            designState = DesignState.ON_SALE
        }
    }
