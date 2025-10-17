package com.mysite.knitly.domain.design.dto;

import com.mysite.knitly.domain.design.entity.Design;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DesignResponse {
    private Long designId;
    private String designName;
    private String pdfUrl;
    private DesignState designState;
    private LocalDateTime createdAt;

    public static DesignResponse from(Design design){
        return DesignResponse.builder()
                .designId(design.getDesignId())
                .designName(design.getDesignName())
                .pdfUrl(design.getPdfUrl())
                .designState(design.getDesignState())
                .createdAt(design.getCreatedAt())
                .build();
    }

}
