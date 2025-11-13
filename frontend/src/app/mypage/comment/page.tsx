'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { getMyComments, deleteComment } from '@/lib/api/mypage.api';
import type { MyCommentListItem } from '@/types/mypage.types';

function formatKST(input: any) {
  try {
    const d = new Date(input);
    if (isNaN(d.getTime())) return String(input ?? '');
    return d.toLocaleString('ko-KR');
  } catch {
    return String(input ?? '');
  }
}

export default function MyCommentPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading } = useAuthStore();

  const [comments, setComments] = useState<MyCommentListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [searchInput, setSearchInput] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [selectedComments, setSelectedComments] = useState<Set<number>>(new Set());

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      alert('로그인이 필요합니다.');
      router.push('/');
    }
  }, [isAuthenticated, authLoading, router]);

  const fetchComments = async (page = 0, query?: string) => {
    if (!user || !isAuthenticated) return;

    try {
      setIsLoading(true);
      setError(null);

      const res = await getMyComments(query, page, 10);
      setComments(res.content as any);
      setCurrentPage(res.page);
      setTotalPages(res.totalPages);
    } catch (err: any) {
      console.error('댓글 목록 조회 실패:', err);
      setError(err?.response?.data?.message ?? '댓글 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (user && isAuthenticated && !authLoading) {
      fetchComments(0, searchQuery);
    }
  }, [user, isAuthenticated, authLoading, searchQuery]);

  const handleSearch = () => {
    setSearchQuery(searchInput);
    setCurrentPage(0);
  };
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  const toggleOne = (commentId: number) => {
    setSelectedComments(prev => {
      const next = new Set(prev);
      if (next.has(commentId)) next.delete(commentId);
      else next.add(commentId);
      return next;
    });
  };

  const toggleAll = () => {
    const allIds = comments.map(c => Number(c.commentId));
    const allSelected = selectedComments.size === allIds.length && allIds.length > 0;
    setSelectedComments(allSelected ? new Set() : new Set(allIds));
  };

  const handleBatchDelete = async () => {
    if (selectedComments.size === 0) {
      alert('삭제할 댓글을 선택해주세요.');
      return;
    }
    if (!confirm(`선택한 ${selectedComments.size}개의 댓글을 삭제하시겠습니까?`)) return;

    try {
      await Promise.all(Array.from(selectedComments).map(id => deleteComment(id)));
      alert('선택한 댓글이 삭제되었습니다.');
      setSelectedComments(new Set());
      fetchComments(currentPage, searchQuery);
    } catch (err: any) {
      console.error('댓글 삭제 실패:', err);
      alert(err?.response?.data?.message ?? '댓글 삭제에 실패했습니다.');
    }
  };

  const handlePageChange = (p: number) => {
    setCurrentPage(p);
    fetchComments(p, searchQuery);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (authLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex justify-center items-center min-h-[60vh]">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]" />
        </div>
      </div>
    );
  }
  if (!user) return null;

  const allChecked = comments.length > 0 && selectedComments.size === comments.length;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="bg-white rounded-lg shadow-sm">
        <div className="bg-gray-100 py-8 px-6 text-center rounded-t-lg">
          <h1 className="text-3xl font-bold text-gray-900">내가 작성한 댓글 조회</h1>
        </div>

        <div className="p-6 bg-gray-50 border-b">
          <div className="flex gap-2 max-w-2xl mx-auto">
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={handleKeyPress}
              placeholder="댓글 내용으로 검색"
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#925C4C]"
            />
            <button
              onClick={handleSearch}
              className="px-6 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d] transition-colors"
            >
              검색
            </button>
          </div>
        </div>

        {error && (
          <div className="m-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
            {error}
          </div>
        )}

        <div className="p-6">
          {isLoading && comments.length === 0 ? (
            <div className="flex justify-center items-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]" />
            </div>
          ) : comments.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <p className="text-lg">작성한 댓글이 없습니다.</p>
            </div>
          ) : (
            <>
              <div className="flex justify-between items-center mb-4">
                <label
                  className="flex items-center gap-2 cursor-pointer"
                  onClick={(e) => e.stopPropagation()}
                >
                  <input
                    type="checkbox"
                    checked={allChecked}
                    onChange={toggleAll}
                    className="w-5 h-5 text-[#925C4C] border-gray-300 rounded focus:ring-[#925C4C]"
                  />
                  <span className="text-sm font-medium text-gray-700">전체 선택</span>
                </label>
                <button
                  onClick={handleBatchDelete}
                  disabled={selectedComments.size === 0}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    selectedComments.size === 0
                      ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                      : 'bg-red-500 text-white hover:bg-red-600'
                  }`}
                >
                  선택 항목 삭제
                </button>
              </div>

              <div className="space-y-3">
                {comments.map((c, idx) => {
                  const id = Number(c.commentId);
                  return (
                    <div
                      key={Number.isFinite(id) ? `c-${id}` : `c-row-${idx}`}
                      className="flex items-start gap-4 p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                    >
                      <input
                        type="checkbox"
                        checked={selectedComments.has(id)}
                        onChange={() => toggleOne(id)}
                        className="mt-1 w-5 h-5 text-[#925C4C] border-gray-300 rounded focus:ring-[#925C4C]"
                      />

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-3 mb-2">
                          <span className="text-sm font-medium text-gray-500">작성일</span>
                          <span className="text-sm text-gray-700">{formatKST(c.createdAt)}</span>
                        </div>
                        <div className="mb-1">
                          <p className="text-gray-800">{c.preview ?? ''}</p>
                        </div>
                      </div>

                      <button
                        onClick={() => router.push(`/community/posts/${c.postId}`)}
                        className="px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors whitespace-nowrap"
                      >
                        게시글 보기
                      </button>
                    </div>
                  );
                })}
              </div>

              {totalPages > 1 && (
                <div className="mt-8 flex justify-center items-center gap-2">
                  {Array.from({ length: totalPages }, (_, i) => (
                    <button
                      key={`p-${i}`}
                      onClick={() => handlePageChange(i)}
                      className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                        currentPage === i
                          ? 'bg-[#925C4C] text-white'
                          : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
