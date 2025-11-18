package com.mysite.knitly.domain.community.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class CommunityStaticResourceConfig(

    @Value("\${file.community.upload-dir:\${user.dir}/uploads}")
    private var uploadDir: String,

    @Value("\${file.community.upload.sub-dir:communitys}")
    private var subDir: String,

    @Value("\${file.community.public-prefix:/uploads/communitys}")
    private var publicPrefix: String
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        var root = Paths.get(uploadDir)
        if (!root.isAbsolute) {
            root = Paths.get(System.getProperty("user.dir")).resolve(uploadDir)
        }

        val communityDir = root.resolve(subDir).toAbsolutePath().normalize().toString()

        registry.addResourceHandler("$publicPrefix/**")
            .addResourceLocations("file:$communityDir/")
            .setCachePeriod(0)

        println("[CommunityStaticResourceConfig] mapped $publicPrefix \u2192 $communityDir")
    }
}
