package com.mysite.knitly.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class FileStorageConfig(
    @Value("\${file.upload-dir:uploads/designs}")
    private val uploadDir: String,

    @Value("\${file.public-prefix:/files}")
    private val publicPrefix: String
) : WebMvcConfigurer {

    // 로컬에 저장된 파일을 HTTP로 접근 가능하게 설정
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // designs 핸들러
        val base = Paths.get(uploadDir).toAbsolutePath().normalize()
        val location = "file:${base}/"
        val pattern = when {
            publicPrefix.endsWith("/**") -> publicPrefix
            else -> "$publicPrefix/**"
        }

        registry.addResourceHandler(pattern)
            .addResourceLocations(location)

        // products 핸들러
        val productBasePath = Paths.get(uploadDir).parent.resolve("products").toAbsolutePath().normalize()
        val productLocation = "file:${productBasePath}/"

        registry.addResourceHandler("/products/**") // /products/ 로 시작하는 URL
            .addResourceLocations(productLocation)    // 실제 .../uploads/products/ 폴더에 매핑
    }
}