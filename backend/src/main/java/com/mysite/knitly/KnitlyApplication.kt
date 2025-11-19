package com.mysite.knitly

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
// @ConfigurationPropertiesScan 역할 : @ConfigurationProperties가 붙은 모든 클래스를 자동으로 스캔하여
// Spring IoC 컨테이너의 빈(Bean)으로 등록하고,
// 외부 설정 파일(properties 또는 YAML)의 값과 바인딩하도록 활성화
@ConfigurationPropertiesScan
class KnitlyApplication

// 자바와 달리 main 함수를 클래스 밖으로 뺍니다 (Top-level function)
fun main(args: Array<String>) {
    // SpringApplication.run(...) 대신 코틀린 전용 확장 함수인 runApplication을 사용합니다.
    // 제네릭<T>을 통해 클래스 타입을 명시하고, 가변 인자(*args)를 전달합니다.
    runApplication<KnitlyApplication>(*args)
}