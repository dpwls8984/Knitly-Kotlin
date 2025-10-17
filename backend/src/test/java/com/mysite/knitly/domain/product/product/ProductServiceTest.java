package com.mysite.knitly.domain.product.product;

import com.mysite.knitly.domain.product.design.entity.Design;
import com.mysite.knitly.domain.product.design.repository.DesignRepositoryTmp;
import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest;
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.product.product.service.ProductService;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepositoryTmp;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepositoryTmp userRepository;
    @Mock
    private DesignRepositoryTmp designRepository;

    @Test
    @DisplayName("성공: 상품이 정상적으로 등록된다")
    void registerProduct_Success() {
        UUID sellerId = UUID.randomUUID();
        Long designId = 1L;
        User fakeUser = User.builder().userId(sellerId).build();
        Design fakeDesign = Design.builder().designId(designId).build();
        ProductRegisterRequest request = new ProductRegisterRequest("멋진 니트", "설명", "TOP", "Free", 30000.0, 50);

        when(userRepository.findById(sellerId)).thenReturn(Optional.of(fakeUser));
        when(designRepository.findById(designId)).thenReturn(Optional.of(fakeDesign));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product productToSave = invocation.getArgument(0);
            return Product.builder()
                    .productId(1L)
                    .title(productToSave.getTitle())
                    .description(productToSave.getDescription())
                    .productCategory(productToSave.getProductCategory())
                    .sizeInfo(productToSave.getSizeInfo())
                    .price(productToSave.getPrice())
                    .stockQuantity(productToSave.getStockQuantity())
                    .user(productToSave.getUser())
                    .design(productToSave.getDesign())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        var response = productService.registerProduct(sellerId, designId, request);

        assertThat(response.title()).isEqualTo("멋진 니트");
        assertThat(response.price()).isEqualTo(30000.0);
        verify(productRepository, times(1)).save(any(Product.class)); // save 메서드가 1번 호출되었는지 검증
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자가 상품 등록을 시도하면 예외가 발생한다")
    void registerProduct_Fail_UserNotFound() {
        // given
        UUID nonExistentUserId = UUID.randomUUID();
        Long designId = 1L;
        ProductRegisterRequest request = new ProductRegisterRequest("테스트", "설명", "TOP", "M", 10000.0, 10);

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty()); // 사용자를 찾을 수 없음

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            productService.registerProduct(nonExistentUserId, designId, request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 도안으로 상품 등록을 시도하면 예외가 발생한다")
    void registerProduct_Fail_DesignNotFound() {
        UUID sellerId = UUID.randomUUID();
        Long nonExistentDesignId = 999L;
        User fakeUser = User.builder().userId(sellerId).build();
        ProductRegisterRequest request = new ProductRegisterRequest("테스트", "설명", "TOP", "M", 10000.0, 10);

        when(userRepository.findById(sellerId)).thenReturn(Optional.of(fakeUser));
        when(designRepository.findById(nonExistentDesignId)).thenReturn(Optional.empty()); // 도안을 찾을 수 없음

        // when & then
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            productService.registerProduct(sellerId, nonExistentDesignId, request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DESIGN_NOT_FOUND);
    }

    // --- 상품 수정 테스트 (modifyProduct) ---

    @Test
    @DisplayName("성공: 상품 소유자가 정보를 정상적으로 수정한다")
    void modifyProduct_Success() {
        // given
        UUID ownerId = UUID.randomUUID();
        Long productId = 1L;
        User owner = User.builder().userId(ownerId).build();
        Product originalProduct = Product.builder()
                .productId(productId)
                .user(owner)
                .description("원본 설명")
                .productCategory("TOP")
                .build();
        ProductModifyRequest request = new ProductModifyRequest("수정된 설명", "BOTTOM", "L", 20);

        when(productRepository.findByIdWithUser(productId)).thenReturn(Optional.of(originalProduct));

        // when
        var response = productService.modifyProduct(ownerId, productId, request);

        // then
        assertThat(response.description()).isEqualTo("수정된 설명");
        assertThat(response.productCategory()).isEqualTo("BOTTOM");
        assertThat(response.stockQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("실패: 다른 사용자가 남의 상품 정보를 수정하려 하면 예외가 발생한다")
    void modifyProduct_Fail_Unauthorized() {
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Long productId = 1L;
        User owner = User.builder().userId(ownerId).build();
        Product targetProduct = Product.builder().productId(productId).user(owner).build();
        ProductModifyRequest request = new ProductModifyRequest("해킹", "ETC", "S", 0);

        when(productRepository.findByIdWithUser(productId)).thenReturn(Optional.of(targetProduct));

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            productService.modifyProduct(attackerId, productId, request); // 공격자 ID로 수정 시도
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_MODIFY_UNAUTHORIZED);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 상품을 수정하려 하면 예외가 발생한다")
    void modifyProduct_Fail_ProductNotFound() {
        UUID userId = UUID.randomUUID();
        Long nonExistentProductId = 999L;
        ProductModifyRequest request = new ProductModifyRequest("테스트", "TOP", "M", 10);

        when(productRepository.findByIdWithUser(nonExistentProductId)).thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            productService.modifyProduct(userId, nonExistentProductId, request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}