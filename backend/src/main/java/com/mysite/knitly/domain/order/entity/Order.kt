package com.mysite.knitly.domain.order.entity

import com.mysite.knitly.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*
@Table(name = "orders")
@Entity
@EntityListeners(AuditingEntityListener::class)
class Order(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(nullable = false)
    var totalPrice: Double = 0.0,

    @Column(nullable = false, unique = true, length = 64)
    var tossOrderId: String

) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderId: Long? = null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderItems: MutableList<OrderItem> = mutableListOf()

    // 연관관계 편의 메서드
    fun addOrderItem(orderItem: OrderItem) {
        orderItems.add(orderItem)
        orderItem.order = this
        totalPrice += orderItem.orderPrice * orderItem.quantity
    }

    companion object {

        fun create(user: User, items: List<OrderItem>): Order {
            val order = Order(
                user = user,
                totalPrice = 0.0,
                tossOrderId = generateTossOrderId()
            )

            items.forEach { order.addOrderItem(it) }

            return order
        }

        private fun generateTossOrderId(): String =
            UUID.randomUUID().toString()
    }

    protected constructor() : this(
        user = null,
        totalPrice = 0.0,
        tossOrderId = ""
    )
}
