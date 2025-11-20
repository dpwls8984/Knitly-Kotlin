package com.mysite.knitly.domain.design.controller

import com.mysite.knitly.domain.design.dto.DesignListResponse
import com.mysite.knitly.domain.design.dto.DesignRequest
import com.mysite.knitly.domain.design.dto.DesignResponse
import com.mysite.knitly.domain.design.dto.DesignUploadRequest
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.design.service.DesignService
import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@RestController
@RequestMapping("/designs")
class DesignController(
    private val designService: DesignService,
    private val designRepository: DesignRepository,
    private val localFileStorage: LocalFileStorage
) {
    // 도안 생성
    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createDesign(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: DesignRequest
    ): ResponseEntity<DesignResponse> {
        val response = designService.createDesign(user, request)
        return ResponseEntity.ok(response)
    }

    // 기존 PDF 업로드
    @PostMapping(
        value = ["/upload"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadDesignPdf(
        @AuthenticationPrincipal user: User,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) designName: String?
    ): ResponseEntity<DesignResponse> {
        val req = DesignUploadRequest(designName ?: "", file)
        return ResponseEntity.ok(designService.uploadPdfDesign(user, req))
    }

    // 도안 목록 조회
    @GetMapping("/my")
    fun getMyDesigns(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<List<DesignListResponse>> {
        val designs = designService.getMyDesigns(user)
        return ResponseEntity.ok(designs)
    }

    // 도안 삭제
    @DeleteMapping("/{designId}")
    fun deleteDesign(
        @AuthenticationPrincipal user: User,
        @PathVariable designId: Long
    ): ResponseEntity<Void> {
        designService.deleteDesign(user, designId)
        return ResponseEntity.noContent().build()
    }

    // 판매 중지
    @PatchMapping("/{designId}/stop")
    fun stopDesignSale(
        @AuthenticationPrincipal user: User,
        @PathVariable designId: Long
    ): ResponseEntity<Void> {
        designService.stopDesignSale(user, designId)
        return ResponseEntity.noContent().build()
    }

    // 판매 재개
    @PatchMapping("/{designId}/relist")
    fun relistDesign(
        @AuthenticationPrincipal user: User,
        @PathVariable designId: Long
    ): ResponseEntity<Void> {
        designService.relistDesign(user, designId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{designId}/pdf")
    fun downloadDesignPdf(
        @PathVariable designId: Long
    ): ResponseEntity<Resource> {
        val design = designRepository.findById(designId)
            .orElseThrow { ServiceException(ErrorCode.DESIGN_NOT_FOUND) }

        // 파일 경로 가져오기
        val pdfUrl = design.pdfUrl ?: throw ServiceException(ErrorCode.DESIGN_FILE_NOT_FOUND)
        val filePath = localFileStorage.toAbsolutePathFromUrl(pdfUrl)

        if (!Files.exists(filePath)) {
            throw ServiceException(ErrorCode.DESIGN_FILE_NOT_FOUND)
        }

        // 파일을 Resource로 변환
        val resource = UrlResource(filePath.toUri())

        // Content-Disposition 헤더 설정 (브라우저에서 열기)
        val contentDisposition = ContentDisposition
            .inline()
            .filename("${design.designName}.pdf", StandardCharsets.UTF_8)
            .build()
            .toString()

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .body(resource)
    }
}
