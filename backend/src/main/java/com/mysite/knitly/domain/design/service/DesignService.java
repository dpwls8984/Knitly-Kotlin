package com.mysite.knitly.domain.design.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.dto.DesignListResponse;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.dto.DesignUploadRequest;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.util.FileValidator;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.design.util.PdfGenerator;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import com.mysite.knitly.global.util.FileNameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DesignService {
    private final DesignRepository designRepository;
    private final PdfGenerator pdfGenerator;
    private final LocalFileStorage localFileStorage;
    private final ObjectMapper objectMapper;
    private final FileValidator fileValidator;

    // 도안 생성
    @Transactional
    public DesignResponse createDesign(User user, DesignRequest request) {
        Long userId = user.getUserId();
        MDC.put("userId", String.valueOf(userId));

        String designName = request.designName();
        log.info("[Design] [Create] 도안 생성 시작 - designName={}", designName);

        try {
            // gridData 입력 검증
            if(!request.isValidGridSize()) throw new ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE);

            // PDF 생성
            byte[] pdfBytes = pdfGenerator.generate(designName, request.gridData());
            log.debug("[Design] [Create] PDF 생성 완료 - size={}B",  pdfBytes.length);

            // 파일명 정리
            String base = (request.fileName() == null || request.fileName().isBlank())
                    ? request.designName()
                    : request.fileName();
            String sanitized = FileNameUtils.sanitize(base);

            // 로컬에 파일 저장
            String pdfUrl = localFileStorage.savePdfFile(pdfBytes, sanitized);
            log.debug("[Design] [Create] PDF 저장 완료 - url={}", pdfUrl);

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
            MDC.put("designId", String.valueOf(savedDesign.getDesignId()));
            log.info("[Design] [Create] 도안 생성 완료 - designName={}, fileSize={}bytes", designName, pdfBytes.length);

            return DesignResponse.from(savedDesign);
        } finally{
            MDC.clear();
        }
    }

    // 기존 pdf 파일 업로드
    public DesignResponse uploadPdfDesign(User user, DesignUploadRequest request){
        Long userId = user.getUserId();
        MDC.put("userId", String.valueOf(userId));

        MultipartFile pdfFile = request.pdfFile();
        String originalFileName = pdfFile.getOriginalFilename();

        log.info("[Design] [Upload] PDF 업로드 시작 - fileName={}, fileSize={}bytes",
                originalFileName, pdfFile.getSize());
        try{
            // 파일 검증
            fileValidator.validatePdfFile(pdfFile);

            byte[] pdfBytes;
            try {
                pdfBytes = pdfFile.getBytes();
            } catch (IOException e) {
                log.error("[Design] [Upload] 파일 읽기 실패 - fileName={}", originalFileName, e);
                throw new ServiceException(ErrorCode.DESIGN_FILE_SAVE_FAILED);
            }

            String base = (request.designName() == null || request.designName().isBlank())
                    ? defaultBaseName(pdfFile.getOriginalFilename())
                    : request.designName();
            String sanitized = FileNameUtils.sanitize(base);

            // 파일 저장
            String pdfUrl = localFileStorage.savePdfFile(pdfBytes, sanitized);
            log.debug("[Design] [Upload] 파일 저장 완료 - url={}",  pdfUrl);

            Design design = Design.builder()
                    .user(user)
                    .designName(sanitized)
                    .pdfUrl(pdfUrl)
                    .gridData("{}")
                    .designState(DesignState.BEFORE_SALE)
                    .build();

            Design savedDesign = designRepository.save(design);
            MDC.put("designId", String.valueOf(savedDesign.getDesignId()));

            log.info("[Design] [Upload] PDF 업로드 완료 - DesignName={}, fileSize={}bytes",
                    sanitized, pdfBytes.length);

            return DesignResponse.from(savedDesign);
        } finally {
            MDC.clear();
        }
    }

    // 본인 도안 조회
    @Transactional(readOnly = true)
    public List<DesignListResponse> getMyDesigns (User user){
        Long userId = user.getUserId();
        MDC.put("userId", String.valueOf(userId));

        log.info("[Design] [List] 도안 목록 조회 시작");

        try {
            List<Design> designs = designRepository.findByUser(user).reversed();
            log.info("[Design] [List] 도안 목록 조회 완료 - count={}", designs.size());

            return designs.stream()
                    .map(DesignListResponse::from)
                    .collect(Collectors.toList());

        } finally {
            MDC.clear();
        }
    }


    // 도안 삭제 - BEFORE_SALE 상태인 도안만 삭제 가능, ON_SALE 또는 STOPPED 상태인 도안은 삭제 불가
    public void deleteDesign(User user, Long designId){
        Long userId = user.getUserId();
        MDC.put("userId", String.valueOf(userId));
        MDC.put("designId", String.valueOf(designId));

        log.info("[Design] [Delete] 도안 삭제 시작");

        try{
            Design design = designRepository.findById(designId)
                    .orElseThrow(() -> {
                        log.warn("[Design] [Delete] 도안을 찾을 수 없음");
                        return new ServiceException(ErrorCode.DESIGN_NOT_FOUND);
                    });

            // 본인 도안인지 확인
            if (!design.isOwnedBy(userId)) {
                log.warn("[Design] [Delete] 권한 없음 - ownerId={}",
                        design.getUser().getUserId());
                throw new ServiceException(ErrorCode.DESIGN_UNAUTHORIZED_DELETE);
            }

            // 삭제 가능 상태인지 확인
            if (!design.isDeletable()) {
                log.warn("[Design] [Delete] 삭제 불가능한 상태 - state={}",
                        design.getDesignState());
                throw new ServiceException(ErrorCode.DESIGN_NOT_DELETABLE);
            }

            // 파일 삭제 시도
            String pdfUrl = design.getPdfUrl();
            try {
                localFileStorage.deleteFile(pdfUrl);
                log.debug("[Design] [Delete] 파일 삭제 완료");
            } catch (Exception e) {
                log.warn("[Design] [Delete] 파일 삭제 실패 (DB는 삭제 진행) - pdfUrl={}", pdfUrl, e);
                // 파일 삭제 실패해도 DB는 삭제 진행
            }

            designRepository.delete(design);
            log.info("[Design] [Delete] 도안 삭제 완료 -");
        } finally {
            MDC.clear();
        }
    }


    // 판매 중지 - ON_SALE -> STOPPED
    @Transactional
    public void stopDesignSale(User user, Long designId) {
        Long userId = user.getUserId();
        MDC.put("userId", String.valueOf(userId));
        MDC.put("designId", String.valueOf(designId));
        log.info("[Design] [Stop] 판매 중지 시작");

        try{
            Design design = designRepository.findById(designId)
                    .orElseThrow(() -> {
                        log.warn("[Design] [Stop] 도안을 찾을 수 없음");
                        return new ServiceException(ErrorCode.DESIGN_NOT_FOUND);
                    });

            // 본인 도안인지 확인
            if (!design.isOwnedBy(userId)) {
                log.warn("[Design] [Stop] 권한 없음 - ownerId={}",
                        design.getUser().getUserId());
                throw new ServiceException(ErrorCode.DESIGN_UNAUTHORIZED_ACCESS);
            }

            // 판매 중인지 확인
            if (design.getDesignState() != DesignState.ON_SALE) {
                log.warn("[Design] [Stop] 판매 중인 상품이 아님 - state={}",
                        design.getDesignState());
                throw new ServiceException(ErrorCode.DESIGN_NOT_ON_SALE);
            }

            design.stopSale();
            log.info("[Design] [Stop] 판매 중지 완료");

        } finally {
            MDC.clear();
        }
    }

    // 판매 재개 - STOPPED -> ON_SALE
    @Transactional
    public void relistDesign(User user, Long designId) {
        Long userId = user.getUserId();
        MDC.put("userId", String.valueOf(userId));
        MDC.put("designId", String.valueOf(designId));
        log.info("[Design] [Relist] 판매 재개 시작");

        try {
            Design design = designRepository.findById(designId)
                    .orElseThrow(() -> {
                        log.warn("[Design] [Relist] 도안을 찾을 수 없음");
                        return new ServiceException(ErrorCode.DESIGN_NOT_FOUND);
                    });

            // 권한 확인
            if (!design.isOwnedBy(userId)) {
                log.warn("[Design] [Relist] 권한 없음 - ownerId={}",
                        design.getUser().getUserId());
                throw new ServiceException(ErrorCode.DESIGN_UNAUTHORIZED_ACCESS);
            }

            // 중지 상태인지 확인
            if (design.getDesignState() != DesignState.STOPPED) {
                log.warn("[Design] [Relist] 중지 상태가 아님 - state={}",
                        design.getDesignState());
                throw new ServiceException(ErrorCode.DESIGN_NOT_STOPPED);
            }

            design.relist();
            log.info("[Design] [Relist] 판매 재개 완료");

        } finally {
            MDC.clear();
        }
    }

    private String convertGridDataToJson(Object gridData) {
        try {
            return objectMapper.writeValueAsString(gridData);
        } catch (JsonProcessingException e) {
            log.error("[Design] gridData JSON 변환 실패", e);
            throw new ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE);
        }
    }

    private String defaultBaseName(String original) {
        if (original == null || original.isBlank()) return "design";
        int i = original.lastIndexOf('.');
        return i > 0 ? original.substring(0, i) : original;
    }

}

