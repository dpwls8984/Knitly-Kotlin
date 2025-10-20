package com.mysite.knitly.domain.design.util;

import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@Component
public class PdfGenerator {
    private static final int GRID_SIZE = 10;
    private static final float MARGIN = 36f;    // A4 기준 상하좌우 여백 - 0.5 inch
    private static final float TITLE_SIZE = 18f; // 도안명 - 18pt
    private static final float SYMBOL_SIZE = 11f; // 격자 셀 안 기호 크기 - 11pt

    public byte[] generate(String designName, List<List<String>> gridData){
        try (PDDocument doc = new PDDocument()) {
            // 문서 메타
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle(designName);
            info.setAuthor("Knitly");

            // 페이지 & 폰트
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType0Font font = loadFont(doc); // 한글 폰트 임베딩

            // 좌표/영역 계산
            PDRectangle box = page.getMediaBox();
            float pageWidth = box.getWidth();
            float pageHeight = box.getHeight();

            float contentLeft = MARGIN;
            float contentRight = pageWidth - MARGIN;
            float contentTop = pageHeight - MARGIN;
            float contentBottom = MARGIN;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // 제목
                float titleY = contentTop - 4;
                drawTextCentered(cs, font, TITLE_SIZE, designName, pageWidth / 2, titleY);

                // 그리드 영역 크기 결정 (가로 폭 기준으로 셀 크기 산정)
                float availableWidth = contentRight - contentLeft;
                float cellSize = Math.min(36f, availableWidth / GRID_SIZE); // 기본 36pt, 필요 시 자동 축소
                float gridWidth = cellSize * GRID_SIZE;
                float gridHeight = cellSize * GRID_SIZE;

                // 그리드 위치(가로 중앙)
                float gridLeft = (pageWidth - gridWidth) / 2;
                float gridTop = titleY - 24f; // 제목과의 간격

                // 격자 라인
                cs.setStrokingColor(Color.DARK_GRAY);
                cs.setLineWidth(0.8f);
                // 가로줄
                for (int r = 0; r <= GRID_SIZE; r++) {
                    float y = gridTop - r * cellSize;
                    cs.moveTo(gridLeft, y);
                    cs.lineTo(gridLeft + gridWidth, y);
                }
                // 세로줄
                for (int c = 0; c <= GRID_SIZE; c++) {
                    float x = gridLeft + c * cellSize;
                    cs.moveTo(x, gridTop);
                    cs.lineTo(x, gridTop - gridHeight);
                }
                cs.stroke();

                // 셀 기호 출력 (중앙정렬)
                cs.setNonStrokingColor(Color.BLACK);
                for (int r = 0; r < GRID_SIZE; r++) {
                    for (int c = 0; c < GRID_SIZE; c++) {
                        String s = gridData.get(r).get(c);
                        if (s != null && !s.isBlank()) {
                            float cx = gridLeft + c * cellSize + cellSize / 2;
                            float cy = gridTop - r * cellSize - cellSize / 2 + 3f; // 살짝 위 보정
                            drawTextCentered(cs, font, SYMBOL_SIZE, s, cx, cy);
                        }
                    }
                }

                // 생성일 푸터
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                drawTextRight(cs, font, 9f, "생성일: " + ts, contentRight, contentBottom);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.save(bos);
            byte[] pdf = bos.toByteArray();
            log.info("PDF 생성 완료: name='{}', size={} bytes", designName, pdf.length);
            return pdf;

        } catch (Exception e) {
            log.error("PDF 생성 실패 - name={}", designName, e);
            // 프로젝트 공통 예외가 있으면 거기에 매핑하세요 (예: new ServiceException(ErrorCode.PDF_GENERATION_FAILED, e))
            throw new ServiceException(ErrorCode.DESIGN_PDF_GENERATION_FAILED);
        }
    }

    private PDType0Font loadFont(PDDocument doc) throws Exception {
        ClassPathResource res = new ClassPathResource("fonts/NanumGothic-Regular.ttf");
        try (InputStream in = res.getInputStream()) {
            return PDType0Font.load(doc, in, true); // true: 폰트 임베딩
        }
    }

    private void drawTextCentered(PDPageContentStream cs, PDType0Font font, float size,
                                  String text, float centerX, float centerY) throws Exception {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        float x = centerX - textWidth / 2;
        drawText(cs, font, size, text, x, centerY);
    }

    private void drawTextRight(PDPageContentStream cs, PDType0Font font, float size,
                               String text, float rightX, float y) throws Exception {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        float x = rightX - textWidth;
        drawText(cs, font, size, text, x, y);
    }

    private void drawText(PDPageContentStream cs, PDType0Font font, float size,
                          String text, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

}
