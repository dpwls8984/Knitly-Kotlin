package com.mysite.knitly.domain.design.controller;

import com.mysite.knitly.domain.design.dto.DesignListResponse;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.dto.DesignUploadRequest;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.service.DesignService;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/designs")
public class DesignController {
    private final DesignService designService;
    private final DesignRepository designRepository;
    private final LocalFileStorage localFileStorage;

    // 도안 생성
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DesignResponse> createDesign(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DesignRequest request

    ) {
        DesignResponse response = designService.createDesign(user, request);

        return ResponseEntity.ok(response);
    }


    //기존 PDF 업로드
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DesignResponse> uploadDesignPdf(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String designName
    ) {
        DesignUploadRequest req = new DesignUploadRequest(designName, file);
        return ResponseEntity.ok(designService.uploadPdfDesign(user, req));
    }

    // 도안 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<DesignListResponse>> getMyDesigns(
            @AuthenticationPrincipal User user
    ) {
        List<DesignListResponse> designs = designService.getMyDesigns(user);
        return ResponseEntity.ok(designs);
    }

    // 도안 삭제
    @DeleteMapping("/{designId}")
    public ResponseEntity<Void> deleteDesign(
            @AuthenticationPrincipal User user,
            @PathVariable Long designId){
        designService.deleteDesign(user, designId);
        return ResponseEntity.noContent().build();
    }

    // 판매 중지
    @PatchMapping("/{designId}/stop")
    public ResponseEntity<Void> stopDesignSale(
            @AuthenticationPrincipal User user,
            @PathVariable Long designId
    ) {
        designService.stopDesignSale(user, designId);
        return ResponseEntity.noContent().build();
    }

    // 판매 재개
    @PatchMapping("/{designId}/relist")
    public ResponseEntity<Void> relistDesign(
            @AuthenticationPrincipal User user,
            @PathVariable Long designId
    ) {
        designService.relistDesign(user, designId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{designId}/pdf")
    public ResponseEntity<Resource> downloadDesignPdf(
            @PathVariable Long designId
    ) throws IOException {
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new ServiceException(ErrorCode.DESIGN_NOT_FOUND));

        // 파일 경로 가져오기
        Path filePath = localFileStorage.toAbsolutePathFromUrl(design.getPdfUrl());

        if (!Files.exists(filePath)) {
            throw new ServiceException(ErrorCode.DESIGN_FILE_NOT_FOUND);
        }

        // 파일을 Resource로 변환
        Resource resource = new UrlResource(filePath.toUri());

        // Content-Disposition 헤더 설정 (브라우저에서 열기)
        String contentDisposition = ContentDisposition
                .inline()
                .filename(design.getDesignName() + ".pdf", StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }
}
