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