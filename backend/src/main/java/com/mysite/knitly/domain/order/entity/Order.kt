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
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true, length = 64)
    val tossOrderId: String

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderId: Long? = null

    @Column(nullable = false)
    var totalPrice: Double = 0.0
        private set

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        private set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderItems: MutableList<OrderItem> = mutableListOf()

    // 연관관계 편의 메서드
    fun addOrderItem(orderItem: OrderItem) {
        orderItems.add(orderItem)
        orderItem.assignOrder(this)
        this.totalPrice += orderItem.orderPrice * orderItem.quantity
    }

    companion object {
        fun create(user: User, items: List<OrderItem>): Order {
            val order = Order(
                user = user,
                tossOrderId = generateTossOrderId()
            )

            // 로직 수행
            items.forEach { order.addOrderItem(it) }
            return order
        }

        private fun generateTossOrderId(): String = UUID.randomUUID().toString()
    }
}