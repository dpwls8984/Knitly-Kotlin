'use client';

import { useSearchParams, useRouter } from 'next/navigation';
import { useEffect, useState, ChangeEvent } from 'react';
import { ReviewCreateRequest, ReviewCreateResponse } from '@/types/review.types';
import { createReview } from '@/lib/api/review.api';
import api from '@/lib/api/axios';

export default function ReviewWritePage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const productId = searchParams.get('productId');
  const [item, setItem] = useState<ReviewCreateResponse | null>(null);
  const [rating, setRating] = useState(0);
  const [content, setContent] = useState('');
  const [images, setImages] = useState<File[]>([]);

  // GET 상품 정보 (리뷰 폼용)
  useEffect(() => {
    if (!productId) return;
    const fetchProduct = async () => {
      try {
        const response = await api.get<ReviewCreateResponse>(`/products/${productId}/review`);
        setItem(response.data);
      } catch (error) {
        console.error('상품 정보 로딩 실패', error);
      }
    };
    fetchProduct();
  }, [productId]);

  if (!productId) return <div className="p-6 text-center text-gray-500">상품 정보가 없습니다.</div>;
  if (!item) return <div className="p-6 text-center text-gray-500">상품 정보를 불러오는 중입니다...</div>;

  const handleRatingChange = (value: number) => setRating(value);
  const handleContentChange = (e: ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value);
  const handleImageChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const newFiles = Array.from(e.target.files);
    setImages(prev => [...prev, ...newFiles].slice(0, 10));
  };
  const handleImageRemove = (index: number) => setImages(prev => prev.filter((_, i) => i !== index));

  const handleSubmit = async () => {
    if (!productId) return;
    try {
      const data: ReviewCreateRequest = { rating, content, images };
      await createReview(Number(productId), data);
      alert('리뷰가 등록되었습니다.');
      router.push('/mypage/review');
    } catch (error) {
      console.error('리뷰 등록 실패', error);
      alert('리뷰 등록에 실패했습니다.');
    }
  };

  return (
    <div className="font-sans p-4 min-h-screen">
      <h2 className="text-[#925C4C] text-2xl font-bold mb-6">리뷰 작성하기</h2>

      {/* 상품 정보 */}
      <div className="flex items-center gap-8 mb-6">
        <img
          src={item.productThumbnailUrl}
          alt={item.productTitle}
          className="w-24 h-24 object-cover rounded-lg"
        />
        <div>
          <div className="font-medium text-lg mb-1">{item.productTitle}</div>
        </div>
      </div>

      {/* 이미지 업로드 + 별점 + 리뷰 작성 */}
      <div className="bg-white p-4 rounded-lg flex flex-col gap-4">
        <div className="flex gap-4">
          <label className="w-24 h-24 bg-gray-100 flex items-center justify-center text-sm text-gray-500 rounded-lg cursor-pointer">
            이미지 추가
            <input type="file" multiple className="hidden" onChange={handleImageChange} />
          </label>

          <div className="flex flex-wrap gap-2">
            {images.map((file, idx) => (
              <div key={idx} className="w-24 h-24 relative">
                <img
                  src={URL.createObjectURL(file)}
                  alt={`preview-${idx}`}
                  className="w-full h-full object-cover rounded-lg"
                />
                <button
                  onClick={() => handleImageRemove(idx)}
                  className="absolute top-0 right-0 w-6 h-6 bg-black bg-opacity-50 text-white text-xs rounded-full flex items-center justify-center hover:bg-red-600"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-1 text-2xl mt-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <svg
              key={i}
              onClick={() => handleRatingChange(i)}
              className={`w-6 h-6 cursor-pointer ${i <= rating ? 'text-yellow-400' : 'text-gray-300'}`}
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.946a1 1 0 00.95.69h4.15c.969 0 1.371 1.24.588 1.81l-3.36 2.44a1 1 0 00-.364 1.118l1.287 3.945c.3.921-.755 1.688-1.54 1.118l-3.36-2.44a1 1 0 00-1.175 0l-3.36 2.44c-.784.57-1.838-.197-1.539-1.118l1.286-3.945a1 1 0 00-.364-1.118L2.075 9.373c-.783-.57-.38-1.81.588-1.81h4.15a1 1 0 00.95-.69l1.286-3.946z" />
            </svg>
          ))}
        </div>

        <textarea
          className="w-full h-32 p-2 resize-none focus:outline-none focus:ring-1 focus:ring-[#925C4C]"
          placeholder="리뷰를 작성해 주세요."
          value={content}
          onChange={handleContentChange}
        />

        <div className="flex justify-end">
          <button
            onClick={handleSubmit}
            className="bg-[#925C4C] text-white px-6 py-2 rounded-md hover:bg-[#7a483a]"
          >
            리뷰 등록
          </button>
        </div>
      </div>
    </div>
  );
}
