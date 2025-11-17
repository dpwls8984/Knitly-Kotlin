package com.mysite.knitly.domain.design.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record DesignUploadRequest(
        @Size(max = 30, message = "도안명은 30자를 초과할 수 없습니다.")
        String designName,
        MultipartFile pdfFile
) {}