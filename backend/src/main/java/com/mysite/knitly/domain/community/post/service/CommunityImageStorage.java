package com.mysite.knitly.domain.community.post.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CommunityImageStorage {

    // 커뮤니티 전용 업로드 설정 (application.yml의 file.community 읽어서)
    @Value("${file.community.upload-dir:${user.dir}/uploads}")
    private String uploadDir;

    @Value("${file.community.upload.sub-dir:communitys}")
    private String subDir;

    @Value("${file.community.public-prefix:/uploads/communitys}")
    private String publicPrefix;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

    // uploadDir이 상대경로면 ${user.dir} 기준으로 보정
    private Path resolveBaseDir() {
        Path configured = Paths.get(uploadDir);
        if (configured.isAbsolute()) return configured.normalize();
        Path userDir = Paths.get(System.getProperty("user.dir"));
        return userDir.resolve(configured).normalize();
    }

    // 이미지 저장 후, 정적 접근 가능한 상대 URL 목록 반환
    public List<String> saveImages(List<MultipartFile> files) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        if (files == null || files.isEmpty()) return savedPaths;

        LocalDate today = LocalDate.now();
        Path targetDir = resolveBaseDir().resolve(Paths.get(
                subDir,
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth())
        ));

        Files.createDirectories(targetDir);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            validateFile(file);

            // [수정됨] UUID 대신 userId_타임스탬프_원본명 형식으로 변경
            String ext = getExtension(file.getOriginalFilename());
            String baseName = stripExtension(file.getOriginalFilename());
            String ts = LocalDateTime.now().format(TS);
            String newName = "guest_" + ts + "_" + sanitizeBaseName(baseName, 40) + ext;

            Path target = targetDir.resolve(newName);
            file.transferTo(target.toFile());

            String relativeUrl = publicPrefix +
                    "/" + today.getYear() + "/" +
                    String.format("%02d", today.getMonthValue()) + "/" +
                    String.format("%02d", today.getDayOfMonth()) + "/" +
                    newName;

            savedPaths.add(relativeUrl);
            log.info("[CommunityImageStorage] saved: {} -> {}", target, relativeUrl);
        }
        return savedPaths;
    }

    private void validateFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        long size = file.getSize();
        if (name == null || !name.matches("(?i).+\\.(png|jpg|jpeg|gif|webp)$")) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (png, jpg, jpeg, gif, webp)");
        }
        if (size > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지 파일 크기는 3MB 이하만 업로드 가능합니다.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : "";
    }

    // 확장자 제거 메서드
    private String stripExtension(String filename) {
        if (filename == null) return "file";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(0, dot) : filename;
    }

    // 파일명 안전화 (공백·특수문자 제거, 길이 제한)
    private String sanitizeBaseName(String base, int maxLen) {
        if (base == null || base.isBlank()) return "file";
        String cleaned = base
                .replaceAll("[\\s]+", "_")
                .replaceAll("[^0-9A-Za-z가-힣._-]", "_");
        if (cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen);
        }
        return cleaned;
    }
}
