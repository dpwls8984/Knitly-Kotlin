package com.mysite.knitly.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import redis.embedded.RedisServer


/**
 * 테스트용 Embedded Redis 설정
 *
 * 통합 테스트 시 실제 Redis 서버 없이 In-Memory Redis를 사용
 *
 * @Profile("test") - 테스트 환경에서만 활성화
 */
@TestConfiguration
@Profile("test")
class EmbeddedRedisConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${spring.data.redis.port:6379}")
    private var redisPort: Int = 6379

    private var redisServer: RedisServer? = null

    @PostConstruct
    fun startRedis() {
        try {
            // Embedded Redis 서버 시작
            redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 128M") // 메모리 제한
                .build()

            redisServer?.start()
            log.info("✅ Embedded Redis started successfully on port: $redisPort")
        } catch (e: Exception) {
            log.warn("⚠️ Failed to start Embedded Redis: ${e.message}")
            log.warn("테스트는 기존 Redis 서버를 사용하여 계속 진행됩니다.")
            // 이미 다른 프로세스가 해당 포트를 사용 중일 수 있음
            // 테스트는 계속 진행 (실제 Redis 사용)
        }
    }

    @PreDestroy
    fun stopRedis() {
        try {
            redisServer?.stop()
            log.info("✅ Embedded Redis stopped successfully")
        } catch (e: Exception) {
            log.warn("⚠️ Failed to stop Embedded Redis: ${e.message}")
        }
    }

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory("localhost", redisPort)
    }
}