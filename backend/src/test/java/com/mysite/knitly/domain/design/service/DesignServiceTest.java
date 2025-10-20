/*
package com.mysite.knitly.domain.design.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.dto.DesignListResponse;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.repository.DesignRepository;
import com.mysite.knitly.domain.design.util.LocalFileStorage;
import com.mysite.knitly.domain.design.util.PdfGenerator;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepositoryTmp;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    UserRepositoryTmp userRepository;
    @Mock
    PdfGenerator pdfGenerator;
    @Mock
    LocalFileStorage localFileStorage;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    DesignService designService;


    @Test
    @DisplayName("도안 생성 - 정상")
    void createDesign_ok() {
        UUID userId = UUID.randomUUID();

        DesignRequest req = new DesignRequest(
                "하트 패턴",
                fake10x10(),
                "하트패턴_샘플"
        );

        User user = User.builder().userId(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        byte[] pdf = new byte[]{1,2,3};
        when(pdfGenerator.generate(eq("하트 패턴"), any())).thenReturn(pdf);

        when(localFileStorage.savePdfFile(pdf)).thenReturn("/files/2025/10/17/uuid.pdf");

        Design saved = Design.builder()
                .designId(10L)
                .user(user)
                .designName("하트 패턴")
                .pdfUrl("/files/2025/10/17/uuid.pdf")
                .gridData("[]")
                .designState(DesignState.BEFORE_SALE)
                .build();

        when(designRepository.save(any(Design.class))).thenReturn(saved);

        DesignResponse res = designService.createDesign(userId, req);

        assertThat(res).isNotNull();
        assertThat(res.designId()).isEqualTo(10L);
        verify(pdfGenerator).generate(eq("하트 패턴"), any());
        verify(localFileStorage).savePdfFile(pdf);
        verify(designRepository).save(any(Design.class));
    }

    @Test
    @DisplayName("도안 생성 - 그리드 크기 불일치 시 실패")
    void createDesign_invalidGrid() {
        UUID userId = UUID.randomUUID();
        DesignRequest req = new DesignRequest(
                "x",
                List.of(List.of("A")),
                null
        );

        assertThatThrownBy(() -> designService.createDesign(userId, req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_INVALID_GRID_SIZE);
        verifyNoInteractions(pdfGenerator, localFileStorage, designRepository);
    }

    @Test
    @DisplayName("도안 생성 - 사용자 없음")
    void createDesign_userNotFound() {
        UUID userId = UUID.randomUUID();
        DesignRequest req = new DesignRequest(
                "x",
                fake10x10(),
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> designService.createDesign(userId, req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private List<List<String>> fake10x10() {
        return java.util.stream.IntStream.range(0,10)
                .mapToObj(r -> java.util.Collections.nCopies(10, "◯"))
                .toList();
    }


    @Test
    @DisplayName("본인 도안 조회 - 정상")
    void getMyDesigns_ok() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();

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

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(designRepository.findByUser(user)).thenReturn(List.of(design1, design2));

        List<DesignListResponse> list = designService.getMyDesigns(userId);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).designId()).isEqualTo(1L);
        assertThat(list.get(0).designName()).isEqualTo("도안1");
        assertThat(list.get(1).designId()).isEqualTo(2L);

    }

    @Test
    @DisplayName("내 도안 조회 - 사용자 없음 → USER_NOT_FOUND")
    void getMyDesigns_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> designService.getMyDesigns(userId))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);

        verifyNoInteractions(designRepository);
    }

    @Test
    @DisplayName("도안 삭제 - 본인 소유 + BEFORE_SALE → 파일 삭제 시도 후 DB 하드 삭제")
    void deleteDesign_ok_beforeSale() throws Exception {
        UUID userId = UUID.randomUUID();
        User owner = User.builder().userId(userId).name("유저1").build();

        Design design = Design.builder()
                .designId(1L)
                .user(owner)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();

        when(designRepository.findById(1L)).thenReturn(Optional.of(design));

        designService.deleteDesign(userId, 1L);

        verify(localFileStorage, times(1)).deleteFile("/files/1.pdf");
        verify(designRepository, times(1)).delete(design);
    }

    @Test
    @DisplayName("도안 삭제 - 파일 삭제 실패해도 DB 삭제는 진행")
    void deleteDesign_fileDeleteFails_butStillDeletesDb() throws Exception {
        UUID userId = UUID.randomUUID();
        User owner = User.builder().userId(userId).build();
        Design design = Design.builder()
                .designId(1L)
                .user(owner)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();

        when(designRepository.findById(1L)).thenReturn(Optional.of(design));
        doThrow(new IOException("io-실패")).when(localFileStorage).deleteFile("/files/1.pdf");

        designService.deleteDesign(userId, 1L);

        verify(localFileStorage, times(1)).deleteFile("/files/1.pdf");
        verify(designRepository, times(1)).delete(design);
    }

    @Test
    @DisplayName("도안 삭제 - 본인 아님 → DESIGN_UNAUTHORIZED_DELETE")
    void deleteDesign_notOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        User owner = User.builder().userId(ownerId).name("유저1").build();

        Design design = Design.builder()
                .designId(1L)
                .user(owner)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.BEFORE_SALE)
                .build();
        when(designRepository.findById(1L)).thenReturn(Optional.of(design));

        assertThatThrownBy(() -> designService.deleteDesign(otherId, 1L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_UNAUTHORIZED_DELETE);

        verify(designRepository, never()).delete(any());
        verifyNoInteractions(localFileStorage);
    }

    @Test
    @DisplayName("도안 삭제 - 상태가 ON_SALE/STOPPED → DESIGN_NOT_DELETABLE")
    void deleteDesign_notDeletable_whenOnSaleOrStopped() {
        UUID userId = UUID.randomUUID();
        User owner = User.builder().userId(userId).build();

        Design design = Design.builder()
                .designId(2L)
                .user(owner)
                .designName("도안1")
                .pdfUrl("/files/1.pdf")
                .designState(DesignState.ON_SALE)
                .build();
        when(designRepository.findById(2L)).thenReturn(Optional.of(design));

        assertThatThrownBy(() -> designService.deleteDesign(userId, 2L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DESIGN_NOT_DELETABLE);

        verify(designRepository, never()).delete(any());
        verifyNoInteractions(localFileStorage);
    }

}
*/
