package com.mysite.knitly.domain.design.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.dto.DesignListResponse;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.dto.DesignUploadRequest;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.util.FileValidator;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.design.util.PdfGenerator;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepository;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DesignServiceTest {

    @Mock
    DesignRepository designRepository;
    @Mock
    PdfGenerator pdfGenerator;
    @Mock
    LocalFileStorage localFileStorage;
    @Mock
    FileValidator fileValidator;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    DesignService designService;


    @Test
    @DisplayName("도안 생성 - 정상")
    void createDesign_ok() {
        User user = User.builder().userId(1L).build();

        DesignRequest req = new DesignRequest(
                "하트 패턴",
                fake10x10(),
                "하트패턴_샘플"
        );

        byte[] pdf = new byte[]{1,2,3};
        when(pdfGenerator.generate(eq("하트 패턴"), any())).thenReturn(pdf);
        when(localFileStorage.savePdfFile(eq(pdf), anyString()))
                .thenReturn("/files/2025/10/17/uuid_하트패턴.pdf");

        Design saved = Design.builder()
                .designId(10L)
                .user(user)
                .designName("하트 패턴")
                .pdfUrl("/files/2025/10/17/uuid_하트패턴.pdf")
                .gridData("[]")
                .designState(DesignState.BEFORE_SALE)
                .build();

        when(designRepository.save(any(Design.class))).thenReturn(saved);

        DesignResponse res = designService.createDesign(user, req);

        assertThat(res).isNotNull();
        assertThat(res.designId()).isEqualTo(10L);
        verify(pdfGenerator).generate(eq("하트 패턴"), any());
        verify(localFileStorage).savePdfFile(eq(pdf), anyString());
        verify(designRepository).save(any(Design.class));
    }

    @Test
    @DisplayName("도안 생성 - 그리드 크기 불일치 시 실패")
    void createDesign_invalidGrid() {
        User user = User.builder().userId(1L).build();

        DesignRequest req = new DesignRequest(
                "x",
                List.of(List.of("A")),
                null
        );

        assertThatThrownBy(() -> designService.createDesign(user, req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_INVALID_GRID_SIZE);
        verifyNoInteractions(pdfGenerator, localFileStorage, designRepository);
    }

    private List<List<String>> fake10x10() {
        return java.util.stream.IntStream.range(0,10)
                .mapToObj(r -> java.util.Collections.nCopies(10, "◯"))
                .toList();
    }


    @Test
    @DisplayName("본인 도안 조회 - 정상")
    void getMyDesigns_ok() {
        User user = User.builder().userId(1L).build();

        Design design1 = Design.builder()
                .designId(1L)
                .user(user)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();

        Design design2 = Design.builder()
                .designId(2L)
                .user(user)
                .designName("도안2")
                .pdfUrl("/files/2.pdf")
                .designState(DesignState.ON_SALE)
                .build();

        when(designRepository.findByUser(user)).thenReturn(List.of(design1, design2));

        List<DesignListResponse> list = designService.getMyDesigns(user);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).designId()).isEqualTo(1L);
        assertThat(list.get(0).designName()).isEqualTo("도안1");
        assertThat(list.get(1).designId()).isEqualTo(2L);

    }

    @Test
    @DisplayName("도안 삭제 - 본인 소유 + BEFORE_SALE → 파일 삭제 시도 후 DB 하드 삭제")
    void deleteDesign_ok_beforeSale() throws Exception {
        User user = User.builder().userId(1L).name("유저1").build();

        Design design = Design.builder()
                .designId(1L)
                .user(user)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();

        when(designRepository.findById(1L)).thenReturn(Optional.of(design));
        designService.deleteDesign(user, 1L);

        verify(localFileStorage, times(1)).deleteFile("/files/1.pdf");
        verify(designRepository, times(1)).delete(design);
    }

    @Test
    @DisplayName("도안 삭제 - 파일 삭제 실패해도 DB 삭제는 진행")
    void deleteDesign_fileDeleteFails_butStillDeletesDb() throws Exception {
        User user = User.builder().userId(1L).build();

        Design design = Design.builder()
                .designId(1L)
                .user(user)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();

        when(designRepository.findById(1L)).thenReturn(Optional.of(design));
        doThrow(new IOException("io-실패")).when(localFileStorage).deleteFile("/files/1.pdf");

        designService.deleteDesign(user, 1L);

        verify(localFileStorage, times(1)).deleteFile("/files/1.pdf");
        verify(designRepository, times(1)).delete(design);
    }

    @Test
    @DisplayName("도안 삭제 - 본인 아님 → DESIGN_UNAUTHORIZED_DELETE")
    void deleteDesign_notOwner() {
        User owner = User.builder().userId(1L).name("유저1").build();
        User other = User.builder().userId(2L).name("유저2").build();

        Design design = Design.builder()
                .designId(1L)
                .user(owner)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();
        when(designRepository.findById(1L)).thenReturn(Optional.of(design));

        assertThatThrownBy(() -> designService.deleteDesign(other, 1L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_UNAUTHORIZED_DELETE);

        verify(designRepository, never()).delete(any());
        verifyNoInteractions(localFileStorage);
    }

    @Test
    @DisplayName("도안 삭제 - 상태가 ON_SALE/STOPPED → DESIGN_NOT_DELETABLE")
    void deleteDesign_notDeletable_whenOnSaleOrStopped() {
        User user = User.builder().userId(1L).build();

        Design design = Design.builder()
                .designId(2L)
                .user(user)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.ON_SALE)
                .build();
        when(designRepository.findById(2L)).thenReturn(Optional.of(design));

        assertThatThrownBy(() -> designService.deleteDesign(user, 2L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_NOT_DELETABLE);

        verify(designRepository, never()).delete(any());
        verifyNoInteractions(localFileStorage);
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 정상")
    void uploadPdfDesign_ok() {
        User user = User.builder().userId(1L).build();

        // validator 통과
        doNothing().when(fileValidator).validatePdfFile(any());

        byte[] bytes = "PDF BYTES".getBytes();
        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", bytes);

        when(localFileStorage.savePdfFile(any(byte[].class), eq("샘플도안.pdf")))
                .thenReturn("/files/2025/10/21/abcd1234_sample.pdf");

        Design saved = Design.builder()
                .designId(100L)
                .user(user)
                .designName("샘플도안")
                .pdfUrl("/files/2025/10/21/abcd1234_sample.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();
        when(designRepository.save(any(Design.class))).thenReturn(saved);

        DesignUploadRequest req = new DesignUploadRequest("샘플도안", file);
        DesignResponse res = designService.uploadPdfDesign(user, req);

        assertThat(res).isNotNull();
        assertThat(res.designId()).isEqualTo(100L);
        assertThat(res.pdfUrl()).isEqualTo("/files/2025/10/21/abcd1234_sample.pdf");

        verify(fileValidator).validatePdfFile(file);
        verify(localFileStorage).savePdfFile(any(byte[].class), eq("샘플도안.pdf"));
        verify(designRepository).save(any(Design.class));
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 파일 유효성 실패 → DESIGN_FILE_INVALID_TYPE")
    void uploadPdfDesign_invalidFile() {
        User user = User.builder().userId(1L).build();

        MultipartFile file = new MockMultipartFile("file", "origin-name.pdf", "application/pdf", new byte[]{1, 2, 3});
        doThrow(new ServiceException(ErrorCode.DESIGN_FILE_INVALID_TYPE))
                .when(fileValidator).validatePdfFile(any());

        DesignUploadRequest req = new DesignUploadRequest("도안", file);

        assertThatThrownBy(() -> designService.uploadPdfDesign(user, req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_FILE_INVALID_TYPE);

        verifyNoInteractions(localFileStorage, designRepository);
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 파일 읽기 실패(getBytes) → DESIGN_FILE_SAVE_FAILED")
    void uploadPdfDesign_ioOnGetBytes() throws Exception {
        User user = User.builder().userId(1L).build();

        doNothing().when(fileValidator).validatePdfFile(any());

        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("sample.pdf");
        when(file.getBytes()).thenThrow(new IOException("boom"));

        DesignUploadRequest req = new DesignUploadRequest("도안", file);

        assertThatThrownBy(() -> designService.uploadPdfDesign(user, req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_FILE_SAVE_FAILED);

        verifyNoInteractions(localFileStorage, designRepository);
    }

    @Test
    @DisplayName("기존 PDF 업로드 - 도안명 공백 → 원본 파일명 기반 저장")
    void uploadPdfDesign_blankName() {
        User user = User.builder().userId(1L).build();

        doNothing().when(fileValidator).validatePdfFile(any());

        MultipartFile file = new MockMultipartFile("file", "origin-name.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(localFileStorage.savePdfFile(any(byte[].class), eq("origin-name.pdf")))
                .thenReturn("/files/2025/10/21/zzzz_origin-name.pdf");

        when(designRepository.save(any(Design.class))).thenAnswer(inv -> {
            Design d = inv.getArgument(0);
            return Design.builder()
                    .designId(11L)
                    .user(d.getUser())
                    .designName(d.getDesignName())
                    .pdfUrl(d.getPdfUrl())
                    .designState(d.getDesignState())
                    .build();
        });

        DesignUploadRequest req = new DesignUploadRequest("   ", file);

        DesignResponse res = designService.uploadPdfDesign(user, req);

        assertThat(res).isNotNull();
        assertThat(res.designId()).isEqualTo(11L);
        verify(localFileStorage).savePdfFile(any(byte[].class), eq("origin-name.pdf"));
    }

}

