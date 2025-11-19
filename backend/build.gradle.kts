import org.gradle.kotlin.dsl.implementation

plugins {
    java
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "com.mysite"
version = "0.0.1-SNAPSHOT"
description = "knitly"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation ("org.redisson:redisson-spring-boot-starter:3.27.2") // Redisson 라이브러리
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    compileOnly("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.apache.pdfbox:pdfbox:2.0.29") // PDF 변환 라이브러리
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("org.springframework.boot:spring-boot-starter-mail")   // Spring Email
    testImplementation("org.springframework.security:spring-security-test")

    // 날짜,시간 모듈 LocalDateTime
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // JWT 라이브러리 (필수)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    implementation("org.apache.pdfbox:pdfbox:2.0.29") // PDF 변환 라이브러리
    implementation("commons-codec:commons-codec:1.16.0")

    // Swagger/OpenAPI
    // SpringDoc OpenAPI (Swagger 3) - WebMVC 및 Swagger UI 포함
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.11")

    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation ("org.springframework.retry:spring-retry")
    implementation ("org.springframework:spring-aspects")

    implementation("me.paulschwarz:spring-dotenv:3.0.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    implementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}