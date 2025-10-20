package com.mysite.knitly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
// @ConfigurationPropertiesScan 역할 : @ConfigurationProperties가 붙은 모든 클래스를 자동으로 스캔하여
// Spring IoC 컨테이너의 빈(Bean)으로 등록하고,
// 외부 설정 파일(properties 또는 YAML)의 값과 바인딩하도록 활성화
@ConfigurationPropertiesScan
public class KnitlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnitlyApplication.class, args);
    }

}
