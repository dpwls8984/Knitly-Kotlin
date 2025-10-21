package com.mysite.knitly.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue likeAddQueue() {
        return new Queue("like.add.queue", true);
    }

    @Bean
    public Queue likeDeleteQueue() {
        return new Queue("like.delete.queue", true); // durable
    }
}