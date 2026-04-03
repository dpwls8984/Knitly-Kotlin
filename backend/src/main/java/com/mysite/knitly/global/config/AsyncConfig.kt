package com.mysite.knitly.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestTemplate
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    @Bean("emailExecutor")
    fun emailExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("email-")
        executor.initialize()
        return executor
    }
}
