package com.mysite.knitly.global.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 락 획득 시도 (3초간 유효)
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String key) {
        // SET key "locked" NX PX 3000
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, "locked", Duration.ofSeconds(3))
        );
    }

    /**
     * 락 해제
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}