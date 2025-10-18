package com.mysite.knitly.domain.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DesignRequest (

    @NotBlank(message = "파일 이름은 필수 항목입니다.")
    @Size(max = 30, message = "파일 이름은 30자를 초과할 수 없습니다.")
    String designName,

    @NotNull(message = "도안 데이터는 필수 항목입니다.")
    @Size(min = 10, max = 10, message = "도안은 10x10 크기여야 합니다.")
    List<@Size(min = 10, max = 10, message = "각 행은 10칸이어야 합니다.") List<String>> gridData,

    @Size(max = 80, message = "파일 이름은 80자를 초과할 수 없습니다.")
    String fileName // (선택) 파일 이름
) {
    // 유효성 검증 메서드 ( 10x10 크기인지 확인 )
    public boolean isValidGridSize() {
        if (gridData == null || gridData.size() != 10) {
            return false;
        }
        for (List<String> row : gridData) {
            if (row == null || row.size() != 10) {
                return false;
            }
        }
        return true;
    }
}
