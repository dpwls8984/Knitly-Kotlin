import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class FileValidator {
    private val log = LoggerFactory.getLogger(FileValidator::class.java)

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        private const val ALLOWED_CONTENT_TYPE = "application/pdf"
        private val ALLOWED_EXTENSIONS = arrayOf(".pdf")
    }

    // pdf 파일 유효성 검증
    fun validatePdfFile(file: MultipartFile?) {
        // 1. 파일 존재 여부
        if (file == null || file.isEmpty) {
            throw ServiceException(ErrorCode.DESIGN_FILE_EMPTY)
        }

        // 2. 파일 크기 검증
        if (file.size > MAX_FILE_SIZE) {
            log.warn("파일 크기 초과: {} bytes (최대: {} bytes)", file.size, MAX_FILE_SIZE)
            throw ServiceException(ErrorCode.DESIGN_FILE_SIZE_EXCEEDED)
        }

        // 3. Content-Type 검증
        val contentType = file.contentType
        if (contentType == null || contentType != ALLOWED_CONTENT_TYPE) {
            log.warn("잘못된 파일 타입: {}", contentType)
            throw ServiceException(ErrorCode.DESIGN_FILE_INVALID_TYPE)
        }

        // 4. 확장자 검증
        val originalFilename = file.originalFilename
        if (originalFilename == null || !hasValidExtension(originalFilename)) {
            log.warn("잘못된 파일 확장자: {}", originalFilename)
            throw ServiceException(ErrorCode.DESIGN_FILE_INVALID_TYPE)
        }

        log.debug("파일 검증 통과: name={}, size={} bytes", originalFilename, file.size)
    }

    private fun hasValidExtension(filename: String): Boolean {
        val lowerFilename = filename.lowercase()
        return ALLOWED_EXTENSIONS.any { lowerFilename.endsWith(it) }
    }
}