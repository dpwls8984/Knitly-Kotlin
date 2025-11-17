package com.mysite.knitly.global.email.entity

import jakarta.persistence.*
import lombok.AccessLevel
import lombok.NoArgsConstructor
import java.time.LocalDateTime

@Entity
@Table(name = "email_outbox")
class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Lob
    @Column(nullable = false)
    lateinit var payload: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EmailStatus = EmailStatus.PENDING

    @Column(nullable = false)
    var retryCount: Int = 0

    protected constructor()

    @Column(updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    var updatedAt: LocalDateTime = LocalDateTime.now()

    companion object {
        fun create(payload: String): EmailOutbox {
            val outbox = EmailOutbox()
            outbox.payload = payload
            return outbox
        }
    }

    fun markAsPublished() {
        status = EmailStatus.PUBLISHED
        updatedAt = LocalDateTime.now()
    }

    fun incrementRetryCount() {
        retryCount += 1
        updatedAt = LocalDateTime.now()
    }

    fun markAsFailedToPublish() {
        status = EmailStatus.FAILED_TO_PUBLISH
        updatedAt = LocalDateTime.now()
    }
}