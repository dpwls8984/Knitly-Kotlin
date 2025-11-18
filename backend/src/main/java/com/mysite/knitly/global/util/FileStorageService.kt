package com.mysite.knitly.global.util;

import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final String uploadDir = "resources/static/";
    private final String urlPrefix = "/resources/static/";

    /**
     * 파일을 저장하고 접근 URL을 반환합니다.
     * @param file 저장할 MultipartFile
     * @param domain 'product', 'review' 등 파일이 속한 도메인
     * @return 파일에 접근할 수 있는 URL
     */
    public String storeFile(MultipartFile file, String domain) {
        String originalFilename = file.getOriginalFilename();
        if (!ImageValidator.isAllowedImageUrl(originalFilename)) {
            throw new ServiceException(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED);
        }

        try {
            String domainUploadDir = uploadDir + domain + "/";
            new File(domainUploadDir).mkdirs(); // 도메인별 디렉토리 생성

            // 수정- 확장자를 별도로 추출
            String extension = getFileExtension(originalFilename);
            String nameWithoutExt = getFileNameWithoutExtension(originalFilename);
            String sanitizedName = sanitizeFileName(nameWithoutExt);


            // UUID + sanitized된 이름 + 원본확장자
            String filename = UUID.randomUUID() + "_" + sanitizedName + extension;
            Path path = Paths.get(domainUploadDir, filename);
            Files.write(path, file.getBytes());
            log.info("[FileStorage] 파일 저장 완료 - original={}, saved={}", originalFilename, filename);

            return urlPrefix + domain + "/" + filename;

        } catch (IOException e) {
            log.error("[FileStorage] 파일 저장 실패 - filename={}", originalFilename, e);
            throw new ServiceException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    /**
     * 파일 URL을 기반으로 실제 파일을 삭제합니다.
     * @param fileUrl 삭제할 파일의 URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // URL에서 실제 파일 시스템 경로를 추출
            // 예: /resources/static/product/abc.jpg -> resources/static/product/abc.jpg
            String filePath = fileUrl.replaceFirst(urlPrefix, uploadDir);
            Path path = Paths.get(filePath);

            Files.deleteIfExists(path);
            log.info("[FileStorage] 파일 삭제 성공 - filePath={}", filePath);

        } catch (IOException e) {
            log.error("[FileStorage] 파일 삭제 실패 - fileUrl={}", fileUrl, e);
            // 파일 삭제 실패가 전체 트랜잭션을 롤백시킬 필요는 없으므로, 여기서는 예외를 다시 던지지 않고 로그만 남깁니다.
        }
    }

    /**
     * 파일 URL을 기반으로 실제 파일 내용을 byte 배열로 읽어옵니다.
     * @param fileUrl 읽어올 파일의 URL
     * @return 파일의 byte[]
     * @throws IOException 파일 읽기 실패 시
     */
    public byte[] loadFileAsBytes(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new IOException("유효하지 않은 파일 URL입니다.");
        }
        try {
            String filePath = fileUrl.replaceFirst(urlPrefix, uploadDir);
            Path path = Paths.get(filePath);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("[FileStorage] 파일 읽기 실패 - fileUrl={}", fileUrl, e);
            throw e; // 예외를 상위로 던져서 Consumer가 처리하도록 함
        }
    }

    /**
     * 파일명에서 확장자 추출 (점 포함)
     * 예: "image.png" → ".png"
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex); // .png, .jpg 등
    }

    /**
     * 파일명에서 확장자 제거
     * 예: "image.png" → "image"
     */
    private String getFileNameWithoutExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return filename;
        }

        return filename.substring(0, lastDotIndex);
    }

    /**
     * 파일명 정리 (특수문자 제거, 공백 처리)
     * 확장자는 포함하지 않음
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }

        // 특수문자를 언더스코어로 변경
        String sanitized = name.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_"); // 공백을 언더스코어로

        // 길이 제한 (확장자 제외)
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized;
    }
}