import api from './axios';
import { MyCommentListItem, MyPostListItemResponse, PageResponse } from '@/types/mypage.types';

/**
 * 내가 쓴 댓글 조회
 */
export const getMyComments = async (
  query?: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<MyCommentListItem>> => {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });

  if (query && query.trim()) {
    params.append('query', query.trim());
  }

  const response = await api.get(`/mypage/comments?${params}`);
  return response.data;
};

/**
 * 내가 쓴 글 조회
 */
export const getMyPosts = async (
  query?: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<MyPostListItemResponse>> => {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });

  if (query && query.trim()) {
    params.append('query', query.trim());
  }

  const response = await api.get(`/mypage/posts?${params}`);
  return response.data;
};

/**
 * 댓글 삭제
 */
export const deleteComment = async (commentId: number): Promise<void> => {
  await api.delete(`/community/comments/${commentId}`);
};

/**
 * 게시글 삭제
 */
export const deletePost = async (postId: number): Promise<void> => {
  await api.delete(`/community/posts/${postId}`);
};
