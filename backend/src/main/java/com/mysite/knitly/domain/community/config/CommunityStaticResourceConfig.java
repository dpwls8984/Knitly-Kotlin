package com.mysite.knitly.domain.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * 커뮤니티 이미지 정적 리소스 매핑 설정
 * - 실제 저장 경로: ${file.community.upload-dir}/${file.community.upload.sub-dir}/yyyy/MM/dd
 * - 접근 URL: ${file.community.public-prefix}/...
 */
@Configuration
public class CommunityStaticResourceConfig implements WebMvcConfigurer {

    // 커뮤니티 전용 업로드 설정
    @Value("${file.community.upload-dir:${user.dir}/uploads}")
    private String uploadDir;

    @Value("${file.community.upload.sub-dir:communitys}")
    private String subDir;

    @Value("${file.community.public-prefix:/uploads/communitys}")
    private String publicPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // base upload dir 기준으로 실제 저장 경로 계산
        Path root = Paths.get(uploadDir);
        if (!root.isAbsolute()) {
            root = Paths.get(System.getProperty("user.dir")).resolve(uploadDir);
        }

        String communityDir = root.resolve(subDir).toAbsolutePath().normalize().toString();

        // 정적 리소스 매핑
        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations("file:" + communityDir + "/")
                .setCachePeriod(0);

        // 콘솔 확인용 로그 (선택)
        System.out.println("[CommunityStaticResourceConfig] mapped " + publicPrefix + " → " + communityDir);
    }
}
