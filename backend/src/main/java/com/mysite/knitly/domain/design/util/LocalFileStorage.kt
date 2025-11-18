package com.mysite.knitly.domain.design.util

import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.lang.String
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.util.*
import kotlin.ByteArray
import kotlin.collections.plus
import kotlin.plus
import kotlin.sequences.plus
import kotlin.text.endsWith
import kotlin.text.format
import kotlin.text.isEmpty
import kotlin.text.lowercase
import kotlin.text.plus
import kotlin.text.replace
import kotlin.text.startsWith
import kotlin.text.substring

@Component
class LocalFileStorage {
    private val log = LoggerFactory.getLogger(LocalFileStorage::class.java)

    @Value("\${file.upload-dir:./uploads/designs}")
    private lateinit var uploadDir: kotlin.String

    @Value("\${file.public-prefix:/files}")
    private lateinit var publicPrefix: kotlin.String

    companion object {
        private const val BACKEND_BASE_URL = "http://localhost:8080"
        private const val PRODUCTS_URL_PREFIX = "/products/"
    }



    // 바이트 배열로 들어온 pdf를 저장
    // {uuid8}_{sanitizedName}.pdf 형태로 저장
    fun savePdfFile(fileData: ByteArray, fileName: kotlin.String?): kotlin.String {
        return try {
            val today = LocalDate.now()

            // 업로드 디렉토리 생성
            val base = Paths.get(uploadDir).toAbsolutePath().normalize()
            val dir = base.resolve(
                Paths.get(
                    today.year.toString(),
                    kotlin.String.format("%02d", today.monthValue),
                    kotlin.String.format("%02d", today.dayOfMonth)
                )
            )
            Files.createDirectories(dir)

            // 고유 파일명 생성
            val baseName = stripPdfExtension(fileName ?: "design")
            val uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
            val savedName = "${uuid8}_${baseName}.pdf"

            // 저장
            val filePath = dir.resolve(savedName)
            Files.write(filePath, fileData, StandardOpenOption.CREATE_NEW)

            // 접근 가능한 URL 생성
            val relativePath = listOf(
                today.year.toString(),
                kotlin.String.format("%02d", today.monthValue),
                kotlin.String.format("%02d", today.dayOfMonth),
                savedName
            ).joinToString("/")

            val prefix = if (publicPrefix.endsWith("/")) {
                publicPrefix.substring(0, publicPrefix.length - 1)
            } else {
                publicPrefix
            }
            val url = "$prefix/$relativePath"

            log.info(
                "[FileStorage] [Save] PDF 저장 완료 - fileName={}, size={}bytes, path={}",
                savedName, fileData.size, filePath
            )

            url // DB에는 접근 가능한 URL만 저장
        } catch (e: IOException) {
            log.error("PDF 파일 저장 실패", e)
            throw ServiceException(ErrorCode.DESIGN_FILE_SAVE_FAILED)
        }
    }

    // PDF URL에서 절대 경로 변환
    fun toAbsolutePathFromUrl(pdfUrl: kotlin.String): Path {
        log.debug("[FileStorage] [PathConvert] URL을 경로로 변환 - url={}", pdfUrl)

        val prefix = if (publicPrefix.endsWith("/")) publicPrefix else "$publicPrefix/"
        var rel = when {
            pdfUrl.startsWith(prefix) -> pdfUrl.substring(prefix.length)
            pdfUrl.startsWith(publicPrefix) -> pdfUrl.substring(publicPrefix.length)
            else -> pdfUrl
        }
        if (rel.startsWith("/")) rel = rel.substring(1)

        val absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(rel).normalize()
        log.debug("[FileStorage] [PathConvert] 변환 완료 - url={}, path={}", pdfUrl, absolutePath)

        return absolutePath
    }

    fun deleteFile(fileUrl: kotlin.String) {
        log.info("[FileStorage] [Delete] 파일 삭제 시작 - url={}", fileUrl)

        try {
            val filePath = toAbsolutePathFromUrl(fileUrl)
            if (Files.exists(filePath)) {
                Files.delete(filePath)
                log.info("[FileStorage] [Delete] 파일 삭제 완료 - url={}, path={}", fileUrl, filePath)
            } else {
                log.warn("[FileStorage] [Delete] 삭제할 파일이 존재하지 않음 - url={}, path={}", fileUrl, filePath)
            }
        } catch (e: IOException) {
            log.error("[FileStorage] [Delete] 파일 삭제 실패 - url={}", fileUrl, e)
        }
    }

    //TODO: 예진 - 상품 이미지 저장
    fun saveProductImage(file: MultipartFile): kotlin.String {
        if (file == null || file.isEmpty()) {
            throw ServiceException(ErrorCode.FILE_STORAGE_FAILED)
        }

        val MAX_FILE_SIZE_BYTES = (3 * 1024 * 1024).toLong() // 3MB

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ServiceException(ErrorCode.PRODUCT_IMAGE_SIZE_EXCEEDED)
        }

        try {
            val today = LocalDate.now()
            val base = Paths.get(uploadDir).getParent().resolve("products").toAbsolutePath().normalize()
            val dir = base.resolve(
                Paths.get(
                    today.getYear().toString(),
                    kotlin.String.format("%02d", today.getMonthValue()),
                    kotlin.String.format("%02d", today.getDayOfMonth())
                )
            )
            Files.createDirectories(dir)

            val originalName = file.getOriginalFilename()
            val uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
            val savedName = uuid8 + "_" + originalName

            val filePath = dir.resolve(savedName)
            Files.write(filePath, file.getBytes(), StandardOpenOption.CREATE_NEW)

            val relativePath = String.join(
                "/",
                today.getYear().toString(),
                kotlin.String.format("%02d", today.getMonthValue()),
                kotlin.String.format("%02d", today.getDayOfMonth()),
                savedName
            )

            //String url = "/products/" + relativePath;
            val url = BACKEND_BASE_URL + "/products/" + relativePath

            log.info("[FileStorage] [Save] Product 이미지 저장 완료 - path={}", filePath)
            return url
        } catch (e: IOException) {
            log.error("Product 이미지 저장 실패", e)
            throw ServiceException(ErrorCode.FILE_STORAGE_FAILED)
        }
    }

    fun deleteProductImage(fileUrl: kotlin.String?) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("삭제할 Product 이미지 URL이 비어있습니다.")
            return
        }

        try {
            // 1. URL에서 상대 경로(relative path) 추출
            // 예: "http://localhost:8080/products/2025/11/11/img.jpg"
            val relativePath: kotlin.String?
            if (fileUrl.startsWith(BACKEND_BASE_URL + PRODUCTS_URL_PREFIX)) {
                // "http://localhost:8080/products/" 부분을 제거
                relativePath = fileUrl.substring((BACKEND_BASE_URL + PRODUCTS_URL_PREFIX).length)
            } else if (fileUrl.startsWith(PRODUCTS_URL_PREFIX)) {
                // (예외 처리) Base URL이 없는 경우 (예: /products/...)
                relativePath = fileUrl.substring(PRODUCTS_URL_PREFIX.length)
            } else {
                log.warn("예상치 못한 Product 이미지 URL 형식입니다: {}", fileUrl)
                return
            }

            // 최종 relativePath = "2025/11/11/img.jpg"

            // 2. 물리적 기본(base) 경로 생성 (saveProductImage와 동일한 로직)
            val base = Paths.get(uploadDir).getParent().resolve("products").toAbsolutePath().normalize()

            // 3. 최종 파일 경로 조합
            // 예: (.../uploads/products) + (2025/11/11/img.jpg)
            val filePath = base.resolve(relativePath).normalize()

            // 4. 파일 삭제 (try-catch로 IOException 처리)
            if (Files.exists(filePath)) {
                Files.delete(filePath)
                log.info("[Product Img Delete] 파일 삭제 완료: {}", filePath)
            } else {
                log.warn("[Product Img Delete] 삭제할 파일이 존재하지 않음: {}", filePath)
            }
        } catch (e: IOException) {
            log.error("[Product Img Delete] 파일 삭제 실패: {}", fileUrl, e)
            // 예외를 밖으로 던지지 않고 로그만 남겨서, 기본 트랜잭션(상품 수정)이
            // 롤백되는 것을 방지합니다.
        }
    }

    //TODO: 시현
    fun saveReviewImage(file: MultipartFile): kotlin.String {
        if (file == null || file.isEmpty()) {
            throw ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED)
        }

        val MAX_FILE_SIZE_BYTES = (3 * 1024 * 1024).toLong() // 3MB

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ServiceException(ErrorCode.REVIEW_IMAGE_SIZE_EXCEEDED)
        }

        try {
            val today = LocalDate.now()
            val base = Paths.get(uploadDir).getParent().resolve("reviews").toAbsolutePath().normalize()
            val dir = base.resolve(
                Paths.get(
                    today.getYear().toString(),
                    kotlin.String.format("%02d", today.getMonthValue()),
                    kotlin.String.format("%02d", today.getDayOfMonth())
                )
            )
            Files.createDirectories(dir)

            val originalName = file.getOriginalFilename()
            val uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
            val savedName = uuid8 + "_" + originalName

            val filePath = dir.resolve(savedName)
            Files.write(filePath, file.getBytes(), StandardOpenOption.CREATE_NEW)

            // public URL
            val relativePath = String.join(
                "/",
                today.getYear().toString(),
                kotlin.String.format("%02d", today.getMonthValue()),
                kotlin.String.format("%02d", today.getDayOfMonth()),
                savedName
            )

            val url = "/reviews/" + relativePath

            log.info("리뷰 이미지 저장 완료: {}", filePath)
            return url
        } catch (e: IOException) {
            log.error("리뷰 이미지 저장 실패", e)
            throw ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED)
        }
    }


    private fun stripPdfExtension(name: kotlin.String): kotlin.String {
        return if (name.lowercase().endsWith(".pdf")) {
            name.substring(0, name.length - 4)
        } else {
            name
        }
    }
}