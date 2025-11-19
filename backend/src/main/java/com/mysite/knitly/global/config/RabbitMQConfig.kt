package com.mysite.knitly.global.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    @Bean
    fun deadLetterExchange() = DirectExchange(DEAD_LETTER_EXCHANGE)

    @Bean
    fun likeExchange() = TopicExchange(LIKE_EXCHANGE)

    @Bean
    fun orderExchange() = TopicExchange("order.exchange")

    @Bean
    fun likeAddQueue() =
        durableQueueWithDLQ(LIKE_ADD_QUEUE)

    @Bean
    fun likeDeleteQueue() =
        durableQueueWithDLQ(LIKE_DELETE_QUEUE)

    @Bean
    fun likeAddDeadLetterQueue() = dlq(LIKE_ADD_DLQ)

    @Bean
    fun likeDeleteDeadLetterQueue() = dlq(LIKE_DELETE_DLQ)

    @Bean
    fun orderEmailQueue() =
        durableQueueWithDLQ("order.email.queue")

    @Bean
    fun orderEmailDeadLetterQueue() = dlq("order.email.queue.dlq")

    // Binding
    @Bean
    fun likeAddBinding(
        @Qualifier("likeAddQueue") queue: Queue,
        @Qualifier("likeExchange") exchange: TopicExchange
    ) = BindingBuilder.bind(queue).to(exchange).with(LIKE_ADD_ROUTING_KEY)

    @Bean
    fun likeDeleteBinding(
        @Qualifier("likeDeleteQueue") queue: Queue,
        @Qualifier("likeExchange") exchange: TopicExchange
    ) = BindingBuilder.bind(queue).to(exchange).with(LIKE_DELETE_ROUTING_KEY)

    @Bean
    fun likeAddDlqBinding(
        @Qualifier("likeAddDeadLetterQueue") dlq: Queue,
        @Qualifier("deadLetterExchange") deadEx: DirectExchange
    ) = BindingBuilder.bind(dlq).to(deadEx)
        .with("$DEAD_LETTER_ROUTING_KEY_PREFIX$LIKE_ADD_QUEUE")

    @Bean
    fun likeDeleteDlqBinding(
        @Qualifier("likeDeleteDeadLetterQueue") dlq: Queue,
        @Qualifier("deadLetterExchange") deadEx: DirectExchange
    ) = BindingBuilder.bind(dlq).to(deadEx)
        .with("$DEAD_LETTER_ROUTING_KEY_PREFIX$LIKE_DELETE_QUEUE")

    @Bean
    fun orderEmailBinding(
        @Qualifier("orderEmailQueue") queue: Queue,
        @Qualifier("orderExchange") exchange: TopicExchange
    ) = BindingBuilder.bind(queue).to(exchange).with("order.completed")

    @Bean
    fun orderEmailDlqBinding(
        @Qualifier("orderEmailDeadLetterQueue") dlq: Queue,
        @Qualifier("deadLetterExchange") deadEx: DirectExchange
    ) = BindingBuilder.bind(dlq)
        .to(deadEx)
        .with("${DEAD_LETTER_ROUTING_KEY_PREFIX}order.email.queue")


    // JSON 메시지 컨버터
    @Bean
    fun jackson2JsonMessageConverter() = Jackson2JsonMessageConverter()


    // RabbitTemplate에 JSON 컨버터 적용
    @Bean
    fun rabbitTemplate(
        factory: ConnectionFactory,
        converter: Jackson2JsonMessageConverter
    ) = RabbitTemplate(factory).apply {
        messageConverter = converter
    }

    // 공통 함수들
    private fun durableQueueWithDLQ(queueName: String) =
        QueueBuilder.durable(queueName)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "$DEAD_LETTER_ROUTING_KEY_PREFIX$queueName")
            .build()

    private fun dlq(name: String) = QueueBuilder.durable(name).build()

    companion object {
        // --- Like 관련 상수 ---
        const val LIKE_ADD_QUEUE: String = "like.add.queue"
        const val LIKE_DELETE_QUEUE: String = "like.delete.queue"
        const val LIKE_ADD_DLQ: String = "like.add.dlq"
        const val LIKE_DELETE_DLQ: String = "like.delete.dlq"

        const val LIKE_EXCHANGE: String = "like.exchange"
        const val LIKE_ADD_ROUTING_KEY: String = "like.add.routingkey"
        const val LIKE_DELETE_ROUTING_KEY: String = "like.delete.routingkey"

        const val DEAD_LETTER_EXCHANGE: String = "dead-letter.exchange"
        const val DEAD_LETTER_ROUTING_KEY_PREFIX: String = "dead." // 라우팅 키 접두사
    }
}