package com.mysite.knitly.domain.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignRequest {

    @NotBlank(message = "파일 이름은 필수 항목입니다.")
    @Size(max=30, message = "파일 이름은 30자를 초과할 수 없습니다.")
    private String designName;

    @NotNull(message = "도안 데이터는 필수 항목입니다.")
    @Size(min=10, max=10, message="도안은 10X10 크기여야 합니다.")
    private List<List<String>> gridData;

    @Size(max = 80)
    private String fileName; // (선택) 파일 이름

    public DesignRequest(String designName, List<List<String>> gridData) {
        this.designName = designName;
        this.gridData = gridData;
    }

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
