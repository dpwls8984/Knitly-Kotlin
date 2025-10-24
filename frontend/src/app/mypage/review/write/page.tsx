'use client';

import { useSearchParams, useRouter } from 'next/navigation';
import { useEffect, useState, ChangeEvent } from 'react';

interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  price: number;
  productImage?: string;
}

// Mock 데이터 (실제 API 연동 시 제거)
const mockOrderItems: OrderItem[] = [
  {
    id: 'itemA1',
    productId: 'prod_abc',
    productName: '따뜻한 겨울 스웨터 도안',
    price: 15000,
    productImage: 'https://via.placeholder.com/150',
  },
  {
    id: 'itemA2',
    productId: 'prod_def',
    productName: '아가일 패턴 양말 도안',
    price: 7000,
    productImage: 'https://via.placeholder.com/150',
  },
  {
    id: 'itemB1',
    productId: 'prod_ghi',
    productName: '초보자용 목도리 도안',
    price: 5000,
    productImage: 'https://via.placeholder.com/150',
  },
];

export default function ReviewWritePage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  // 쿼리에서 orderItemId와 orderDate 가져오기
  const orderItemId = searchParams.get('orderItemId');
  const orderDate = searchParams.get('orderDate') || '';

  const [item, setItem] = useState<OrderItem | null>(null);
  const [rating, setRating] = useState(0);
  const [content, setContent] = useState('');
  const [images, setImages] = useState<File[]>([]);

  // 선택한 상품 정보 가져오기
  useEffect(() => {
    if (orderItemId) {
      const found = mockOrderItems.find((i) => i.id === orderItemId);
      setItem(found || null);
    }
  }, [orderItemId]);

  if (!orderItemId)
    return <div className="p-6 text-center text-gray-500">주문 상품 정보가 없습니다.</div>;
  if (!item)
    return <div className="p-6 text-center text-gray-500">해당 상품 정보를 불러올 수 없습니다.</div>;

  // 별점 변경
  const handleRatingChange = (value: number) => setRating(value);

  // 리뷰 내용 변경
  const handleContentChange = (e: ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value);

  // 이미지 업로드
  const handleImageChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const newFiles = Array.from(e.target.files);
    const combined = [...images, ...newFiles].slice(0, 10); // 최대 10개
    setImages(combined);
  };

  // 이미지 삭제
  const handleImageRemove = (index: number) => {
    setImages((prev) => prev.filter((_, i) => i !== index));
  };

  // 리뷰 등록 (임시)
  const handleSubmit = () => {
    console.log({ rating, content, images });
    alert('리뷰가 등록되었습니다.');
    router.push('/mypage/review');
  };

  return (
    <div className="font-sans p-4 min-h-screen">
      <h2 className="text-[#925C4C] text-2xl font-bold mb-6">리뷰 작성하기</h2>

      {/* 상품 정보 */}
      <div className="flex items-center gap-8 mb-6">
        <img
          src={item.productImage || ''}
          alt="상품 썸네일"
          className="w-24 h-24 object-cover rounded-lg"
        />
        <div>
          <div className="font-medium text-lg mb-1">{item.productName}</div>
          <div className="text-sm text-gray-500">구매일: {orderDate}</div>
        </div>
      </div>

      {/* 이미지 업로드 + 미리보기 + 별점 + 리뷰 작성란 */}
      <div className="bg-white p-4 rounded-lg flex flex-col gap-4">
        {/* 이미지 업로드 */}
        <div className="flex gap-4">
          <label className="w-24 h-24 bg-gray-100 flex items-center justify-center text-sm text-gray-500 rounded-lg cursor-pointer">
            이미지 추가
            <input type="file" multiple className="hidden" onChange={handleImageChange} />
          </label>

          {/* 업로드한 이미지 미리보기 */}
          <div className="flex flex-wrap gap-2">
            {images.map((file, idx) => (
              <div key={idx} className="w-24 h-24 relative">
                <img
                  src={URL.createObjectURL(file)}
                  alt={`preview-${idx}`}
                  className="w-full h-full object-cover rounded-lg"
                />
                {/* 삭제 버튼 */}
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

        {/* 별점 */}
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

        {/* 리뷰 작성란 */}
        <textarea
          className="w-full h-32 p-2 resize-none focus:outline-none focus:ring-1 focus:ring-[#925C4C]"
          placeholder="리뷰를 작성해 주세요."
          value={content}
          onChange={handleContentChange}
        />

        {/* 등록 버튼 */}
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
