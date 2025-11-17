package com.mysite.knitly.domain.design.dto;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;

import java.time.LocalDateTime;

public record DesignListResponse (
        Long designId,
        String designName,
        String pdfUrl,
        DesignState designState,
        LocalDateTime createdAt
){
    public static DesignListResponse from(Design design) {
        return new DesignListResponse(
                design.getDesignId(),
                design.getDesignName(),
                design.getPdfUrl(),
                design.getDesignState(),
                design.getCreatedAt()
        );
    }
}