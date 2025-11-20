package com.mysite.knitly.domain.design.dto

import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class DesignUploadRequest(
    @field:Size(max = 30, message = "도안명은 30자를 초과할 수 없습니다.")
    val designName: String,

    val pdfFile: MultipartFile
)