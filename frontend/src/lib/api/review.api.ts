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
  
  export const getMyReviews = async (page = 0, size = 10): Promise<PageResponse<ReviewListItem>> => {
    const response = await api.get(`/mypage/reviews`, { params: { page, size } });
    return response.data;
  };
  
  export const deleteReview = async (reviewId: number) => {
    await api.delete(`/mypage/reviews/${reviewId}`);
  };