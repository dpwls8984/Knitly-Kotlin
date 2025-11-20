package com.mysite.knitly.global.email.repository

import com.mysite.knitly.global.email.entity.EmailOutbox
import com.mysite.knitly.global.email.entity.EmailStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface EmailOutboxRepository : JpaRepository<EmailOutbox, Long> {

    fun findByStatusAndRetryCountLessThan(
        status: EmailStatus,
        maxRetries: Int
    ): List<EmailOutbox>

    fun deleteByStatusInAndCreatedAtBefore(
        statuses: List<EmailStatus>,
        cutoffDate: LocalDateTime
    )
}