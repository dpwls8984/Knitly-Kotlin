package com.mysite.knitly.domain.design.util

import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import lombok.extern.slf4j.Slf4j
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.min

@Component
class PdfGenerator {
    private val log = LoggerFactory.getLogger(PdfGenerator::class.java)

    companion object {
        private const val GRID_SIZE = 10
        private const val MARGIN = 36f    // A4 기준 상하좌우 여백 - 0.5 inch
        private const val TITLE_SIZE = 18f // 도안명 - 18pt
        private const val SYMBOL_SIZE = 14f // 격자 셀 안 기호 크기 - 14pt
    }

    fun generate(designName: String, gridData: List<List<String>>): ByteArray {
        return try {
            PDDocument().use { doc ->
                // 문서 메타
                val info = PDDocumentInformation().apply {
                    title = designName
                    author = "Knitly"
                }
                doc.documentInformation = info

                // 페이지 & 폰트
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)

                val font = loadFont(doc) // 한글 폰트 임베딩

                // 좌표/영역 계산
                val box = page.mediaBox
                val pageWidth = box.width
                val pageHeight = box.height

                val contentLeft = MARGIN
                val contentRight = pageWidth - MARGIN
                val contentTop = pageHeight - MARGIN
                val contentBottom = MARGIN

                PDPageContentStream(doc, page).use { cs ->
                    // 제목
                    val titleY = contentTop - 4
                    drawTextCentered(cs, font, TITLE_SIZE, designName, pageWidth / 2, titleY)

                    // 그리드 영역 크기 결정 (가로 폭 기준으로 셀 크기 산정)
                    val availableWidth = contentRight - contentLeft
                    val cellSize = minOf(36f, availableWidth / GRID_SIZE) // 기본 36pt, 필요 시 자동 축소
                    val gridWidth = cellSize * GRID_SIZE
                    val gridHeight = cellSize * GRID_SIZE

                    // 그리드 위치(가로 중앙)
                    val gridLeft = (pageWidth - gridWidth) / 2
                    val gridTop = titleY - 24f // 제목과의 간격

                    // 격자 라인
                    cs.setStrokingColor(Color.DARK_GRAY)
                    cs.setLineWidth(0.8f)
                    // 가로줄
                    for (r in 0..GRID_SIZE) {
                        val y = gridTop - r * cellSize
                        cs.moveTo(gridLeft, y)
                        cs.lineTo(gridLeft + gridWidth, y)
                    }
                    // 세로줄
                    for (c in 0..GRID_SIZE) {
                        val x = gridLeft + c * cellSize
                        cs.moveTo(x, gridTop)
                        cs.lineTo(x, gridTop - gridHeight)
                    }
                    cs.stroke()

                    // 셀 기호 출력 (중앙정렬)
                    cs.setNonStrokingColor(Color.BLACK)
                    for (r in 0 until GRID_SIZE) {
                        for (c in 0 until GRID_SIZE) {
                            val symbolType = gridData[r][c]
                            if (symbolType.isNotBlank()) {
                                val cx = gridLeft + c * cellSize + cellSize / 2
                                val cy = gridTop - r * cellSize - cellSize / 2 + 3f // 살짝 위 보정

                                // 기호 타입에 따라 다른 심볼 출력
                                val displaySymbol = convertSymbolToDisplay(symbolType)
                                drawTextCentered(cs, font, SYMBOL_SIZE, displaySymbol, cx, cy)
                            }
                        }
                    }

                    // 생성일 푸터
                    val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    drawTextRight(cs, font, 9f, "생성일: $ts", contentRight, contentBottom)
                }

                val bos = ByteArrayOutputStream()
                doc.save(bos)
                val pdf = bos.toByteArray()
                log.info("[Design] PDF 생성 완료: name='{}', size={} bytes", designName, pdf.size)
                pdf
            }
        } catch (e: Exception) {
            log.error("[Design] PDF 생성 실패 - name={}", designName, e)
            throw ServiceException(ErrorCode.DESIGN_PDF_GENERATION_FAILED)
        }
    }

    /**
     * 프론트엔드에서 받은 기호 타입을 PDF에 표시할 실제 문자로 변환
     */
    private fun convertSymbolToDisplay(symbolType: String?): String {
        return when (symbolType?.lowercase()) {
            "empty" -> "○"  // 빈 원 (U+25CB)
            "filled" -> "●"  // 채워진 원 (U+25CF)
            "x" -> "×"  // 곱하기 기호 (U+00D7)
            "v" -> "V"  // V
            "t" -> "T"  // T
            "plus" -> "+"  // +
            "a" -> "A"  // A
            else -> symbolType ?: ""  // 알 수 없는 경우 그대로 출력
        }
    }

    private fun loadFont(doc: PDDocument): PDType0Font {
        val res = ClassPathResource("fonts/NanumGothic-Regular.ttf")
        return res.inputStream.use { inputStream ->
            PDType0Font.load(doc, inputStream, true) // true: 폰트 임베딩
        }
    }

    private fun drawTextCentered(
        cs: PDPageContentStream,
        font: PDType0Font,
        size: Float,
        text: String,
        centerX: Float,
        centerY: Float
    ) {
        val textWidth = font.getStringWidth(text) / 1000 * size
        val x = centerX - textWidth / 2
        drawText(cs, font, size, text, x, centerY)
    }

    private fun drawTextRight(
        cs: PDPageContentStream,
        font: PDType0Font,
        size: Float,
        text: String,
        rightX: Float,
        y: Float
    ) {
        val textWidth = font.getStringWidth(text) / 1000 * size
        val x = rightX - textWidth
        drawText(cs, font, size, text, x, y)
    }

    private fun drawText(
        cs: PDPageContentStream,
        font: PDType0Font,
        size: Float,
        text: String,
        x: Float,
        y: Float
    ) {
        cs.beginText()
        cs.setFont(font, size)
        cs.newLineAtOffset(x, y)
        cs.showText(text)
        cs.endText()
    }
}