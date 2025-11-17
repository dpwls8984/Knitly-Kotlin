package com.mysite.knitly.domain.design.util

import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class PdfGeneratorTest {
    private val generator = PdfGenerator()

    @Test
    @DisplayName("PDF 생성 - 정상")
    fun generate_ok_returnsPdfBytes() {
        val pdf = generator.generate("테스트 도안", fake10x10())

        // PDF 시그니처 %PDF 확인
        assertThat(pdf).isNotEmpty()
        assertThat(String(pdf, 0, 4)).isEqualTo("%PDF")
        // 사이즈 대략 체크
        assertThat(pdf.size).isGreaterThan(100)
    }

    @Test
    @DisplayName("PDF 생성 실패 - 10x10 격자 형식 지키지 않은 경우")
    fun generate_invalidGrid_throwsServiceException() {
        assertThatThrownBy { generator.generate("깨진 도안", listOf(listOf("A"))) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DESIGN_PDF_GENERATION_FAILED)
    }

    private fun fake10x10(): List<List<String>> {
        return (0 until 10).map { Collections.nCopies(10, "◯") }
    }
}