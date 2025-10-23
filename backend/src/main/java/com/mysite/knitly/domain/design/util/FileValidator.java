package com.mysite.knitly.domain.design.util;

import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FileValidator {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final String[] ALLOWED_EXTENSIONS = {".pdf"};

    // pdf 파일 유효성 검증
    public void validatePdfFile(MultipartFile file) {
        // 1. 파일 존재 여부
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ErrorCode.DESIGN_FILE_EMPTY);
        }

        // 2. 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("파일 크기 초과: {} bytes (최대: {} bytes)", file.getSize(), MAX_FILE_SIZE);
            throw new ServiceException(ErrorCode.DESIGN_FILE_SIZE_EXCEEDED);
        }

        // 3. Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(ALLOWED_CONTENT_TYPE)) {
            log.warn("잘못된 파일 타입: {}", contentType);
            throw new ServiceException(ErrorCode.DESIGN_FILE_INVALID_TYPE);
        }

        // 4. 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidExtension(originalFilename)) {
            log.warn("잘못된 파일 확장자: {}", originalFilename);
            throw new ServiceException(ErrorCode.DESIGN_FILE_INVALID_TYPE);
        }

        log.debug("파일 검증 통과: name={}, size={} bytes", originalFilename, file.getSize());
    }

    private boolean hasValidExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

}
