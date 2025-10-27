import api from './axios';
import { ProductListResponse, PageResponse } from '@/types/product.types';

/**
 * 판매자의 상품 목록 조회
 */
export const getSellerProducts = async (
    userId: string,
    page: number = 0,
    size: number = 20,
    sort?: string
): Promise<PageResponse<ProductListResponse>> => {
    const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
    });

    if (sort) {
        params.append('sort', sort);
    }

    const response = await api.get(`/users/${userId}/products?${params}`);
    return response.data;
};

/**
 * 상품 수정
 */
export const updateProduct = async (productId: number, data: any) => {
    const response = await api.put(`/products/${productId}`, data);
    return response.data;
};

/**
 * 상품 삭제
 */
export const deleteProduct = async (productId: number) => {
    const response = await api.delete(`/products/${productId}`);
    return response.data;
};

/**
 * 스토어 설명 조회
 */
export const getStoreDescription = async (userId: string): Promise<string> => {
    const response = await api.get(`/userstore/${userId}/description`);
    return response.data.description;
};

/**
 * 스토어 설명 업데이트
 */
export const updateStoreDescription = async (
    userId: string,
    description: string
): Promise<void> => {
    await api.put(`/userstore/${userId}/description`, { description });
};


// 상품 목록 조회 
/**
 * 상품 카테고리 타입
 */
export type ProductCategory = 'TOP' | 'BOTTOM' | 'OUTER' | 'BAG' | 'ETC';

/**
 * 상품 필터 타입
 */
export type ProductFilterType = 'ALL' | 'FREE' | 'LIMITED';

/**
 * 상품 정렬 타입
 */
export type ProductSortType = 'POPULAR' | 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC';

/**
 * 상품 목록 조회 파라미터
 */
export interface ProductListParams {
    category?: ProductCategory;
    filter?: ProductFilterType;
    sort?: ProductSortType;
    page?: number;
    size?: number;
}

/**
 * 상품 상세 정보
 */
export interface ProductDetail {
    productId: number;
    title: string;
    description: string;
    productCategory: ProductCategory;
    sizeInfo: string;
    price: number;
    stockQuantity: number | null;
    likeCount: number;
    isLikedByUser: boolean;
    avgReviewRating: number | null;
    productImageUrls: string[];
}

/**
 * 전체 상품 목록 조회 (카테고리/필터/정렬 지원)
 * GET /products
 */
export const getProductList = async (
    params: ProductListParams = {}
): Promise<PageResponse<ProductListResponse>> => {
    const queryParams = new URLSearchParams();
    
    if (params.category) queryParams.append('category', params.category);
    if (params.filter) queryParams.append('filter', params.filter);
    if (params.sort) queryParams.append('sort', params.sort);
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());

    const response = await api.get(`/products?${queryParams.toString()}`);
    return response.data;
};

/**
 * 상품 상세 조회
 * GET /products/{productId}
 */
export const getProductDetail = async (productId: number): Promise<ProductDetail> => {
    const response = await api.get(`/products/${productId}`);
    return response.data;
};

/**
 * 상품 찜하기/취소
 * POST /products/{productId}/like
 * 
 * @description 로그인이 필요한 기능입니다. axios 인터셉터에서 자동으로 토큰을 추가합니다.
 */
export const toggleProductLike = async (productId: number): Promise<void> => {
    await api.post(`/products/${productId}/like`);
};
