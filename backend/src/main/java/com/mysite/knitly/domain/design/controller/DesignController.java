package com.mysite.knitly.domain.design.controller;

import com.mysite.knitly.domain.design.dto.DesignListResponse;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.service.DesignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/designs")
public class DesignController {
    private final DesignService designService;

    // 도안 생성
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DesignResponse> createDesign(
            @RequestParam Long userId,
            @Valid @RequestBody DesignRequest request

    ) {
        DesignResponse response = designService.createDesign(userId, request);

        // Location: /designs/{id}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.designId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }


    // 도안 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<DesignListResponse>> getMyDesigns(
            @RequestParam Long userId
    ) {
        List<DesignListResponse> designs = designService.getMyDesigns(userId);
        return ResponseEntity.ok(designs);
    }

    // 도안 삭제
    @DeleteMapping("/{designId}")
    public ResponseEntity<Void> deleteDesign(
            @RequestParam Long userId,
            @PathVariable Long designId){
        designService.deleteDesign(userId, designId);
        return ResponseEntity.noContent().build();
    }
}
