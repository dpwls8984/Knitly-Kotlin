package com.mysite.knitly.domain.design.controller;

import com.mysite.knitly.domain.design.dto.DesignListResponse;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.dto.DesignUploadRequest;
import com.mysite.knitly.domain.design.service.DesignService;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/designs")
public class DesignController {
    private final DesignService designService;

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
}
