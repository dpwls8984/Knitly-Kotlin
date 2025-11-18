package com.mysite.knitly.domain.design.util

import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.util.*

@Component
class LocalFileStorage(
    @Value("\${file.upload-dir:./uploads/designs}")
    private val uploadDir: String,

    @Value("\${file.public-prefix:/files}")
    private val publicPrefix: String
) {
    private val log = LoggerFactory.getLogger(LocalFileStorage::class.java)

    companion object {
        private const val BACKEND_BASE_URL = "http://localhost:8080"
        private const val PRODUCTS_URL_PREFIX = "/products/"
    }

    fun savePdfFile(fileData: ByteArray, fileName: String?): String {
        return try {
            val today = LocalDate.now()
            val base = Paths.get(uploadDir).toAbsolutePath().normalize()

            val dateDir = "${today.year}/${today.monthValue.toString().padStart(2, '0')}/${today.dayOfMonth.toString().padStart(2, '0')}"
            val dir = base.resolve(dateDir)
            Files.createDirectories(dir)

            val baseName = stripPdfExtension(fileName ?: "design")
            val uuid8 = UUID.randomUUID().toString().replace("-", "").take(8)
            val savedName = "${uuid8}_${baseName}.pdf"

            val filePath = dir.resolve(savedName)
            Files.write(filePath, fileData, StandardOpenOption.CREATE_NEW)

            val relativePath = "$dateDir/$savedName"
            val url = "${publicPrefix.removeSuffix("/")}/$relativePath"

            log.info("[FileStorage] [Save] PDF 저장 완료 - fileName={}, size={}bytes, path={}", savedName, fileData.size, filePath)
            url
        } catch (e: IOException) {
            log.error("PDF 파일 저장 실패", e)
            throw ServiceException(ErrorCode.DESIGN_FILE_SAVE_FAILED)
        }
    }

    fun toAbsolutePathFromUrl(pdfUrl: String): Path {
        log.debug("[FileStorage] [PathConvert] URL을 경로로 변환 - url={}", pdfUrl)

        val prefix = publicPrefix.removeSuffix("/")
        val rel = when {
            pdfUrl.startsWith("$prefix/") -> pdfUrl.substring(prefix.length + 1)
            pdfUrl.startsWith(prefix) -> pdfUrl.substring(prefix.length)
            else -> pdfUrl
        }.removePrefix("/")

        val absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(rel).normalize()
        log.debug("[FileStorage] [PathConvert] 변환 완료 - url={}, path={}", pdfUrl, absolutePath)
        return absolutePath
    }

    fun deleteFile(fileUrl: String) {
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

    fun saveProductImage(file: MultipartFile): String {
        if (file.isEmpty) {
            throw ServiceException(ErrorCode.FILE_STORAGE_FAILED)
        }

        val maxFileSizeBytes = 3 * 1024 * 1024L // 3MB

        if (file.size > maxFileSizeBytes) {
            throw ServiceException(ErrorCode.PRODUCT_IMAGE_SIZE_EXCEEDED)
        }

        return try {
            val today = LocalDate.now()
            val base = Paths.get(uploadDir).parent.resolve("products").toAbsolutePath().normalize()

            val dateDir = "${today.year}/${today.monthValue.toString().padStart(2, '0')}/${today.dayOfMonth.toString().padStart(2, '0')}"
            val dir = base.resolve(dateDir)
            Files.createDirectories(dir)

            val originalName = file.originalFilename ?: "unknown"
            val uuid8 = UUID.randomUUID().toString().replace("-", "").take(8)
            val savedName = "${uuid8}_$originalName"

            val filePath = dir.resolve(savedName)
            Files.write(filePath, file.bytes, StandardOpenOption.CREATE_NEW)

            val relativePath = "$dateDir/$savedName"
            val url = "$BACKEND_BASE_URL/products/$relativePath"

            log.info("[FileStorage] [Save] Product 이미지 저장 완료 - path={}", filePath)
            url
        } catch (e: IOException) {
            log.error("Product 이미지 저장 실패", e)
            throw ServiceException(ErrorCode.FILE_STORAGE_FAILED)
        }
    }

    fun deleteProductImage(fileUrl: String?) {
        if (fileUrl.isNullOrEmpty()) {
            log.warn("삭제할 Product 이미지 URL이 비어있습니다.")
            return
        }

        try {
            val relativePath = when {
                fileUrl.startsWith(BACKEND_BASE_URL + PRODUCTS_URL_PREFIX) ->
                    fileUrl.substring((BACKEND_BASE_URL + PRODUCTS_URL_PREFIX).length)
                fileUrl.startsWith(PRODUCTS_URL_PREFIX) ->
                    fileUrl.substring(PRODUCTS_URL_PREFIX.length)
                else -> {
                    log.warn("예상치 못한 Product 이미지 URL 형식입니다: {}", fileUrl)
                    return
                }
            }

            val base = Paths.get(uploadDir).parent.resolve("products").toAbsolutePath().normalize()
            val filePath = base.resolve(relativePath).normalize()

            if (Files.exists(filePath)) {
                Files.delete(filePath)
                log.info("[Product Img Delete] 파일 삭제 완료: {}", filePath)
            } else {
                log.warn("[Product Img Delete] 삭제할 파일이 존재하지 않음: {}", filePath)
            }
        } catch (e: IOException) {
            log.error("[Product Img Delete] 파일 삭제 실패: {}", fileUrl, e)
        }
    }

    fun saveReviewImage(file: MultipartFile): String {
        if (file.isEmpty) {
            throw ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED)
        }

        val maxFileSizeBytes = 3 * 1024 * 1024L // 3MB

        if (file.size > maxFileSizeBytes) {
            throw ServiceException(ErrorCode.REVIEW_IMAGE_SIZE_EXCEEDED)
        }

        return try {
            val today = LocalDate.now()
            val base = Paths.get(uploadDir).parent.resolve("reviews").toAbsolutePath().normalize()

            val dateDir = "${today.year}/${today.monthValue.toString().padStart(2, '0')}/${today.dayOfMonth.toString().padStart(2, '0')}"
            val dir = base.resolve(dateDir)
            Files.createDirectories(dir)

            val originalName = file.originalFilename ?: "unknown"
            val uuid8 = UUID.randomUUID().toString().replace("-", "").take(8)
            val savedName = "${uuid8}_$originalName"

            val filePath = dir.resolve(savedName)
            Files.write(filePath, file.bytes, StandardOpenOption.CREATE_NEW)

            val relativePath = "$dateDir/$savedName"
            val url = "/reviews/$relativePath"

            log.info("리뷰 이미지 저장 완료: {}", filePath)
            url
        } catch (e: IOException) {
            log.error("리뷰 이미지 저장 실패", e)
            throw ServiceException(ErrorCode.REVIEW_IMAGE_SAVE_FAILED)
        }
    }

    private fun stripPdfExtension(name: String): String =
        if (name.endsWith(".pdf", ignoreCase = true)) name.dropLast(4) else name
}