'use client';

import { useEffect, useMemo, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import {
  getPost as getPostDetail,
  getPostComments,
  createComment,
  updateComment,
  deleteComment,
  updatePost,
  updatePostWithImages,
  deletePost,
} from '@/lib/api/community.api';
import type { PostCategory } from '@/types/community.types';

// 절대주소 보정(정적 이미지)
const ASSET_ORIGIN = process.env.NEXT_PUBLIC_ASSET_ORIGIN || '';
const abs = (u?: string) =>
  !u ? '' : /^https?:\/\//i.test(u) ? u : `${ASSET_ORIGIN}${u.startsWith('/') ? '' : '/'}${u}`;

// [카테고리] 프리픽스 제거
const stripPrefix = (t: string) => t.replace(/^\[[^\]]+\]\s*/, '');

// ---- 타입(최소) ----
type PostDetailX = {
  postId: number;
  title: string;
  content: string;
  category: PostCategory;
  authorId?: number | null;
  userId?: number | null;
  author?: { userId?: number | null };
  authorName?: string;
  createdAt: string;
  images?: string[];
  thumbnailUrl?: string;
  imageUrls?: string[];
};

type FlatComment = {
  commentId: number | string;
  parentId: number | string | null;
  content: string;
  authorId: number | null;
  authorName?: string;
  createdAt: string;
  children?: FlatComment[];
};
type TreeComment = {
  commentId: number;
  parentId: number | null;
  content: string;
  authorId: number | null;
  createdAt: string;
  children: TreeComment[];
};

// ===== 댓글 응답 표준화 =====
const toArr = (x: any): any[] =>
  Array.isArray(x) ? x : x?.content && Array.isArray(x.content) ? x.content : [];

function looksTree(arr: any[]) {
  return Array.isArray(arr) && arr.length > 0 && Array.isArray(arr[0]?.children);
}
function fixNode(n: any): TreeComment {
  const id = Number(n.id ?? n.commentId);
  const pidRaw = n.parentId;
  const pid = pidRaw === null || pidRaw === '' ? null : Number(pidRaw);
  return {
    commentId: id,
    parentId: pid,
    content: n.content,
    authorId: n.authorId ?? null,
    createdAt: n.createdAt,
    children: Array.isArray(n.children) ? n.children.map(fixNode) : [],
  };
}
function normalizeToTree(input: any): TreeComment[] {
  const raw = toArr(input);
  if (looksTree(raw)) return raw.map(fixNode);
  const items = raw as FlatComment[];
  const byId = new Map<number, TreeComment>();
  const roots: TreeComment[] = [];
  items.forEach((it) => {
    const id = Number((it as any).id ?? it.commentId);
    const pid =
      it.parentId === null || it.parentId === '' ? null : Number(it.parentId);
    byId.set(id, {
      commentId: id,
      parentId: pid,
      content: it.content,
      authorId: it.authorId ?? null,
      createdAt: it.createdAt,
      children: [],
    });
  });
  byId.forEach((node) => {
    if (node.parentId && byId.has(node.parentId)) byId.get(node.parentId)!.children.push(node);
    else roots.push(node);
  });
  return roots;
}

export default function PostDetailPage() {
  const router = useRouter();
  const params = useParams<{ postId: string }>();
  const postIdNum = Number(params.postId || 0);

  const meId = useAuthStore((s) => s.user?.userId ?? null);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [loading, setLoading] = useState(true);
  const [post, setPost] = useState<PostDetailX | null>(null);
  const [comments, setComments] = useState<TreeComment[]>([]);

  const [newComment, setNewComment] = useState('');
  const [replyTo, setReplyTo] = useState<number | null>(null);
  const [replyText, setReplyText] = useState('');
  const [editCommentId, setEditCommentId] = useState<number | null>(null);
  const [editCommentText, setEditCommentText] = useState('');

  const [isEditingPost, setIsEditingPost] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [keepImages, setKeepImages] = useState<string[]>([]);
  const [newFiles, setNewFiles] = useState<File[]>([]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const nicknameMap = useMemo(() => new Map<number, number>(), []);
  const postAuthorId = useMemo(() => {
    if (!post) return null;
    return (post.authorId ?? post.userId ?? post.author?.userId ?? null) ?? null;
  }, [post]);
  const displayAnon = (authorId?: number | null) => {
    if (!authorId) return '익명의 털실';
    if (postAuthorId && Number(authorId) === Number(postAuthorId)) return '익명의 털실';
    if (!nicknameMap.has(authorId)) nicknameMap.set(authorId, nicknameMap.size + 1);
    return `익명의 털실 ${nicknameMap.get(authorId)}`;
  };

  const loadAll = async () => {
    if (!postIdNum) return;
    setLoading(true);
    try {
      const [p, cs] = await Promise.all([getPostDetail(postIdNum), getPostComments(postIdNum, sortOrder)]);
      setPost(p as any);
      setComments(normalizeToTree(cs));
      nicknameMap.clear();
      const title0 = (p as any)?.title ?? '';
      const content0 = (p as any)?.content ?? '';
      setEditTitle(title0);
      setEditContent(content0);
      const imgs: string[] =
        ((p as any)?.imageUrls && (p as any).imageUrls.length > 0 && (p as any).imageUrls) ||
        ((p as any)?.images && (p as any).images.length > 0 && (p as any).images) ||
        ((p as any)?.thumbnailUrl ? [(p as any).thumbnailUrl] : []);
      setKeepImages(imgs ?? []);
      setNewFiles([]);
      setErrorMsg(null);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [postIdNum, sortOrder]);

  if (loading) return <div className="max-w-4xl mx-auto p-6">로딩 중…</div>;
  if (!post)
    return (
      <div className="max-w-4xl mx-auto p-6 text-red-600">
        게시글을 불러올 수 없습니다.
        <button className="ml-3 underline" onClick={() => router.push('/community')}>목록으로</button>
      </div>
    );

  const authorIdForCheck = (post.authorId ?? post.userId ?? post.author?.userId ?? null) ?? null;
  const isMine = meId != null && authorIdForCheck != null && Number(authorIdForCheck) === Number(meId);

  const heroImages: string[] =
    (post.images && post.images.length > 0 && post.images) ||
    (post.imageUrls && post.imageUrls.length > 0 && post.imageUrls) ||
    (post.thumbnailUrl ? [post.thumbnailUrl] : []);

  const submitNewComment = async () => {
    if (!isAuthenticated) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/');
      return;
    }
    if (!newComment.trim()) return;
    await createComment(postIdNum, newComment.trim());
    setNewComment('');
    await loadAll();
  };

  const submitReply = async () => {
    if (!isAuthenticated) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/');
      return;
    }
    if (!replyTo || !replyText.trim()) return;
    await createComment(postIdNum, replyText.trim(), Number(replyTo));
    setReplyTo(null);
    setReplyText('');
    await loadAll();
  };

  const beginEditComment = (cid: number, current: string) => {
    setEditCommentId(Number(cid));
    setEditCommentText(current);
  };
  const commitEditComment = async () => {
    if (!isAuthenticated) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/');
      return;
    }
    if (!editCommentId || !editCommentText.trim()) return;
    await updateComment(Number(editCommentId), editCommentText.trim());
    setEditCommentId(null);
    setEditCommentText('');
    await loadAll();
  };
  const removeComment = async (cid: number) => {
    if (!isAuthenticated) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/');
      return;
    }
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return;
    await deleteComment(Number(cid));
    await loadAll();
  };

  const onClickEditPost = () => {
    setIsEditingPost(true);
    setErrorMsg(null);
  };
  const onCancelEditPost = () => {
    setIsEditingPost(false);
    setEditTitle(post.title);
    setEditContent(post.content);
    const imgs =
      (post.imageUrls && post.imageUrls.length > 0 && post.imageUrls) ||
      (post.images && post.images.length > 0 && post.images) ||
      (post.thumbnailUrl ? [post.thumbnailUrl] : []);
    setKeepImages(imgs ?? []);
    setNewFiles([]);
    setErrorMsg(null);
  };

  const toggleKeep = (url: string) => {
    setKeepImages((prev) =>
      prev.includes(url) ? prev.filter((u) => u !== url) : [...prev, url],
    );
  };

  const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    const files = Array.from(e.target.files ?? []);
    if (!files.length) return;
    setNewFiles((prev) => [...prev, ...files]);
    e.currentTarget.value = '';
  };

  const removeNewFile = (idx: number) => {
    setNewFiles((prev) => prev.filter((_, i) => i !== idx));
  };

  const onSaveEditPost = async () => {
    setErrorMsg(null);
    try {
      const finalKeep = keepImages.slice();
      const totalCount = finalKeep.length + newFiles.length;
      if (totalCount > 5) {
        setErrorMsg('이미지는 최대 5개까지 업로드할 수 있습니다.');
        return;
      }

      if (newFiles.length > 0 || JSON.stringify(finalKeep.sort()) !== JSON.stringify((post.imageUrls ?? []).slice().sort())) {
        await updatePostWithImages(postIdNum, {
          title: editTitle,
          content: editContent,
          category: post.category,
          imageUrls: finalKeep,
        }, newFiles);
      } else {
        await updatePost(postIdNum, { title: editTitle, content: editContent });
      }

      setIsEditingPost(false);
      await loadAll();
    } catch (e: any) {
      setErrorMsg(e?.response?.data?.error?.message ?? '수정 중 오류가 발생했습니다.');
    }
  };

  const onRemovePost = async () => {
    if (!isAuthenticated) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/');
      return;
    }
    if (!window.confirm('게시글을 삭제하시겠습니까?')) return;
    await deletePost(postIdNum);
    router.push('/community');
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-6 flex items-center gap-2">
        <span className="inline-flex items-center px-3 py-1 text-sm rounded-full bg-[#925C4C] text-white">
          {post.category === 'FREE' ? '자유' : post.category === 'QUESTION' ? '질문' : 'TIP'}
        </span>
        <h1 className="text-2xl font-bold ml-2">{stripPrefix(post.title)}</h1>
      </div>

      <div className="mb-4 text-sm text-gray-500 flex items-center justify-between">
        <div>
          작성자 <span className="font-medium">익명의 털실</span>
          <span className="mx-2">·</span>
          {new Date(post.createdAt).toLocaleString()}
        </div>
        <div className="flex gap-2">
          <button onClick={() => router.push('/community')} className="px-3 py-1 rounded border hover:bg-gray-50">
            목록으로
          </button>
          {isMine && !isEditingPost && (
            <>
              <button className="px-3 py-1 rounded border hover:bg-gray-50" onClick={onClickEditPost}>수정</button>
              <button className="px-3 py-1 rounded border hover:bg-gray-50" onClick={onRemovePost}>삭제</button>
            </>
          )}
        </div>
      </div>

      <div className="rounded-2xl border p-5 mb-10">
        {isEditingPost ? (
          <div className="space-y-4">
            {!!errorMsg && (
              <div className="p-3 rounded bg-red-50 text-red-600 text-sm">{errorMsg}</div>
            )}

            <input
              className="w-full px-3 py-2 border rounded-lg"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              placeholder="제목"
            />
            <textarea
              className="w-full min-h-[160px] px-3 py-2 border rounded-lg"
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
              placeholder="내용"
            />

            <div>
              <div className="mb-2 text-sm font-semibold">기존 이미지</div>
              {((post.imageUrls ?? heroImages) ?? []).length === 0 ? (
                <div className="text-sm text-gray-500">기존 이미지가 없습니다.</div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {((post.imageUrls ?? heroImages) as string[]).map((u) => (
                    <label
                      key={u}
                      className="flex gap-3 items-center border rounded-lg p-2"
                    >
                      <input
                        type="checkbox"
                        checked={keepImages.includes(u)}
                        onChange={() => toggleKeep(u)}
                      />
                      <img src={abs(u)} alt="" className="w-24 h-24 object-cover rounded" />
                      <span className="text-xs text-gray-600 break-all">{u}</span>
                    </label>
                  ))}
                </div>
              )}
            </div>

            <div>
              <div className="mb-2 text-sm font-semibold">새 이미지 추가</div>
              <input
                type="file"
                multiple
                accept="image/*"
                onChange={onPickFiles}
                className="block"
              />
              {newFiles.length > 0 && (
                <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {newFiles.map((f, i) => {
                    const url = URL.createObjectURL(f);
                    return (
                      <div key={`${f.name}-${i}`} className="border rounded-lg p-2">
                        <img src={url} alt="" className="w-full h-40 object-cover rounded" />
                        <div className="mt-1 flex items-center justify-between text-xs">
                          <span className="truncate">{f.name}</span>
                          <button
                            className="underline"
                            onClick={() => removeNewFile(i)}
                          >
                            제거
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
              <div className="mt-2 text-xs text-gray-500">
                총 이미지 수(기존 유지 + 신규)는 최대 5장까지 가능합니다.
              </div>
            </div>

            <div className="flex gap-2">
              <button className="px-4 py-2 rounded-lg border hover:bg-gray-50" onClick={onCancelEditPost}>취소</button>
              <button className="px-4 py-2 rounded-lg bg-[#925C4C] text-white hover:bg-[#7a4a3d]" onClick={onSaveEditPost}>
                저장
              </button>
            </div>
          </div>
        ) : (
          <>
            {heroImages.length > 0 && (
              <div className="mb-5 grid grid-cols-1 sm:grid-cols-2 gap-3">
                {heroImages.map((src, i) => (
                  <img key={`pimg-${i}-${src}`} src={abs(src)} alt="" className="w-full rounded-lg object-cover" />
                ))}
              </div>
            )}
            <div className="whitespace-pre-wrap leading-7 text-gray-800">{post.content}</div>
          </>
        )}
      </div>

      <div className="flex items-center justify-between mb-3">
        <div className="text-lg font-semibold">댓글 {countNodes(comments)}개</div>
        <div className="flex gap-4 text-sm text-gray-600">
          <button className={sortOrder === 'asc' ? 'underline' : ''} onClick={() => setSortOrder('asc')}>등록순</button>
          <button className={sortOrder === 'desc' ? 'underline' : ''} onClick={() => setSortOrder('desc')}>최신순</button>
        </div>
      </div>

      <div className="flex gap-2 mb-6">
        <input
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="댓글을 입력하세요"
          className="flex-1 px-4 py-2 border rounded-lg"
        />
        <button onClick={submitNewComment} className="px-4 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d]">
          등록
        </button>
      </div>

      <div className="space-y-3">
        {comments.map((n, i) => (
          <CommentNode
            key={`c-${n.commentId}-${i}`}
            node={n}
            depth={0}
            meId={meId}
            displayAnon={displayAnon}
            onReply={(id) => {
              const nid = Number(id);
              if (Number.isFinite(nid)) {
                setReplyTo(nid);
                setReplyText('');
                setEditCommentId(null);
              }
            }}
            onEdit={(id, current) => {
              const nid = Number(id);
              if (Number.isFinite(nid)) {
                setEditCommentId(nid);
                setEditCommentText(current);
                setReplyTo(null);
              }
            }}
            onDelete={(id) => removeComment(Number(id))}
            editCommentId={editCommentId}
            editText={editCommentText}
            setEditText={setEditCommentText}
            commitEdit={commitEditComment}
          />
        ))}
      </div>

      {replyTo && (
        <div className="mt-6 pl-5 border-l">
          <div className="text-sm text-gray-500 mb-2">답글 대상: #{replyTo}</div>
          <div className="flex gap-2">
            <input
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
              placeholder="답글을 입력하세요"
              className="flex-1 px-4 py-2 border rounded-lg"
            />
            <button onClick={submitReply} className="px-4 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d]">
              등록
            </button>
            <button onClick={() => { setReplyTo(null); setReplyText(''); }} className="px-4 py-2 border rounded-lg">
              취소
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function countNodes(nodes: TreeComment[]): number {
  let n = 0;
  const dfs = (arr: TreeComment[]) => {
    for (const it of arr) {
      n += 1;
      if (it.children?.length) dfs(it.children);
    }
  };
  dfs(nodes);
  return n;
}

function CommentNode({
  node,
  depth,
  meId,
  displayAnon,
  onReply,
  onEdit,
  onDelete,
  editCommentId,
  editText,
  setEditText,
  commitEdit,
}: {
  node: TreeComment;
  depth: number;
  meId: number | null;
  displayAnon: (authorId?: number | null) => string;
  onReply: (id: number) => void;
  onEdit: (id: number, current: string) => void;
  onDelete: (id: number) => void;
  editCommentId: number | null;
  editText: string;
  setEditText: (v: string) => void;
  commitEdit: () => void;
}) {
  const mine = meId != null && node.authorId != null && Number(node.authorId) === Number(meId);
  const isEditing = editCommentId != null && Number(editCommentId) === Number(node.commentId);

  return (
    <div className="border rounded-xl p-3">
      <div className="text-sm text-gray-500 mb-1 flex items-center justify-between">
        <div>
          {displayAnon(node.authorId)}
          <span className="mx-2">·</span>
          {new Date(node.createdAt).toLocaleString()}
        </div>
        <div className="flex gap-3 text-gray-500">
          <button className="underline" onClick={() => onReply(node.commentId)}>답글</button>
          {mine && !isEditing && (
            <>
              <button className="underline" onClick={() => onEdit(node.commentId, node.content)}>수정</button>
              <button className="underline" onClick={() => onDelete(node.commentId)}>삭제</button>
            </>
          )}
        </div>
      </div>

      {!isEditing ? (
        <div className="whitespace-pre-wrap text-gray-800">{node.content}</div>
      ) : (
        <div className="flex gap-2">
          <input
            value={editText}
            onChange={(e) => setEditText(e.target.value)}
            className="flex-1 px-3 py-2 border rounded-lg"
          />
            <button className="px-3 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d]" onClick={commitEdit}>
            저장
          </button>
        </div>
      )}

      {node.children?.length ? (
        <div className="mt-3 pl-5 border-l space-y-3">
          {node.children.map((c, i) => (
            <CommentNode
              key={`c-${c.commentId}-${depth + 1}-${i}`}
              node={c}
              depth={depth + 1}
              meId={meId}
              displayAnon={displayAnon}
              onReply={onReply}
              onEdit={onEdit}
              onDelete={onDelete}
              editCommentId={editCommentId}
              editText={editText}
              setEditText={setEditText}
              commitEdit={commitEdit}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}
