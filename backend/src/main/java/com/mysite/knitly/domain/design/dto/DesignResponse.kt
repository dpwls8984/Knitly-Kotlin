package com.mysite.knitly.domain.design.dto

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import java.time.LocalDateTime

data class DesignResponse(
    val designId: Long?,
    val designName: String,
    val pdfUrl: String?,
    val designState: DesignState,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(design: Design): DesignResponse {
            return DesignResponse(
                designId = design.designId,
                designName = design.designName,
                pdfUrl = design.pdfUrl,
                designState = design.designState,
                createdAt = design.createdAt
            )
        }
    }
}