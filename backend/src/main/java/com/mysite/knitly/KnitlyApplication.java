package com.mysite.knitly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KnitlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnitlyApplication.class, args);
    }

}
