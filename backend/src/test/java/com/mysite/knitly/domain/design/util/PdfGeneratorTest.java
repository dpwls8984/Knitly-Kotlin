package com.mysite.knitly.domain.design.util;

import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PdfGeneratorTest {
    private final PdfGenerator generator = new PdfGenerator();

    @Test
    @DisplayName("PDF 생성 - 정상")
    void generate_ok_returnsPdfBytes() {
        byte[] pdf = generator.generate("테스트 도안", fake10x10());

        // PDF 시그니처 %PDF 확인
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        // 사이즈 대략 체크
        assertThat(pdf.length).isGreaterThan(100);
    }

    @Test
    @DisplayName("PDF 생성 실패 - 10x10 격자 형식 지키지 않은 경우")
    void generate_invalidGrid_throwsServiceException() {
        assertThatThrownBy(() -> generator.generate("깨진 도안", List.of(List.of("A"))))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DESIGN_PDF_GENERATION_FAILED);
    }

    private static List<List<String>> fake10x10() {
        return java.util.stream.IntStream.range(0, 10)
                .mapToObj(r -> java.util.Collections.nCopies(10, "◯"))
                .toList();
    }
}
