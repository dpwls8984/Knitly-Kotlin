package com.mysite.knitly.domain.design.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class DesignRequest(
    @field:NotBlank(message = "파일 이름은 필수 항목입니다.")
    @field:Size(max = 30, message = "파일 이름은 30자를 초과할 수 없습니다.")
    val designName: String,

    @field:NotNull(message = "도안 데이터는 필수 항목입니다.")
    @field:Size(min = 10, max = 10, message = "도안은 10x10 크기여야 합니다.")
    val gridData: List<List<String>>,

    @field:Size(max = 80, message = "파일 이름은 80자를 초과할 수 없습니다.")
    val fileName: String? = null // (선택) 파일 이름
) {
    // 유효성 검증 메서드 (10x10 크기인지 확인)
    fun isValidGridSize(): Boolean {
        if (gridData.size != 10) {
            return false
        }
        return gridData.all { row -> row.size == 10 }
    }
}