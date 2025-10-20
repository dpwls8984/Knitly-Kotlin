package com.mysite.knitly.domain.design.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.dto.DesignListResponse;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.design.util.PdfGenerator;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DesignService {
    private final DesignRepository designRepository;
    private final UserRepository userRepository; // 충돌 방지용 임시 네이밍
    private final PdfGenerator pdfGenerator;
    private final LocalFileStorage localFileStorage;
    private final ObjectMapper objectMapper;

    // 도안 생성
    @Transactional
    public DesignResponse createDesign(Long userId, DesignRequest request) {
        // gridData 입력 검증
        if(!request.isValidGridSize()) throw new ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE);

        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // PDF 생성
        byte[] pdfBytes = pdfGenerator.generate(request.designName(), request.gridData());

        // 로컬에 파일 저장
        String pdfUrl = localFileStorage.savePdfFile(pdfBytes);

        // gridData를 JSON 문자열로 변환
        String gridDataJson = convertGridDataToJson(request.gridData());

        // 도안 엔티티 생성 및 저장
        Design design = Design.builder()
                .user(user)
                .designName(request.designName())
                .pdfUrl(pdfUrl)
                .gridData(gridDataJson)
                .designState(DesignState.BEFORE_SALE)
                .build();

        Design savedDesign = designRepository.save(design);

        return DesignResponse.from(savedDesign);

    }


    // 본인 도안 조회
    @Transactional(readOnly = true)
    public List<DesignListResponse> getMyDesigns (Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        List<Design> designs = designRepository.findByUser(user);

        return designs.stream()
                .map(DesignListResponse::from)
                .collect(Collectors.toList());
    }


    // 도안 삭제 - BEFORE_SALE 상태인 도안만 삭제 가능, ON_SALE 또는 STOPPED 상태인 도안은 삭제 불가
    public void deleteDesign(Long userId, Long designId){
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new ServiceException(ErrorCode.DESIGN_NOT_FOUND));

        // 본인 도안인지 확인
        if(!design.isOwnedBy(userId)){
            throw new ServiceException(ErrorCode.DESIGN_UNAUTHORIZED_DELETE);
        }

        if(!design.isDeletable()){
            throw new ServiceException(ErrorCode.DESIGN_NOT_DELETABLE);
        }

        try {
            localFileStorage.deleteFile(design.getPdfUrl());
        } catch (Exception e) {
            log.warn("파일 삭제 실패 (DB는 삭제 진행): pdfUrl={}", design.getPdfUrl(), e);
            // 파일 삭제 실패해도 DB는 삭제 진행
        }

        designRepository.delete(design);
    }


    private String convertGridDataToJson(Object gridData) {
        try {
            return objectMapper.writeValueAsString(gridData);
        } catch (JsonProcessingException e) {
            throw new ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE);
        }
    }
}

