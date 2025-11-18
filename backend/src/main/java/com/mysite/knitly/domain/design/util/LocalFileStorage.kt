package com.mysite.knitly.domain.design.util;

import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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

    @Value("${file.upload-dir:./uploads/designs}")
    private String uploadDir;

    @Value("${file.public-prefix:/files}")
    private String publicPrefix;

    private final String backendBaseUrl = "http://localhost:8080";
    private final String productsUrlPrefix = "/products/";
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

            String url = (publicPrefix.endsWith("/") ? publicPrefix.substring(0, publicPrefix.length() - 1) : publicPrefix)
                    + "/" + relativePath;

            log.info("[FileStorage] [Save] PDF 저장 완료 - fileName={}, size={}bytes, path={}", savedName, fileData.length, filePath);

            return url; // DB에는 접근 가능한 URL만 저장
        } catch (IOException e) {
            log.error("PDF 파일 저장 실패", e);
            throw new ServiceException(ErrorCode.DESIGN_FILE_SAVE_FAILED);
        }
    }

    // PDF URL에서 절대 경로 변환
    public Path toAbsolutePathFromUrl(String pdfUrl) {
        log.debug("[FileStorage] [PathConvert] URL을 경로로 변환 - url={}", pdfUrl);

        String prefix = publicPrefix.endsWith("/") ? publicPrefix : publicPrefix + "/";
        String rel = pdfUrl.startsWith(prefix) ? pdfUrl.substring(prefix.length()) :
                (pdfUrl.startsWith(publicPrefix) ? pdfUrl.substring(publicPrefix.length()) : pdfUrl);
        if (rel.startsWith("/")) rel = rel.substring(1);

        Path absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(rel).normalize();
        log.debug("[FileStorage] [PathConvert] 변환 완료 - url={}, path={}", pdfUrl, absolutePath);

        return absolutePath;
    }

    public void deleteFile(String fileUrl) throws IOException {
        log.info("[FileStorage] [Delete] 파일 삭제 시작 - url={}", fileUrl);

        Path filePath = toAbsolutePathFromUrl(fileUrl);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("[FileStorage] [Delete] 파일 삭제 완료 - url={}, path={}", fileUrl, filePath);
        } else {
            log.warn("[FileStorage] [Delete] 삭제할 파일이 존재하지 않음 - url={}, path={}", fileUrl, filePath);
        }
    }

        public String saveProductImage (MultipartFile file){
            if (file == null || file.isEmpty()) {
                throw new ServiceException(ErrorCode.FILE_STORAGE_FAILED);
            }

            final long MAX_FILE_SIZE_BYTES = 3 * 1024 * 1024; // 3MB

            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ServiceException(ErrorCode.PRODUCT_IMAGE_SIZE_EXCEEDED);
            }

            try {
                LocalDate today = LocalDate.now();
                Path base = Paths.get(uploadDir).getParent().resolve("products").toAbsolutePath().normalize();
                Path dir = base.resolve(Paths.get(
                        String.valueOf(today.getYear()),
                        String.format("%02d", today.getMonthValue()),
                        String.format("%02d", today.getDayOfMonth())
                ));
                Files.createDirectories(dir);

                String originalName = file.getOriginalFilename();
                String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                String savedName = uuid8 + "_" + originalName;

                Path filePath = dir.resolve(savedName);
                Files.write(filePath, file.getBytes(), StandardOpenOption.CREATE_NEW);

                String relativePath = String.join("/",
                        String.valueOf(today.getYear()),
                        String.format("%02d", today.getMonthValue()),
                        String.format("%02d", today.getDayOfMonth()),
                        savedName
                );

                //String url = "/products/" + relativePath;

                String url = backendBaseUrl + "/products/" + relativePath;

                log.info("[FileStorage] [Save] Product 이미지 저장 완료 - path={}", filePath);
                return url;

            } catch (IOException e) {
                log.error("Product 이미지 저장 실패", e);
                throw new ServiceException(ErrorCode.FILE_STORAGE_FAILED);
            }
        }

        public void deleteProductImage (String fileUrl){
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("삭제할 Product 이미지 URL이 비어있습니다.");
                return;
            }

            try {
                // 1. URL에서 상대 경로(relative path) 추출
                // 예: "http://localhost:8080/products/2025/11/11/img.jpg"
                String relativePath;
                if (fileUrl.startsWith(backendBaseUrl + productsUrlPrefix)) {
                    // "http://localhost:8080/products/" 부분을 제거
                    relativePath = fileUrl.substring((backendBaseUrl + productsUrlPrefix).length());
                } else if (fileUrl.startsWith(productsUrlPrefix)) {
                    // (예외 처리) Base URL이 없는 경우 (예: /products/...)
                    relativePath = fileUrl.substring(productsUrlPrefix.length());
                } else {
                    log.warn("예상치 못한 Product 이미지 URL 형식입니다: {}", fileUrl);
                    return;
                }
                // 최종 relativePath = "2025/11/11/img.jpg"

                // 2. 물리적 기본(base) 경로 생성 (saveProductImage와 동일한 로직)
                Path base = Paths.get(uploadDir).getParent().resolve("products").toAbsolutePath().normalize();

                // 3. 최종 파일 경로 조합
                // 예: (.../uploads/products) + (2025/11/11/img.jpg)
                Path filePath = base.resolve(relativePath).normalize();

                // 4. 파일 삭제 (try-catch로 IOException 처리)
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("[Product Img Delete] 파일 삭제 완료: {}", filePath);
                } else {
                    log.warn("[Product Img Delete] 삭제할 파일이 존재하지 않음: {}", filePath);
                }
            } catch (IOException e) {
                log.error("[Product Img Delete] 파일 삭제 실패: {}", fileUrl, e);
                // 예외를 밖으로 던지지 않고 로그만 남겨서, 기본 트랜잭션(상품 수정)이
                // 롤백되는 것을 방지합니다.
            }
        }

        public String saveReviewImage (MultipartFile file){
            if (file == null || file.isEmpty()) {
                throw new ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED);
            }

            final long MAX_FILE_SIZE_BYTES = 3 * 1024 * 1024; // 3MB

            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ServiceException(ErrorCode.REVIEW_IMAGE_SIZE_EXCEEDED);
            }

            try {
                LocalDate today = LocalDate.now();
                Path base = Paths.get(uploadDir).getParent().resolve("reviews").toAbsolutePath().normalize();
                Path dir = base.resolve(Paths.get(
                        String.valueOf(today.getYear()),
                        String.format("%02d", today.getMonthValue()),
                        String.format("%02d", today.getDayOfMonth())
                ));
                Files.createDirectories(dir);

                String originalName = file.getOriginalFilename();
                String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                String savedName = uuid8 + "_" + originalName;

                Path filePath = dir.resolve(savedName);
                Files.write(filePath, file.getBytes(), StandardOpenOption.CREATE_NEW);

                // public URL
                String relativePath = String.join("/",
                        String.valueOf(today.getYear()),
                        String.format("%02d", today.getMonthValue()),
                        String.format("%02d", today.getDayOfMonth()),
                        savedName
                );

                String url = "/reviews/" + relativePath;

                log.info("리뷰 이미지 저장 완료: {}", filePath);
                return url;

            } catch (IOException e) {
                log.error("리뷰 이미지 저장 실패", e);
                throw new ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED);
            }
        }


        private String stripPdfExtension (String name){
            if (name.toLowerCase().endsWith(".pdf")) {
                return name.substring(0, name.length() - 4);
            }
            return name;
        }
    }