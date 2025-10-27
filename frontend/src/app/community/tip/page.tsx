'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getPosts } from '@/lib/api/community.api';
import { PostListItem, CATEGORY_LABELS } from '@/types/community.types';

export default function TipPage() {
  const router = useRouter();
  
  const [posts, setPosts] = useState<PostListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLastPage, setIsLastPage] = useState(false);
  
  const [searchQuery, setSearchQuery] = useState('');
  const [searchInput, setSearchInput] = useState('');

  const fetchPosts = async (page: number = 0, query?: string) => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await getPosts('TIP', query, page, 10);
      
      setPosts(response.content);
      setCurrentPage(response.page);
      setTotalPages(response.totalPages);
      setIsLastPage(response.last);
    } catch (err: any) {
      console.error('게시글 목록 조회 실패:', err);
      setError(err.response?.data?.message || '게시글 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts(0, searchQuery);
  }, [searchQuery]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    setSearchQuery(searchInput);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    fetchPosts(page, searchQuery);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handlePostClick = (postId: number) => {
    router.push(`/community/posts/${postId}`);
  };

  const handleWriteClick = () => {
    router.push('/community/posts/write');
  };

  return (
    <div className="bg-white rounded-lg shadow-sm">
      <div className="p-6 border-b border-gray-200">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-900">팁 게시판</h1>
          <button
            onClick={handleWriteClick}
            className="px-4 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d] transition-colors"
          >
            새 글쓰기
          </button>
        </div>

        <form onSubmit={handleSearch} className="flex gap-2">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="검색어를 입력하세요"
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#925C4C]"
          />
          <button
            type="submit"
            className="px-6 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d] transition-colors"
          >
            검색
          </button>
        </form>
      </div>

      <div className="p-6">
        {isLoading && posts.length === 0 ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
          </div>
        ) : error ? (
          <div className="text-center py-12">
            <p className="text-red-600">{error}</p>
            <button
              onClick={() => fetchPosts(0, searchQuery)}
              className="mt-4 px-4 py-2 bg-[#925C4C] text-white rounded-lg hover:bg-[#7a4a3d] transition-colors"
            >
              다시 시도
            </button>
          </div>
        ) : posts.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500">게시글이 없습니다.</p>
          </div>
        ) : (
          <>
            <div className="space-y-4">
              {posts.map((post) => (
                <div
                  key={post.id}
                  onClick={() => handlePostClick(post.id)}
                  className="border border-gray-200 rounded-lg p-4 hover:border-[#925C4C] cursor-pointer transition-colors"
                >
                  <div className="flex gap-4">
                    <div className="flex-shrink-0 w-32 h-32 bg-gray-100 rounded-lg overflow-hidden">
                      {post.thumbnailUrl ? (
                        <img
                          src={post.thumbnailUrl}
                          alt={post.title}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-gray-400">
                          이미지
                        </div>
                      )}
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="mb-2">
                        <span className="inline-block px-2 py-1 text-xs font-medium bg-gray-100 text-gray-700 rounded">
                          {CATEGORY_LABELS[post.category]}
                        </span>
                      </div>

                      <h3 className="text-lg font-bold text-gray-900 mb-2 line-clamp-1">
                        {post.title}
                      </h3>

                      <p className="text-gray-600 text-sm mb-3 line-clamp-2">
                        {post.excerpt}
                      </p>

                      <div className="flex items-center gap-4 text-sm text-gray-500">
                        <span>작성자 {post.authorDisplay}</span>
                        <span>{new Date(post.createdAt).toLocaleDateString('ko-KR', {
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}</span>
                        <span>댓글수 {post.commentCount}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex justify-center items-center gap-2 mt-8">
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                  className="px-3 py-1 rounded border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                >
                  이전
                </button>

                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  const startPage = Math.max(0, Math.min(currentPage - 2, totalPages - 5));
                  const page = startPage + i;
                  
                  return (
                    <button
                      key={page}
                      onClick={() => handlePageChange(page)}
                      className={`px-3 py-1 rounded ${
                        currentPage === page
                          ? 'bg-[#925C4C] text-white'
                          : 'border border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      {page + 1}
                    </button>
                  );
                })}

                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={isLastPage}
                  className="px-3 py-1 rounded border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                >
                  다음
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}