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
        // Graceful Shutdown: 종료 시 큐에 쌓인 작업까지 모두 완료될 때까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        return executor
    }
}
