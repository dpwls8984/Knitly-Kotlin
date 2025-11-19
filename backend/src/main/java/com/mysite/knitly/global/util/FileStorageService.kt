package com.mysite.knitly.global.util

import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Service
class FileStorageService {

    private val uploadDir = "backend/uploads/designs/"
    private val urlPrefix = "/files/"

    /**
     * 파일을 저장하고 접근 URL을 반환합니다.
     * @param file 저장할 MultipartFile
     * @param domain 'product', 'review' 등 파일이 속한 도메인
     * @return 파일에 접근할 수 있는 URL
     */
    fun storeFile(file: MultipartFile, domain: String): String {
        val originalFilename = file.originalFilename
        if (!ImageValidator.isAllowedImageUrl(originalFilename)) {
            throw ServiceException(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED)
        }

        return try {
            val domainUploadDir = "$uploadDir$domain/"
            File(domainUploadDir).mkdirs() // 도메인별 디렉토리 생성

            // 확장자를 별도로 추출
            val extension = getFileExtension(originalFilename)
            val nameWithoutExt = getFileNameWithoutExtension(originalFilename)
            val sanitizedName = sanitizeFileName(nameWithoutExt)

            // UUID + sanitized된 이름 + 원본확장자
            val filename = "${UUID.randomUUID()}_$sanitizedName$extension"
            val path = Paths.get(domainUploadDir, filename)
            Files.write(path, file.bytes)
            log.info("[FileStorage] 파일 저장 완료 - original={}, saved={}", originalFilename, filename)

            "$urlPrefix$domain/$filename"

        } catch (e: IOException) {
            log.error("[FileStorage] 파일 저장 실패 - filename={}", originalFilename, e)
            throw ServiceException(ErrorCode.FILE_STORAGE_FAILED)
        }
    }

    /**
     * 파일 URL을 기반으로 실제 파일을 삭제합니다.
     * @param fileUrl 삭제할 파일의 URL
     */
    fun deleteFile(fileUrl: String?) {
        if (fileUrl.isNullOrEmpty()) {
            return
        }

        try {
            // URL에서 실제 파일 시스템 경로를 추출
            // 예: /resources/static/product/abc.jpg -> resources/static/product/abc.jpg
            val filePath = fileUrl.replaceFirst(urlPrefix, uploadDir)
            val path = Paths.get(filePath)

            Files.deleteIfExists(path)
            log.info("[FileStorage] 파일 삭제 성공 - filePath={}", filePath)

        } catch (e: IOException) {
            log.error("[FileStorage] 파일 삭제 실패 - fileUrl={}", fileUrl, e)
            // 파일 삭제 실패가 전체 트랜잭션을 롤백시킬 필요는 없으므로, 여기서는 예외를 다시 던지지 않고 로그만 남깁니다.
        }
    }

    /**
     * 파일 URL을 기반으로 실제 파일 내용을 byte 배열로 읽어옵니다.
     * @param fileUrl 읽어올 파일의 URL
     * @return 파일의 byte[]
     * @throws IOException 파일 읽기 실패 시
     */
    @Throws(IOException::class)
    fun loadFileAsBytes(fileUrl: String?): ByteArray {
        if (fileUrl.isNullOrEmpty()) {
            throw IOException("유효하지 않은 파일 URL입니다.")
        }

        return try {
            val filePath = fileUrl.replaceFirst(urlPrefix, uploadDir)
            val path = Paths.get(filePath)
            Files.readAllBytes(path)
        } catch (e: IOException) {
            log.error("[FileStorage] 파일 읽기 실패 - fileUrl={}", fileUrl, e)
            throw e // 예외를 상위로 던져서 Consumer가 처리하도록 함
        }
    }

    /**
     * 파일명에서 확장자 추출 (점 포함)
     * 예: "image.png" → ".png"
     */
    private fun getFileExtension(filename: String?): String {
        if (filename.isNullOrEmpty()) {
            return ""
        }

        val lastDotIndex = filename.lastIndexOf('.')
        return when {
            lastDotIndex == -1 || lastDotIndex == filename.length - 1 -> ""
            else -> filename.substring(lastDotIndex) // .png, .jpg 등
        }
    }

    /**
     * 파일명에서 확장자 제거
     * 예: "image.png" → "image"
     */
    private fun getFileNameWithoutExtension(filename: String?): String {
        if (filename.isNullOrEmpty()) {
            return "file"
        }

        val lastDotIndex = filename.lastIndexOf('.')
        return when {
            lastDotIndex == -1 -> filename
            else -> filename.substring(0, lastDotIndex)
        }
    }

    /**
     * 파일명 정리 (특수문자 제거, 공백 처리)
     * 확장자는 포함하지 않음
     */
    private fun sanitizeFileName(name: String?): String {
        if (name.isNullOrBlank()) {
            return "file"
        }

        // 특수문자를 언더스코어로 변경
        val sanitized = name.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_") // 공백을 언더스코어로

        // 길이 제한 (확장자 제외)
        return when {
            sanitized.length > 50 -> sanitized.substring(0, 50)
            else -> sanitized
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileStorageService::class.java)
    }
}