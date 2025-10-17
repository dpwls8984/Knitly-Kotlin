package com.mysite.knitly.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {
    @Value("${file.upload-dir:uploads/designs}")
    private String uploadDir;

    @Value("${file.public-prefix:/files}")
    private String publicPrefix;

    //로컬에 저장된 파일을 HTTP로 접근 가능하게 설정
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + base.toString() + "/";
        String pattern = publicPrefix.endsWith("/**") ? publicPrefix : publicPrefix + "/**";

        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}
