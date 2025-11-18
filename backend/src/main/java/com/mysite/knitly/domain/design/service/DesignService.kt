package com.mysite.knitly.domain.design.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.design.dto.DesignListResponse
import com.mysite.knitly.domain.design.dto.DesignRequest
import com.mysite.knitly.domain.design.dto.DesignResponse
import com.mysite.knitly.domain.design.dto.DesignUploadRequest
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.design.util.FileValidator
import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.design.util.PdfGenerator
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import com.mysite.knitly.global.util.FileNameUtils
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException

@Service
@Transactional
class DesignService(
    private val designRepository: DesignRepository,
    private val pdfGenerator: PdfGenerator,
    private val localFileStorage: LocalFileStorage,
    private val objectMapper: ObjectMapper,
    private val fileValidator: FileValidator
) {
    private val log = LoggerFactory.getLogger(DesignService::class.java)

    // 도안 생성
    @Transactional
    fun createDesign(user: User, request: DesignRequest): DesignResponse {
        val userId = user.userId
        MDC.put("userId", userId.toString())

        val designName = request.designName
        log.info("[Design] [Create] 도안 생성 시작 - designName={}", designName)

        return try {
            // gridData 입력 검증
            if (!request.isValidGridSize()) {
                throw ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE)
            }

            // PDF 생성
            val pdfBytes = pdfGenerator.generate(designName, request.gridData)
            log.debug("[Design] [Create] PDF 생성 완료 - size={}B", pdfBytes.size)

            // 파일명 정리
            val base = request.fileName?.takeIf { it.isNotBlank() } ?: request.designName
            val sanitized = FileNameUtils.sanitize(base)

            // 로컬에 파일 저장
            val pdfUrl = localFileStorage.savePdfFile(pdfBytes, sanitized)
            log.debug("[Design] [Create] PDF 저장 완료 - url={}", pdfUrl)

            // gridData를 JSON 문자열로 변환
            val gridDataJson = convertGridDataToJson(request.gridData)

            // 도안 엔티티 생성 및 저장
            val design = Design(
                user = user,
                designName = request.designName,
                pdfUrl = pdfUrl,
                gridData = gridDataJson,
                designState = DesignState.BEFORE_SALE
            )

            val savedDesign = designRepository.save(design)
            MDC.put("designId", savedDesign.designId.toString())
            log.info(
                "[Design] [Create] 도안 생성 완료 - designName={}, fileSize={}bytes",
                designName, pdfBytes.size
            )

            DesignResponse.from(savedDesign)
        } finally {
            MDC.clear()
        }
    }

    // 기존 pdf 파일 업로드
    fun uploadPdfDesign(user: User, request: DesignUploadRequest): DesignResponse {
        val userId = user.userId
        MDC.put("userId", userId.toString())

        val pdfFile = request.pdfFile
        val originalFileName = pdfFile.originalFilename

        log.info(
            "[Design] [Upload] PDF 업로드 시작 - fileName={}, fileSize={}bytes",
            originalFileName, pdfFile.size
        )

        return try {
            // 파일 검증
            fileValidator.validatePdfFile(pdfFile)

            val pdfBytes = try {
                pdfFile.bytes
            } catch (e: IOException) {
                log.error("[Design] [Upload] 파일 읽기 실패 - fileName={}", originalFileName, e)
                throw ServiceException(ErrorCode.DESIGN_FILE_SAVE_FAILED)
            }

            val base = request.designName?.takeIf { it.isNotBlank() }
                ?: defaultBaseName(pdfFile.originalFilename)
            val sanitized = FileNameUtils.sanitize(base)

            // 파일 저장
            val pdfUrl = localFileStorage.savePdfFile(pdfBytes, sanitized)
            log.debug("[Design] [Upload] 파일 저장 완료 - url={}", pdfUrl)

            val design = Design(
                user = user,
                designName = sanitized,
                pdfUrl = pdfUrl,
                gridData = "{}",
                designState = DesignState.BEFORE_SALE
            )

            val savedDesign = designRepository.save(design)
            MDC.put("designId", savedDesign.designId.toString())

            log.info(
                "[Design] [Upload] PDF 업로드 완료 - DesignName={}, fileSize={}bytes",
                sanitized, pdfBytes.size
            )

            DesignResponse.from(savedDesign)
        } finally {
            MDC.clear()
        }
    }

    // 본인 도안 조회
    @Transactional(readOnly = true)
    fun getMyDesigns(user: User): List<DesignListResponse> {
        val userId = user.userId
        MDC.put("userId", userId.toString())

        log.info("[Design] [List] 도안 목록 조회 시작")

        return try {
            val designs = designRepository.findByUser(user).reversed()
            log.info("[Design] [List] 도안 목록 조회 완료 - count={}", designs.size)

            designs.map { DesignListResponse.from(it) }
        } finally {
            MDC.clear()
        }
    }

    // 도안 삭제 - BEFORE_SALE 상태인 도안만 삭제 가능
    fun deleteDesign(user: User, designId: Long) {
        val userId = user.userId
        MDC.put("userId", userId.toString())
        MDC.put("designId", designId.toString())

        log.info("[Design] [Delete] 도안 삭제 시작")

        try {
            val design = designRepository.findById(designId)
                .orElseThrow {
                    log.warn("[Design] [Delete] 도안을 찾을 수 없음")
                    ServiceException(ErrorCode.DESIGN_NOT_FOUND)
                }

            // 본인 도안인지 확인
            if (!design.isOwnedBy(userId)) {
                log.warn(
                    "[Design] [Delete] 권한 없음 - ownerId={}",
                    design.user.userId
                )
                throw ServiceException(ErrorCode.DESIGN_UNAUTHORIZED_DELETE)
            }

            // 삭제 가능 상태인지 확인
            if (!design.isDeletable()) {
                log.warn(
                    "[Design] [Delete] 삭제 불가능한 상태 - state={}",
                    design.designState
                )
                throw ServiceException(ErrorCode.DESIGN_NOT_DELETABLE)
            }

            // 파일 삭제 시도
            val pdfUrl = design.pdfUrl
            try {
                pdfUrl?.let { localFileStorage.deleteFile(it) }
                log.debug("[Design] [Delete] 파일 삭제 완료")
            } catch (e: Exception) {
                log.warn("[Design] [Delete] 파일 삭제 실패 (DB는 삭제 진행) - pdfUrl={}", pdfUrl, e)
                // 파일 삭제 실패해도 DB는 삭제 진행
            }

            designRepository.delete(design)
            log.info("[Design] [Delete] 도안 삭제 완료")
        } finally {
            MDC.clear()
        }
    }

    // 판매 중지 - ON_SALE -> STOPPED
    @Transactional
    fun stopDesignSale(user: User, designId: Long) {
        val userId = user.userId
        MDC.put("userId", userId.toString())
        MDC.put("designId", designId.toString())
        log.info("[Design] [Stop] 판매 중지 시작")

        try {
            val design = designRepository.findById(designId)
                .orElseThrow {
                    log.warn("[Design] [Stop] 도안을 찾을 수 없음")
                    ServiceException(ErrorCode.DESIGN_NOT_FOUND)
                }

            // 본인 도안인지 확인
            if (!design.isOwnedBy(userId)) {
                log.warn(
                    "[Design] [Stop] 권한 없음 - ownerId={}",
                    design.user.userId
                )
                throw ServiceException(ErrorCode.DESIGN_UNAUTHORIZED_ACCESS)
            }

            // 판매 중인지 확인
            if (design.designState != DesignState.ON_SALE) {
                log.warn(
                    "[Design] [Stop] 판매 중인 상품이 아님 - state={}",
                    design.designState
                )
                throw ServiceException(ErrorCode.DESIGN_NOT_ON_SALE)
            }

            design.stopSale()
            log.info("[Design] [Stop] 판매 중지 완료")
        } finally {
            MDC.clear()
        }
    }

    // 판매 재개 - STOPPED -> ON_SALE
    @Transactional
    fun relistDesign(user: User, designId: Long) {
        val userId = user.userId
        MDC.put("userId", userId.toString())
        MDC.put("designId", designId.toString())
        log.info("[Design] [Relist] 판매 재개 시작")

        try {
            val design = designRepository.findById(designId)
                .orElseThrow {
                    log.warn("[Design] [Relist] 도안을 찾을 수 없음")
                    ServiceException(ErrorCode.DESIGN_NOT_FOUND)
                }

            // 권한 확인
            if (!design.isOwnedBy(userId)) {
                log.warn(
                    "[Design] [Relist] 권한 없음 - ownerId={}",
                    design.user.userId
                )
                throw ServiceException(ErrorCode.DESIGN_UNAUTHORIZED_ACCESS)
            }

            // 중지 상태인지 확인
            if (design.designState != DesignState.STOPPED) {
                log.warn(
                    "[Design] [Relist] 중지 상태가 아님 - state={}",
                    design.designState
                )
                throw ServiceException(ErrorCode.DESIGN_NOT_STOPPED)
            }

            design.relist()
            log.info("[Design] [Relist] 판매 재개 완료")
        } finally {
            MDC.clear()
        }
    }

    private fun convertGridDataToJson(gridData: Any): String {
        return try {
            objectMapper.writeValueAsString(gridData)
        } catch (e: JsonProcessingException) {
            log.error("[Design] gridData JSON 변환 실패", e)
            throw ServiceException(ErrorCode.DESIGN_INVALID_GRID_SIZE)
        }
    }

    private fun defaultBaseName(original: String?): String {
        if (original.isNullOrBlank()) return "design"
        val i = original.lastIndexOf('.')
        return if (i > 0) original.substring(0, i) else original
    }
}