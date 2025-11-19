package com.mysite.knitly.domain.order.entity

import com.mysite.knitly.domain.product.product.entity.Product
import jakarta.persistence.*

@Entity
class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(nullable = false)
    val orderPrice: Double,

    @Column(nullable = false)
    val quantity: Int
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderItemId: Long? = null

    // Order 생성 시점에 addOrderItem을 통해 할당되므로 var 필요.
    // 하지만 null로 시작하는 것보다, 생성자에서 못 받는 상황(순환참조)이므로
    // nullable var로 두고 관리하거나 lateinit을 씁니다.
    // JPA 스펙상 nullable var가 가장 안전합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var order: Order? = null
        protected set

    fun assignOrder(order: Order) {
        if (this.order != null) {
            throw IllegalStateException("이미 주문이 할당되었습니다.")
        }
        this.order = order
    }
}