package com.mysite.knitly.domain.design.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.dto.DesignState;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.design.util.PdfGenerator;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepositoryTmp;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DesignService {
    private final DesignRepository designRepository;
    private final UserRepositoryTmp userRepository; // 충돌 방지용 임시 네이밍
    private final PdfGenerator pdfGenerator;
    private final LocalFileStorage localFileStorage;
    private final ObjectMapper objectMapper;

    @Transactional
    public DesignResponse createDesign(UUID userId, DesignRequest request) {
        // gridData 입력 검증
        if(!request.isValidGridSize()) throw new ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE);

        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // PDF 생성
        byte[] pdfBytes = pdfGenerator.generate(request.getDesignName(), request.getGridData());

        // 로컬에 파일 저장
        String pdfUrl = localFileStorage.savePdfFile(pdfBytes);

        // gridData를 JSON 문자열로 변환
        String gridDataJson = convertGridDataToJson(request.getGridData());

        // 도안 엔티티 생성 및 저장
        Design design = Design.builder()
                .user(user)
                .designName(request.getDesignName())
                .pdfUrl(pdfUrl)
                .gridData(gridDataJson)
                .designState(DesignState.BEFORE_SALE)
                .build();

        Design savedDesign = designRepository.save(design);

        return DesignResponse.from(savedDesign);

    }

    private String convertGridDataToJson(Object gridData) {
        try {
            return objectMapper.writeValueAsString(gridData);
        } catch (JsonProcessingException e) {
            throw new ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE);
        }
    }
}

