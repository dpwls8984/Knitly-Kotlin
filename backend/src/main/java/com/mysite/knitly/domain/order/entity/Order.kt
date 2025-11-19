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

    // 2. 변경이 필요한 상태값은 var로 선언하되, 외부 변경을 막기 위해 private set 사용
    @Column(nullable = false)
    var totalPrice: Double = 0.0
        private set

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        private set

    // MutableList를 내부에서만 쓰고, 밖으로는 Immutable List로 노출하는 것이 더 안전하지만,
    // JPA 편의상 MutableList를 유지하되 초기화 로직 개선
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
            // 생성자를 통해 필수값을 주입
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