package com.mysite.knitly.domain.design.util;

import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileStorage {

    @Value("${file.upload-dir:uploads/designs}")
    private String uploadDir;

    @Value("${file.public-prefix:/files}")
    private String publicPrefix;

    public String savePdfFile(byte[] fileData) {
        try {
            LocalDate today = LocalDate.now();

            // 업로드 디렉토리 생성
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dir = base.resolve(Paths.get(
                    String.valueOf(today.getYear()),
                    String.format("%02d", today.getMonthValue()),
                    String.format("%02d", today.getDayOfMonth())
            ));
            Files.createDirectories(dir);

            // 고유 파일명 생성
            String saved = UUID.randomUUID() + ".pdf";
            Path filePath = dir.resolve(saved);
            Files.write(filePath, fileData, StandardOpenOption.CREATE_NEW);

            // 접근 가능한 URL 생성
            String relativePath = String.join("/",
                    String.valueOf(today.getYear()),
                    String.format("%02d", today.getMonthValue()),
                    String.format("%02d", today.getDayOfMonth()),
                    saved
            );

            String url = String.join("/", publicPrefix, relativePath);

            log.info("PDF 저장 완료: {}", filePath);

            return url; // DB에는 접근 가능한 URL만 저장
        } catch (IOException e) {
            log.error("PDF 파일 저장 실패", e);
            throw new ServiceException(ErrorCode.DESIGN_FILE_SAVE_FAILED);
        }


    }

    // PDF URL에서 절대 경로 변환
    public Path toAbsolutePathFromUrl(String pdfUrl) {
        // pdfUrl에서 publicPrefix("/files") 제거 → 상대 경로
        String rel = pdfUrl.startsWith(publicPrefix) ? pdfUrl.substring(publicPrefix.length()) : pdfUrl;
        if (rel.startsWith("/")) rel = rel.substring(1);

        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(rel).normalize();
    }

    public void deleteFile(String fileUrl) throws IOException {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadDir).resolve(fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("파일 삭제 완료: {}", filePath);
            } else {
                log.warn("삭제할 파일이 존재하지 않음: {}", filePath);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: url={}", fileUrl, e);
        }
    }
}
