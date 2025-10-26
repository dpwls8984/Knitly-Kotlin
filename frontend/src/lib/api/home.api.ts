import api from './axios';
import { ProductListResponse } from '@/types/product.types';
import { LatestReviewItem, LatestPostItem, HomeSummaryResponse } from '@/types/home.types';

/**
 * 홈 화면 인기 상품 TOP5 조회
 * GET /home/popular/top5
 */
export const getPopularTop5 = async (): Promise<ProductListResponse[]> => {
    const response = await api.get('/home/popular/top5');
    return response.data;
};

/**
 * 최신 리뷰 조회
 * GET /home/latest/reviews
 */
export const getLatestReviews = async (): Promise<LatestReviewItem[]> => {
    const response = await api.get('/home/latest/reviews');
    return response.data;
};

/**
 * 최신 커뮤니티 글 조회
 * GET /home/latest/posts
 */
export const getLatestPosts = async (): Promise<LatestPostItem[]> => {
    const response = await api.get('/home/latest/posts');
    return response.data;
};

/**
 * 홈 요약 정보 (인기 상품 + 최신 리뷰 + 최신 글) 한번에 조회
 * GET /home/summary
 */
export const getHomeSummary = async (): Promise<HomeSummaryResponse> => {
    const response = await api.get('/home/summary');
    return response.data;
};
