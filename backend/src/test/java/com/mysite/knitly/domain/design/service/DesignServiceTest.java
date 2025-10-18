package com.mysite.knitly.domain.design.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.design.dto.DesignRequest;
import com.mysite.knitly.domain.design.dto.DesignResponse;
import com.mysite.knitly.domain.design.entity.DesignState;
import com.mysite.knitly.domain.design.entity.Design;
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

        // when
        DesignResponse res = designService.createDesign(userId, req);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getDesignId()).isEqualTo(10L);
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
}
