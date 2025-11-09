// ========== path: frontend/src/lib/api/community.api.ts ==========
import api from '@/lib/api/axios';
import type {
  PostListResponse,
  PostCategory,
  PostDetail,
  CommentItem,
} from '@/types/community.types';

/** 배열/페이지 응답을 항상 배열로 정규화 */
function toArray<T>(data: any): T[] {
  if (Array.isArray(data)) return data as T[];
  if (data?.content && Array.isArray(data.content)) return data.content as T[];
  return [];
}

// 게시글 목록
export async function getPosts(
  category?: PostCategory | null,
  query?: string,
  page = 0,
  size = 10,
): Promise<PostListResponse> {
  const params = new URLSearchParams();
  if (category) params.append('category', category);
  if (query) params.append('query', query);
  params.append('page', String(page));
  params.append('size', String(size));

  const res = await api.get(`/community/posts?${params.toString()}`);
  const data = (res as any).data ?? res;
  const pageFixed = data?.page ?? data?.number ?? 0;
  return { ...data, page: pageFixed } as PostListResponse;
}

// 게시글 상세
export async function getPostDetail(postId: number): Promise<PostDetail> {
  const res = await api.get(`/community/posts/${postId}`);
  return ((res as any).data ?? res) as PostDetail;
}
/** 호환 별칭 */
export const getPost = getPostDetail;

// 댓글 목록
export async function getPostComments(
  postId: number,
  sort: 'asc' | 'desc' = 'asc',
): Promise<CommentItem[]> {
  const res = await api.get(`/community/posts/${postId}/comments`, { params: { sort } });
  const data = (res as any).data ?? res;
  return toArray<CommentItem>(data);
}
/** 호환 별칭 */
export const getComments = getPostComments;

// 댓글 생성
/** 백엔드 스키마: { postId, content, parentId? } */
export async function createComment(
  postId: number,
  content: string,
  parentId?: number,
): Promise<CommentItem> {
  const payload: any = { postId, content };
  if (typeof parentId === 'number' && Number.isFinite(parentId) && parentId > 0) {
    payload.parentId = parentId;
  }
  const res = await api.post(`/community/posts/${postId}/comments`, payload, {
    withCredentials: true,
  });
  return ((res as any).data ?? res) as CommentItem;
}

// 댓글 수정
export async function updateComment(commentId: number, content: string): Promise<void> {
  await api.patch(
    `/community/comments/${commentId}`,
    { content },
    { withCredentials: true },
  );
}

// 댓글 삭제
export async function deleteComment(commentId: number): Promise<void> {
  await api.delete(`/community/comments/${commentId}`, { withCredentials: true });
}

// ===================== 여기부터 수정된 부분 =====================
/** JSON 기반 간단 수정(텍스트만) → 항상 멀티파트 PUT로 전송(파일 0개) */
export async function updatePost(
  postId: number,
  data: { title?: string; content?: string; category?: PostCategory; imageUrls?: string[] },
) {
  const form = new FormData();
  form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  // images 파트는 비워둠(0개) → 백엔드 @RequestPart("data")만 처리

  const res = await api.put(`/community/posts/${postId}`, form, {
    withCredentials: true,
    transformRequest: [
      (d, headers) => {
        if (headers) delete (headers as any)['Content-Type']; // boundary 자동 설정
        return d;
      },
    ],
  });
  return (res as any).data ?? res;
}
// ===================== 수정 끝 =====================

/** 멀티파트 기반 수정(텍스트 + 이미지 동반) */
export async function updatePostWithImages(
  postId: number,
  data: { title?: string; content?: string; category: PostCategory; imageUrls: string[] },
  files: File[] = [],
) {
  const form = new FormData();
  // @RequestPart("data") 로 보낼 JSON
  form.append(
    'data',
    new Blob([JSON.stringify(data)], { type: 'application/json' }),
  );
  // @RequestPart("images") 로 보낼 파일들
  for (const f of files) form.append('images', f);

  const res = await api.put(`/community/posts/${postId}`, form, {
    withCredentials: true,
    transformRequest: [
      (d, headers) => {
        if (headers) delete (headers as any)['Content-Type']; // boundary 자동
        return d;
      },
    ],
  });
  return (res as any).data ?? res;
}

export async function deletePost(postId: number): Promise<void> {
  await api.delete(`/community/posts/${postId}`, { withCredentials: true });
}

// 게시글 생성(멀티파트)
export async function createPost(form: FormData) {
  const res = await api.post('/community/posts', form, {
    withCredentials: true,
    // boundary 자동 설정을 위해 CT 제거
    transformRequest: [
      (data, headers) => {
        if (headers) delete (headers as any)['Content-Type'];
        return data;
      },
    ],
  });
  return (res as any).data ?? res;
}
