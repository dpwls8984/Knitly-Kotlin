package com.mysite.knitly.global.lock

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisLockService(
    private val redisTemplate: StringRedisTemplate
) {

    fun tryLock(key: String): Boolean {
        return redisTemplate.opsForValue()
            .setIfAbsent(key, "locked", Duration.ofSeconds(3)) == true
    }

    fun unlock(key: String) {
        redisTemplate.delete(key)
    }
}