package com.mysite.knitly.global.lock

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class RedisLockService(
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private val LOCK_TTL = Duration.ofSeconds(10)

        // Lua 스크립트: key의 value가 본인 것일 때만 삭제
        private val UNLOCK_SCRIPT = DefaultRedisScript<Long>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """.trimIndent(),
            Long::class.java
        )
    }

    /**
     * 락 획득 시도
     * @return 락 소유자 식별용 UUID (획득 실패 시 null)
     */
    fun tryLock(key: String): String? {
        val lockValue = UUID.randomUUID().toString()
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, lockValue, LOCK_TTL) == true
        return if (acquired) lockValue else null
    }

    /**
     * 락 해제 (본인이 획득한 락만 삭제)
     * @param lockValue tryLock()에서 반환받은 UUID
     */
    fun unlock(key: String, lockValue: String) {
        redisTemplate.execute(UNLOCK_SCRIPT, listOf(key), lockValue)
    }
}
