package com.mysite.knitly.global.email.entity;

public enum EmailStatus {
    //PENDING: RabbitMQ에 아직 발행되지 않은 상태 (스케줄러가 처리할 대상)
    PENDING,

    //PUBLISHED: RabbitMQ에 성공적으로 발행된 상태 (더 이상 처리 안 함)
    PUBLISHED,

    //FAILED_TO_PUBLISH: RabbitMQ 발행에 최종 실패한 상태 (개발자 확인 필요)
    FAILED_TO_PUBLISH
}
