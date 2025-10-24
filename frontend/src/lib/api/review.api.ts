// lib/api/review.api.ts
import api from './axios';
import { PageResponse } from '@/types/review.types';

export interface ReviewListItem {
    reviewId: number;
    productId: number;
    productTitle: string;
    productThumbnailUrl: string;
    rating: number;
    content: string;
    reviewImageUrls?: string[];
    createdDate: string;
    purchasedDate?: string;
  }
  
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
