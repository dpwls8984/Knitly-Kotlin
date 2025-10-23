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
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Component
public class LocalFileStorage {

    @Value("${file.upload-dir:uploads/designs}")
    private String uploadDir;

    @Value("${file.public-prefix:/files}")
    private String publicPrefix;

    // 바이트 배열로 들어온 pdf를 저장
    // {uuid8}_{sanitizedName}.pdf 형태로 저장
    public String savePdfFile(byte[] fileData, String fileName) {
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
            String baseName = stripPdfExtension(Objects.toString(fileName, "design"));
            String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String savedName = uuid8 + "_" + baseName + ".pdf";

            // 저장
            Path filePath = dir.resolve(savedName);
            Files.write(filePath, fileData, StandardOpenOption.CREATE_NEW);

            // 접근 가능한 URL 생성
            String relativePath = String.join("/",
                    String.valueOf(today.getYear()),
                    String.format("%02d", today.getMonthValue()),
                    String.format("%02d", today.getDayOfMonth()),
                    savedName
            );

            String url = (publicPrefix.endsWith("/") ? publicPrefix.substring(0, publicPrefix.length()-1) : publicPrefix)
                    + "/" + relativePath;

            log.info("PDF 저장 완료: {}", filePath);

            return url; // DB에는 접근 가능한 URL만 저장
        } catch (IOException e) {
            log.error("PDF 파일 저장 실패", e);
            throw new ServiceException(ErrorCode.DESIGN_FILE_SAVE_FAILED);
        }
    }

    // PDF URL에서 절대 경로 변환
    public Path toAbsolutePathFromUrl(String pdfUrl) {
        String prefix = publicPrefix.endsWith("/") ? publicPrefix : publicPrefix + "/";
        String rel = pdfUrl.startsWith(prefix) ? pdfUrl.substring(prefix.length()) :
                (pdfUrl.startsWith(publicPrefix) ? pdfUrl.substring(publicPrefix.length()) : pdfUrl);
        if (rel.startsWith("/")) rel = rel.substring(1);
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(rel).normalize();
    }

    public void deleteFile(String fileUrl) throws IOException {
        Path filePath = toAbsolutePathFromUrl(fileUrl);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("파일 삭제 완료: {}", filePath);
        } else {
            log.warn("삭제할 파일이 존재하지 않음: {}", filePath);
        }
    }

    private String stripPdfExtension(String name) {
        if (name.toLowerCase().endsWith(".pdf")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }
}
