package com.mysite.knitly.global.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- Like 관련 상수 ---
    public static final String LIKE_ADD_QUEUE = "like.add.queue";
    public static final String LIKE_DELETE_QUEUE = "like.delete.queue";
    public static final String LIKE_ADD_DLQ = "like.add.dlq";
    public static final String LIKE_DELETE_DLQ = "like.delete.dlq";

    public static final String LIKE_EXCHANGE = "like.exchange";
    public static final String LIKE_ADD_ROUTING_KEY = "like.add.routingkey";
    public static final String LIKE_DELETE_ROUTING_KEY = "like.delete.routingkey";

    public static final String DEAD_LETTER_EXCHANGE = "dead-letter.exchange";
    public static final String DEAD_LETTER_ROUTING_KEY_PREFIX = "dead."; // 라우팅 키 접두사

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public TopicExchange likeExchange() {
        return new TopicExchange(LIKE_EXCHANGE);
    }

    @Bean
    public Queue likeAddQueue() {
        return QueueBuilder.durable(LIKE_ADD_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY_PREFIX + LIKE_ADD_QUEUE)
                .build();
    }

    @Bean
    public Queue likeDeleteQueue() {
        return QueueBuilder.durable(LIKE_DELETE_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY_PREFIX + LIKE_DELETE_QUEUE)
                .build();
    }

    @Bean
    public Queue likeAddDeadLetterQueue() {
        return QueueBuilder.durable(LIKE_ADD_DLQ).build();
    }

    @Bean
    public Queue likeDeleteDeadLetterQueue() {
        return QueueBuilder.durable(LIKE_DELETE_DLQ).build();
    }

    // Binding
    @Bean
    public Binding likeAddBinding(Queue likeAddQueue, TopicExchange likeExchange) {
        return BindingBuilder.bind(likeAddQueue).to(likeExchange).with(LIKE_ADD_ROUTING_KEY);
    }

    @Bean
    public Binding likeDeleteBinding(Queue likeDeleteQueue, TopicExchange likeExchange) {
        return BindingBuilder.bind(likeDeleteQueue).to(likeExchange).with(LIKE_DELETE_ROUTING_KEY);
    }

    // DLQ Binding
    @Bean
    public Binding likeAddDlqBinding(Queue likeAddDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(likeAddDeadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY_PREFIX + LIKE_ADD_QUEUE);
    }

    @Bean
    public Binding likeDeleteDlqBinding(Queue likeDeleteDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(likeDeleteDeadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY_PREFIX + LIKE_DELETE_QUEUE);
    }


    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange");
    }

    @Bean
    public Queue orderEmailQueue() {
        return QueueBuilder.durable("order.email.queue")
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY_PREFIX + "order.email.queue")
                .build();
    }

    @Bean
    public Queue orderEmailDeadLetterQueue() {
        return QueueBuilder.durable("order.email.queue.dlq").build();
    }

    @Bean
    public Binding orderEmailBinding(Queue orderEmailQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderEmailQueue).to(orderExchange).with("order.completed");
    }

    @Bean
    public Binding orderEmailDlqBinding(Queue orderEmailDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderEmailDeadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY_PREFIX + "order.email.queue");
    }

    // JSON 메시지 컨버터
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate에 JSON 컨버터 적용
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

}