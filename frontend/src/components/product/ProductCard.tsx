'use client';

import { useRouter } from 'next/navigation';
import { ProductListResponse } from '@/types/product.types';

interface ProductCardProps {
  product: ProductListResponse;
  onLikeToggle: (productId: number) => void;
}

export default function ProductCard({ product, onLikeToggle }: ProductCardProps) {
  const router = useRouter();
  const API_URL = process.env.NEXT_PUBLIC_API_URL || '';

  const handleCardClick = () => {
    router.push(`/product/${product.productId}`);
  };

  const handleAuthorClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    // TODO: 판매자 스토어 페이지로 이동 (아직 구현 전)
    alert('판매자 스토어 페이지로 이동합니다. (구현 예정)');
  };

  const handleLikeClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onLikeToggle(product.productId);
  };

  return (
    <div 
      onClick={handleCardClick}
      className="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
    >
      {/* 상품 이미지 */}
      <div className="aspect-square bg-gray-100 flex items-center justify-center">
        {product.thumbnailUrl ? (
          <img 
            src={`${API_URL}${product.thumbnailUrl}`}
            alt={product.title}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="text-gray-400 text-sm">이미지</div>
        )}
      </div>
      
      {/* 상품 정보 */}
      <div className="p-4 flex justify-between items-end">
        {/* 좌측: 상품명, 작가명, 가격 */}
        <div className="flex-1">
          <h3 className="font-medium text-gray-900 text-sm line-clamp-2 mb-1">
            {product.title}
          </h3>
          <button
            onClick={handleAuthorClick}
            className="text-xs text-gray-600 hover:text-[#925C4C] transition-colors block mb-2"
          >
            {product.sellerName}
          </button>
          {/* 🔥 가격 스타일 수정: 갈색(브랜드컬러) + 볼드 처리 */}
          <div className="text-base font-bold text-[#925C4C]">
            {product.isFree ? '무료' : `${product.price.toLocaleString()}원`}
          </div>
        </div>
        
        {/* 우측: 찜 버튼 + 찜 개수 (세로 배치) */}
        <div className="flex flex-col items-center gap-1 ml-4">
          <button
            onClick={handleLikeClick}
            className="flex-shrink-0"
          >
            <svg
              className="w-5 h-5 text-gray-400"
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
          <div className="text-xs text-gray-500">
            {product.likeCount}
          </div>
        </div>
      </div>
    </div>
  );
}
