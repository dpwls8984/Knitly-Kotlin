package com.mysite.knitly.domain.design.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LocalFileStorageTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @DisplayName("로컬 저장 - URL 반환 및 URL → 실경로 복원 성공")
    fun savePdfFile_and_resolve_ok() {
        val storage = LocalFileStorage(
            uploadDir = tempDir.toString(),
            publicPrefix = "/files"
        )

        val pdfBytes = byteArrayOf(1, 2, 3)
        val url = storage.savePdfFile(pdfBytes, "testFile")

        assertThat(url).startsWith("/files/")

        // URL -> 실제 경로 복원
        val abs = storage.toAbsolutePathFromUrl(url)
        assertThat(Files.exists(abs)).isTrue()
        assertThat(Files.size(abs)).isEqualTo(3)
    }
}