package com.mysite.knitly.global.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LIKE_ADD_QUEUE = "like.add.queue";
    public static final String LIKE_DELETE_QUEUE = "like.delete.queue";
    public static final String LIKE_ADD_DLQ = "like.add.dlq";
    public static final String LIKE_DELETE_DLQ = "like.delete.dlq";

    public static final String LIKE_EXCHANGE = "like.exchange";
    public static final String LIKE_ADD_ROUTING_KEY = "like.add.routingkey";
    public static final String LIKE_DELETE_ROUTING_KEY = "like.delete.routingkey";

    // 메인 큐
    @Bean
    public Queue likeAddQueue() {
        return QueueBuilder.durable(LIKE_ADD_QUEUE)
                .withArgument("x-dead-letter-exchange", LIKE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", LIKE_ADD_DLQ)
                .build();
    }

    @Bean
    public Queue likeDeleteQueue() {
        return QueueBuilder.durable(LIKE_DELETE_QUEUE)
                .withArgument("x-dead-letter-exchange", LIKE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", LIKE_DELETE_DLQ)
                .build();
    }

    // DLQ
    @Bean
    public Queue likeAddDeadLetterQueue() {
        return QueueBuilder.durable(LIKE_ADD_DLQ).build();
    }

    @Bean
    public Queue likeDeleteDeadLetterQueue() {
        return QueueBuilder.durable(LIKE_DELETE_DLQ).build();
    }

    // Exchange
    @Bean
    public TopicExchange likeExchange() {
        return new TopicExchange(LIKE_EXCHANGE);
    }

    // Binding
    @Bean
    public Binding likeAddBinding() {
        return BindingBuilder.bind(likeAddQueue())
                .to(likeExchange())
                .with(LIKE_ADD_ROUTING_KEY);
    }

    @Bean
    public Binding likeDeleteBinding() {
        return BindingBuilder.bind(likeDeleteQueue())
                .to(likeExchange())
                .with(LIKE_DELETE_ROUTING_KEY);
    }

    // DLQ Binding
    @Bean
    public Binding likeAddDlqBinding() {
        return BindingBuilder.bind(likeAddDeadLetterQueue())
                .to(likeExchange())
                .with(LIKE_ADD_DLQ);
    }

    @Bean
    public Binding likeDeleteDlqBinding() {
        return BindingBuilder.bind(likeDeleteDeadLetterQueue())
                .to(likeExchange())
                .with(LIKE_DELETE_DLQ);
    }
}