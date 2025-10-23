'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

interface FavoriteProductItem {
  productId: number;
  productTitle: string;
  thumbnailUrl: string;
  price: number;
  averageRating: number;
  likedAt: string;
  isLiked: boolean;
}

export default function LikesPage() {
  const router = useRouter();
  const [products, setProducts] = useState<FavoriteProductItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchLikes = async () => {
      setLoading(true);
      try {
        const res = await fetch('/mocks/data/likes.json');
        const data: FavoriteProductItem[] = await res.json();
        setProducts(data.map(p => ({ ...p, isLiked: true })));
      } catch (error) {
        console.error('찜 목록 로딩 실패:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchLikes();
  }, []);

  const handleCardClick = (productId: number) => {
    router.push(`/product/${productId}`);
  };

  const handleLikeToggle = (productId: number) => {
    setProducts(prev =>
      prev.map(p =>
        p.productId === productId
          ? { ...p, isLiked: !p.isLiked }
          : p
      )
    );
  };

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-7xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold mb-6">내 찜 목록</h1>

        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
          </div>
        ) : products.length === 0 ? (
          <div className="text-center text-gray-500 py-20">
            찜한 상품이 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((product) => (
              <div
                key={product.productId}
                onClick={() => handleCardClick(product.productId)}
                className="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
              >
                {/* 상품 이미지 */}
                <div className="aspect-square bg-gray-100 flex items-center justify-center">
                  {product.thumbnailUrl ? (
                    <img
                      src={product.thumbnailUrl}
                      alt={product.productTitle}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="text-gray-400 text-sm">이미지</div>
                  )}
                </div>

                {/* 상품 정보 + 찜 버튼 */}
                <div className="p-4 flex justify-between items-end">
                  {/* 상품 정보 */}
                  <div className="flex-1">
                    <h3 className="font-medium text-gray-900 text-sm line-clamp-2 mb-1">
                      {product.productTitle}
                    </h3>
                    <div className="text-sm font-bold text-[#925C4C]">
                      {product.price.toLocaleString()}원
                    </div>
                  </div>

                  {/* 찜 버튼 + 별 1개 */}
                  <div className="flex flex-col items-center gap-1 ml-4">
                    <div className="flex items-center gap-1 mb-1">
                      <span className="text-yellow-500 text-sm">★</span>
                      <span className="text-black text-xs ml-1">{product.averageRating.toFixed(1)}</span>
                    </div>

                    {/* 찜 버튼 */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleLikeToggle(product.productId);
                      }}
                      className="flex-shrink-0"
                    >
                      <svg
                        className={`w-5 h-5 ${product.isLiked ? 'text-[#925C4C] fill-current' : 'text-gray-400'}`}
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                        />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
