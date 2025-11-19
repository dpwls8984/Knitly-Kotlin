package com.mysite.knitly.domain.community.post.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class CommunityImageStorage(

    @Value("\${file.community.upload-dir:\${user.dir}/uploads}")
    private val uploadDir: String,

    @Value("\${file.community.upload.sub-dir:communitys}")
    private val subDir: String,

    @Value("\${file.community.public-prefix:/uploads/communitys}")
    private val publicPrefix: String
) {

    private val log = LoggerFactory.getLogger(CommunityImageStorage::class.java)

    private val ts: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")

    private fun resolveBaseDir(): Path {
        val configured = Paths.get(uploadDir)
        if (configured.isAbsolute) {
            return configured.normalize()
        }
        val userDirPath = Paths.get(System.getProperty("user.dir"))
        return userDirPath.resolve(configured).normalize()
    }

    fun saveImages(files: List<MultipartFile>?): List<String> {
        val savedPaths = mutableListOf<String>()
        if (files == null || files.isEmpty()) {
            return savedPaths
        }

        val today = LocalDate.now()
        val targetDir = resolveBaseDir().resolve(
            Paths.get(
                subDir,
                today.year.toString(),
                String.format("%02d", today.monthValue),
                String.format("%02d", today.dayOfMonth)
            )
        )

        Files.createDirectories(targetDir)

        for (file in files) {
            if (file == null || file.isEmpty) continue

            validateFile(file)

            val ext = getExtension(file.originalFilename)
            val baseName = stripExtension(file.originalFilename)
            val nowTs = LocalDateTime.now().format(ts)
            val newName = "guest_${nowTs}_${sanitizeBaseName(baseName, 40)}$ext"

            val target = targetDir.resolve(newName)
            file.transferTo(target.toFile())

            val relativeUrl =
                "$publicPrefix/${today.year}/${String.format("%02d", today.monthValue)}/${String.format("%02d", today.dayOfMonth)}/$newName"

            savedPaths.add(relativeUrl)
            log.info("[CommunityImageStorage] saved: {} -> {}", target, relativeUrl)
        }

        return savedPaths
    }

    private fun validateFile(file: MultipartFile) {
        val name = file.originalFilename
        val size = file.size

        val validName = name?.matches(Regex("(?i).+\\.(png|jpg|jpeg|gif|webp)$")) == true
        if (!validName) {
            throw IllegalArgumentException("지원하지 않는 이미지 형식입니다. (png, jpg, jpeg, gif, webp)")
        }

        if (size > 3L * 1024L * 1024L) {
            throw IllegalArgumentException("이미지 파일 크기는 3MB 이하만 업로드 가능합니다.")
        }
    }

    private fun getExtension(filename: String?): String {
        if (filename == null) return ""
        val dot = filename.lastIndexOf('.')
        return if (dot >= 0) filename.substring(dot) else ""
    }

    private fun stripExtension(filename: String?): String {
        if (filename == null) return "file"
        val dot = filename.lastIndexOf('.')
        return if (dot >= 0) filename.substring(0, dot) else filename
    }

    private fun sanitizeBaseName(base: String?, maxLen: Int): String {
        if (base == null || base.isBlank()) return "file"

        var cleaned = base.replace("\\s+".toRegex(), "_")
        cleaned = cleaned.replace("[^0-9A-Za-z가-힣._-]".toRegex(), "_")

        if (cleaned.length > maxLen) {
            cleaned = cleaned.substring(0, maxLen)
        }
        return cleaned
    }
}
