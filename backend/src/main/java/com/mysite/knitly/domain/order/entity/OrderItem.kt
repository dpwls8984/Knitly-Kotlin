package com.mysite.knitly.domain.order.entity

import com.mysite.knitly.domain.product.product.entity.Product
import jakarta.persistence.*

@Entity
class OrderItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var order: Order? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null,

    @Column(nullable = false)
    var orderPrice: Double = 0.0,

    @Column(nullable = false)
    var quantity: Int = 0

) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderItemId: Long? = null

    protected constructor() : this(null, null, 0.0, 0)
}

