import api from '@/lib/api/axios';
import { PostListResponse, PostCategory } from '@//types/community.types';

// 게시글 목록 조회
export const getPosts = async (
  category?: PostCategory | null,
  query?: string,
  page: number = 0,
  size: number = 10
): Promise<PostListResponse> => {
  const params = new URLSearchParams();
  
  if (category) params.append('category', category);
  if (query) params.append('query', query);
  params.append('page', page.toString());
  params.append('size', size.toString());

  const response = await api.get(`/community/posts?${params}`);
  return response.data;
};
