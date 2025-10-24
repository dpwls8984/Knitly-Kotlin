import api from './axios';
import { FavoriteProductItem, PageResponse } from '@/types/like.types';

// 내가 찜한 상품 목록 조회
export const getMyFavorites = async (
  page = 0,
  size = 12
): Promise<PageResponse<FavoriteProductItem>> => {
  const res = await api.get('/mypage/favorites', { params: { page, size } });
  return res.data;
};

// 찜 해제
export const removeFavorite = async (productId: number): Promise<void> => {
  await api.delete(`/products/${productId}/like`);
};