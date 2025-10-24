// lib/api/review.api.ts
import api from './axios';
import { PageResponse, ReviewListItem, ProductReviewItem } from '@/types/review.types';

  //내가 쓴 리뷰 조회
  export const getMyReviews = async (page = 0, size = 10): Promise<PageResponse<ReviewListItem>> => {
    const response = await api.get(`/mypage/reviews`, { params: { page, size } });
    return response.data;
  };
  
  //리뷰 삭제
  export const deleteReview = async (reviewId: number) => {
    await api.delete(`/reviews/${reviewId}`);
  };

  //리뷰 등록
export const createReview = async (productId: number, data: { rating: number; content: string; images?: File[] }) => {
  const formData = new FormData();
  formData.append('rating', String(data.rating));
  formData.append('content', data.content);

  if (data.images && data.images.length > 0) {
    data.images.forEach(file => formData.append('reviewImageUrls', file));
  }

  const response = await api.post(`/products/${productId}/reviews`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });

  return response.data;
};

//특정 상품 리뷰 목록 조회
export const getProductReviews = async (
  productId: number,
  page = 0,
  size = 10
): Promise<PageResponse<ProductReviewItem>> => {
  const response = await api.get(`/products/${productId}/reviews`, {
    params: { page, size },
  });
  return response.data;
};
