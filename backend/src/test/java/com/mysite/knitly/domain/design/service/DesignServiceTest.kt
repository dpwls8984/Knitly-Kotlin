package com.mysite.knitly.domain.design.service

// AssertJ
import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.design.dto.DesignRequest
import com.mysite.knitly.domain.design.dto.DesignUploadRequest
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.design.util.FileValidator
import com.mysite.knitly.domain.design.util.LocalFileStorage
import com.mysite.knitly.domain.design.util.PdfGenerator
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension

// Mockito Kotlin 전용
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import org.mockito.kotlin.never
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.Collections
import java.util.Optional
import kotlin.jvm.java
import kotlin.test.Test


@ExtendWith(MockitoExtension::class)
class DesignServiceTest {

    @Mock
    private lateinit var designRepository: DesignRepository

    @Mock
    private lateinit var pdfGenerator: PdfGenerator

    @Mock
    private lateinit var localFileStorage: LocalFileStorage

    @Mock
    private lateinit var fileValidator: FileValidator

    @Mock
    private lateinit var redisProductService: com.mysite.knitly.domain.product.product.service.RedisProductService

    @Spy
    private var objectMapper: ObjectMapper = ObjectMapper()

    private lateinit var designService: DesignService

    @BeforeEach
    fun setUp() {
        designService = DesignService(
            designRepository = designRepository,
            pdfGenerator = pdfGenerator,
            localFileStorage = localFileStorage,
            objectMapper = objectMapper,
            fileValidator = fileValidator,
            redisProductService = redisProductService
        )
    }

    @Test
    @DisplayName("도안 생성 - 정상")
    fun createDesign_ok() {
        val user = testUser(1L)

        val req = DesignRequest(
            designName = "하트 패턴",
            gridData = fake10x10(),
            fileName = "하트패턴_샘플"
        )

        val pdf = byteArrayOf(1, 2, 3)

        whenever(pdfGenerator.generate(eq("하트 패턴"), any()))
            .thenReturn(pdf)

        whenever(localFileStorage.savePdfFile(eq(pdf), any()))
            .thenReturn("/files/2025/10/17/uuid_하트패턴.pdf")

        val saved = Design(
            designId = 10L,
            user = user,
            pdfUrl = "/files/2025/10/17/uuid_하트패턴.pdf",
            designState = DesignState.BEFORE_SALE,
            designType = null,
            designName = "하트 패턴",
            gridData = "[]"
        )

        whenever(designRepository.save(any<Design>()))
            .thenReturn(saved)
        val res = designService.createDesign(user, req)

        assertThat(res).isNotNull
        assertThat(res.designId).isEqualTo(10L)
        verify(pdfGenerator).generate(eq("하트 패턴"), any())
        verify(localFileStorage).savePdfFile(eq(pdf), any())
        verify(designRepository).save(any())
    }

    @Test
    @DisplayName("도안 생성 - 그리드 크기 불일치 시 실패")
    fun createDesign_invalidGrid() {
        val user = testUser(1L)

        val req = DesignRequest(
            designName = "x",
            gridData = listOf(listOf("A")),
            fileName = null
        )

        assertThatThrownBy { designService.createDesign(user, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_INVALID_GRID_SIZE)
        verifyNoInteractions(pdfGenerator, localFileStorage, designRepository)
    }

    private fun fake10x10(): List<List<String>> {
        return (0 until 10).map { Collections.nCopies(10, "◯") }
    }

    @Test
    @DisplayName("본인 도안 조회 - 정상")
    fun getMyDesigns_ok() {
        val user = testUser(1L)

        val design1 = Design(
            designId = 1L,
            user = user,
            pdfUrl = "/files/1.pdf",
            designState = DesignState.BEFORE_SALE,
            designType = null,
            designName = "도안1",
            gridData = "[]"   // 최소 더미 값
        )

        val design2 = Design(
            designId = 2L,
            user = user,
            pdfUrl = "/files/2.pdf",
            designState = DesignState.ON_SALE,
            designType = null,
            designName = "도안2",
            gridData = "[]"
        )

        whenever(designRepository.findByUser(user)).thenReturn(listOf(design1, design2))

        val list = designService.getMyDesigns(user)

        assertThat(list).hasSize(2)
        assertThat(list[0].designId).isEqualTo(2L)
        assertThat(list[0].designName).isEqualTo("도안2")
        assertThat(list[1].designId).isEqualTo(1L)
    }

    @Test
    @DisplayName("도안 삭제 - 본인 소유 + BEFORE_SALE → 파일 삭제 시도 후 DB 하드 삭제")
    fun deleteDesign_ok_beforeSale() {
        val user = testUser(1L)

        val design = Design(
            designId = 1L,
            user = user,
            pdfUrl = "/files/1.pdf",
            designState = DesignState.BEFORE_SALE,
            designType = null,
            designName = "도안1",
            gridData = "[]"
        )

        whenever(designRepository.findById(1L)).thenReturn(Optional.of(design))
        designService.deleteDesign(user, 1L)

        verify(localFileStorage, times(1)).deleteFile("/files/1.pdf")
        verify(designRepository, times(1)).delete(design)
    }

    @Test
    @DisplayName("도안 삭제 - 파일 삭제 실패해도 DB 삭제는 진행")
    fun deleteDesign_fileDeleteFails_butStillDeletesDb() {
        val user = testUser(1L)

        val design = Design(
            designId = 1L,
            user = user,
            pdfUrl = "/files/1.pdf",
            designState = DesignState.BEFORE_SALE,
            designType = null,
            designName = "도안1",
            gridData = "[]"
        )
        whenever(designRepository.findById(1L)).thenReturn(Optional.of(design))
        doThrow(RuntimeException("파일 삭제 실패"))
            .`when`(localFileStorage).deleteFile("/files/1.pdf")

        designService.deleteDesign(user, 1L)

        verify(localFileStorage, times(1)).deleteFile("/files/1.pdf")
        verify(designRepository, times(1)).delete(design)
    }

    @Test
    @DisplayName("도안 삭제 - 본인 아님 → DESIGN_UNAUTHORIZED_DELETE")
    fun deleteDesign_notOwner() {
        val owner = testUser(1L)
        val other = testUser(2L)

        val design = Design(
            designId = 1L,
            user = owner,
            pdfUrl = "/files/1.pdf",
            designState = DesignState.BEFORE_SALE,
            designType = null,
            designName = "도안1",
            gridData = "[]"
        )
        whenever(designRepository.findById(1L)).thenReturn(Optional.of(design))

        assertThatThrownBy { designService.deleteDesign(other, 1L) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_UNAUTHORIZED_DELETE)

        verify(designRepository, never()).delete(any())
        verifyNoInteractions(localFileStorage)
    }

    @Test
    @DisplayName("도안 삭제 - 상태가 ON_SALE/STOPPED → DESIGN_NOT_DELETABLE")
    fun deleteDesign_notDeletable_whenOnSaleOrStopped() {
        val user = testUser(1L)

        val design = Design(
            designId = 2L,
            user = user,
            pdfUrl = "/files/1.pdf",
            designState = DesignState.ON_SALE,
            designType = null,
            designName = "도안1",
            gridData = "[]"
        )
        whenever(designRepository.findById(2L)).thenReturn(Optional.of(design))

        assertThatThrownBy { designService.deleteDesign(user, 2L) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_NOT_DELETABLE)

        verify(designRepository, never()).delete(any())
        verifyNoInteractions(localFileStorage)
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 정상")
    fun uploadPdfDesign_ok() {
        val user = testUser(1L)

        // validator 통과
        doNothing().`when`(fileValidator).validatePdfFile(any())

        val bytes = "PDF BYTES".toByteArray()
        val file: MultipartFile = MockMultipartFile("file", "sample.pdf", "application/pdf", bytes)

        whenever(localFileStorage.savePdfFile(any(), eq("샘플도안.pdf")))
            .thenReturn("/files/2025/10/21/abcd1234_sample.pdf")

        val saved = Design(
            designId = 100L,
            user = user,
            pdfUrl = "/files/2025/10/21/abcd1234_sample.pdf",
            designState = DesignState.BEFORE_SALE,
            designType = null,
            designName = "샘플도안",
            gridData = "[]"
        )
        whenever(designRepository.save(any<Design>()))
            .thenReturn(saved)
        val req = DesignUploadRequest("샘플도안", file)
        val res = designService.uploadPdfDesign(user, req)

        assertThat(res).isNotNull
        assertThat(res.designId).isEqualTo(100L)
        assertThat(res.pdfUrl).isEqualTo("/files/2025/10/21/abcd1234_sample.pdf")

        verify(fileValidator).validatePdfFile(file)
        verify(localFileStorage).savePdfFile(any(), eq("샘플도안.pdf"))
        verify(designRepository).save(any())
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 파일 유효성 실패 → DESIGN_FILE_INVALID_TYPE")
    fun uploadPdfDesign_invalidFile() {
        val user = testUser(1L)

        val file: MultipartFile = MockMultipartFile("file", "origin-name.pdf", "application/pdf", byteArrayOf(1, 2, 3))
        doThrow(ServiceException(ErrorCode.DESIGN_FILE_INVALID_TYPE))
            .`when`(fileValidator).validatePdfFile(any())

        val req = DesignUploadRequest("도안", file)

        assertThatThrownBy { designService.uploadPdfDesign(user, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_FILE_INVALID_TYPE)

        verifyNoInteractions(localFileStorage, designRepository)
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 파일 읽기 실패(getBytes) → DESIGN_FILE_SAVE_FAILED")
    fun uploadPdfDesign_ioOnGetBytes() {
        val user = testUser(1L)

        doNothing().`when`(fileValidator).validatePdfFile(any())

        val file = mock(MultipartFile::class.java)
        whenever(file.originalFilename).thenReturn("sample.pdf")
        whenever(file.bytes).thenThrow(IOException("boom"))

        val req = DesignUploadRequest("도안", file)

        assertThatThrownBy { designService.uploadPdfDesign(user, req) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_FILE_SAVE_FAILED)

        verifyNoInteractions(localFileStorage, designRepository)
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 도안명 공백 → 원본 파일명 기반 저장")
    fun uploadPdfDesign_blankName() {
        val user = testUser(1L)

        doNothing().`when`(fileValidator).validatePdfFile(any())

        val file: MultipartFile = MockMultipartFile("file", "origin-name.pdf", "application/pdf", byteArrayOf(1, 2, 3))
        whenever(localFileStorage.savePdfFile(any(), eq("origin-name.pdf")))
            .thenReturn("/files/2025/10/21/zzzz_origin-name.pdf")

        whenever(designRepository.save(any<Design>())).thenAnswer { inv ->
            val d = inv.getArgument<Design>(0)
            Design(
                designId = 11L,
                user = d.user,
                pdfUrl = d.pdfUrl,
                designState = d.designState,
                designType = null,
                designName = d.designName,
                gridData = "[]"
            )
        }

        val req = DesignUploadRequest("   ", file)

        val res = designService.uploadPdfDesign(user, req)

        assertThat(res).isNotNull
        assertThat(res.designId).isEqualTo(11L)
        verify(localFileStorage).savePdfFile(any(), eq("origin-name.pdf"))
    }

    private fun testUser(
        id: Long = 1L,
        name: String = "유저$id",
        email: String = "user$id@test.com"
    ): User =
        User.builder()
            .userId(id)
            .name(name)
            .email(email)
            .provider(Provider.GOOGLE)
            .build()
}
