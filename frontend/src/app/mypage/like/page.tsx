'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getMyFavorites, removeFavorite } from '@/lib/api/like.api';
import { FavoriteProductItem } from '@/types/like.types';

export default function LikesPage() {
  const router = useRouter();
  const [products, setProducts] = useState<FavoriteProductItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const fetchFavorites = async () => {
      setLoading(true);
      try {
        const data = await getMyFavorites(page, 12);
        setProducts(data.content);
        setTotalPages(data.totalPages);
      } catch (err) {
        console.error('찜 목록 로딩 실패:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchFavorites();
  }, [page]);

  const handleCardClick = (productId: number) => {
    router.push(`/product/${productId}`);
  };

  // 찜 해제
  const handleUnlike = async (productId: number) => {
    try {
      await removeFavorite(productId);
      setProducts(prev => prev.filter(p => p.productId !== productId));
    } catch (error) {
      console.error('찜 해제 실패:', error);
    }
  };

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-7xl mx-auto px-4 py-8">
        <h2 className="text-[#925C4C] text-2xl font-bold mb-3">내 찜</h2>

        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]" />
          </div>
        ) : products.length === 0 ? (
          <div className="text-center text-gray-500 py-20">
            찜한 상품이 없습니다.
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {products.map((product) => (
                <div
                  key={product.productId}
                  className="relative bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
                >
                  <div onClick={() => handleCardClick(product.productId)}>
                    <div className="aspect-square bg-gray-100 flex items-center justify-center">
                      {product.thumbnailUrl ? (
                        <img
                          src={product.thumbnailUrl}
                          alt={product.productTitle}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="text-gray-400 text-sm">이미지 없음</div>
                      )}
                    </div>

                    <div className="p-4">
                      <h3 className="font-medium text-gray-900 text-sm line-clamp-2 mb-1">
                        {product.productTitle}
                      </h3>
                      <p className="text-xs text-gray-500">{product.sellerName}</p>
                    </div>
                  </div>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleUnlike(product.productId);
                    }}
                    className="absolute bottom-2 right-2"
                  >
                    <svg
                      className="w-6 h-6 text-[#925C4C] fill-current"
                      viewBox="0 0 24 24"
                      fill="currentColor"
                    >
                      <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 
                        3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 
                        14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 
                        6.86-8.55 11.54L12 21.35z" />
                    </svg>
                  </button>
                </div>
              ))}
            </div>

            <div className="flex justify-center mt-8">
              <button
                disabled={page <= 0}
                onClick={() => setPage(page - 1)}
                className="px-4 py-2 text-sm border rounded disabled:text-gray-300 disabled:border-gray-200"
              >
                이전
              </button>
              <span className="px-4 py-2 text-sm">
                {page + 1} / {totalPages}
              </span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
                className="px-4 py-2 text-sm border rounded disabled:text-gray-300 disabled:border-gray-200"
              >
                다음
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}