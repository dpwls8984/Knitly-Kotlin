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