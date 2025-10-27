'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { ProductListResponse } from '@/types/product.types';

interface ProductCardProps {
  product: ProductListResponse;
}

export default function ProductCard({ product }: ProductCardProps) {
  const router = useRouter();

  const handleCardClick = () => {
    router.push(`/product/${product.productId}`);
  };

  // ✅ 우선순위: thumbnailUrl → productImageUrls → productImageUrlList → productImages
  const imageUrl =
    product.thumbnailUrl ||
    product.productImageUrls?.[0] ||
    product.productImageUrlList?.[0] ||
    product.productImages?.[0]?.productImageUrl ||
    null;

  return (
    <div
      onClick={handleCardClick}
      className="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
    >
      {/* 상품 이미지 */}
      <div className="aspect-square bg-gray-100 flex items-center justify-center">
        {imageUrl ? (
          <img
            src={
              imageUrl.startsWith('http')
                ? imageUrl
                : `http://localhost:8080${imageUrl}`
            }
            alt={product.title}
            className="w-full h-full object-cover"
            onError={(e) => {
              (e.target as HTMLImageElement).src =
                'https://placehold.co/400x400/CCCCCC/FFFFFF?text=No+Image';
            }}
          />
        ) : (
          <div className="text-gray-400 text-sm">이미지 없음</div>
        )}
      </div>

      {/* 상품 정보 */}
      <div className="p-3">
        <h3 className="text-sm font-medium text-gray-800 truncate">
          {product.title}
        </h3>
        <p className="text-gray-600 text-sm mt-1">
          {product.isFree ? '무료' : `${product.price.toLocaleString()}원`}
        </p>
      </div>
    </div>
  );
}
