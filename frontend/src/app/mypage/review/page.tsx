'use client';

import { useState } from "react";
import { useRouter } from "next/navigation";
import reviewsData from "@/../public/mocks/data/reviews.json";

type Review = {
  userId: number;
  reviewId: number;
  productId: number;
  productTitle: string;
  productThumbnailUrl: string;
  rating: number;
  content: string;
  reviewImageUrls?: string[];
  createdDate: string;
  purchasedDate?: string;
};

export default function MyReviewsPage() {
  const router = useRouter();

  // 목데이터를 상태로 관리하여 삭제 시 UI에서 제거 가능
  const [reviews, setReviews] = useState<Review[]>(
    (reviewsData as Review[]).filter((r) => r.userId === 1)
  );

  // 날짜별로 그룹핑
  const grouped = reviews.reduce((acc: Record<string, Review[]>, review) => {
    if (!acc[review.createdDate]) acc[review.createdDate] = [];
    acc[review.createdDate].push(review);
    return acc;
  }, {});

  const sortedDates = Object.keys(grouped).sort((a, b) => b.localeCompare(a));

  const [openIds, setOpenIds] = useState<number[]>([]);
  const [currentImageIndex, setCurrentImageIndex] = useState<Record<number, number>>({});

  const toggleDetail = (id: number) => {
    setOpenIds((prev) =>
      prev.includes(id) ? prev.filter((v) => v !== id) : [...prev, id]
    );
  };

  const prevImage = (reviewId: number, maxIndex: number) => {
    setCurrentImageIndex((prev) => ({
      ...prev,
      [reviewId]:
        prev[reviewId] === undefined
          ? 0
          : (prev[reviewId] - 1 + maxIndex + 1) % (maxIndex + 1),
    }));
  };

  const nextImage = (reviewId: number, maxIndex: number) => {
    setCurrentImageIndex((prev) => ({
      ...prev,
      [reviewId]:
        prev[reviewId] === undefined
          ? 0
          : (prev[reviewId] + 1) % (maxIndex + 1),
    }));
  };

  // 삭제 버튼 클릭
  const handleDelete = (reviewId: number) => {
    alert("리뷰가 삭제되었습니다.");
    // 실제 API 호출 시에는 여기에 fetch/axios.delete 등 사용
    // 목데이터 상태에서 제거
    setReviews((prev) => prev.filter((r) => r.reviewId !== reviewId));
    // 현재 페이지 새로고침 효과
    router.refresh();
  };

  return (
    <div className="font-sans p-4">
      <h2 className="text-[#925C4C] text-2xl font-bold mb-3">작성한 리뷰</h2>

      {sortedDates.map((date) => (
        <div key={date}>
          {/* 날짜 */}
          <div className="text-sm text-gray-500 mt-4 mb-1">{date}</div>

          {/* 리뷰 카드 */}
          {grouped[date].map((review) => {
            const isOpen = openIds.includes(review.reviewId);
            const images = review.reviewImageUrls || [];
            const idx = currentImageIndex[review.reviewId] || 0;

            return (
              <div
                key={review.reviewId}
                className="border border-gray-200 rounded-lg mb-2 p-3 bg-white shadow-sm relative"
              >
                {/* 상단 */}
                <div className="flex justify-between items-start">
                  <div className="flex items-center">
                    <img
                      src={review.productThumbnailUrl}
                      alt="상품 썸네일"
                      className="w-16 h-16 rounded-lg object-cover mr-3"
                    />
                    <div>
                      <div className="font-medium">{review.productTitle}</div>
                      <div className="text-sm text-gray-500">
                        구매일: {review.purchasedDate || review.createdDate}
                      </div>
                    </div>
                  </div>

                  {/* 별점 + 삭제 버튼 + 펼쳐보기 버튼 */}
                  <div className="flex flex-col mt-2 items-end gap-1">
                    <div className="flex items-center gap-4 mb-1">
                      {/* 별점 */}
                      <div className="flex items-center gap-1">
                        <span className="text-yellow-500 text-sm mt-1">★</span>
                        <span className="text-black text-sm">
                          {review.rating.toFixed(1)}
                        </span>
                      </div>

                      {/* 삭제 버튼 */}
                      <button
                        onClick={() => handleDelete(review.reviewId)}
                        className="bg-[#925C4C] text-white rounded-md px-3 py-1 cursor-pointer w-fit"
                      >
                        삭제
                      </button>
                    </div>

                    {/* 펼쳐보기 */}
                    <div
                      onClick={() => toggleDetail(review.reviewId)}
                      className="flex items-center gap-1 text-sm text-gray-400 cursor-pointer select-none w-fit"
                    >
                      <span>{isOpen ? "접기" : "펼쳐보기"}</span>
                      <span
                        className={`inline-block text-base transform transition-transform ${
                          isOpen ? "rotate-180 -mt-0.5" : "rotate-0"
                        }`}
                      >
                        ⌃
                      </span>
                    </div>
                  </div>
                </div>

                {/* 상세보기 */}
                {isOpen && (
                  <div className="mt-3">
                    {/* '리뷰사진' 텍스트 */}
                    <div className="text-xs text-gray-400 mb-1 ml-0">리뷰사진</div>

                    {/* 사진과 리뷰 내용 */}
                    <div className="flex items-start gap-4">
                      <div className="relative w-24 h-24 rounded-lg overflow-hidden bg-gray-100 flex items-center justify-center">
                        {images.length > 0 ? (
                          <>
                            <img
                              src={images[idx]}
                              alt={`review-${review.reviewId}-${idx}`}
                              className="w-full h-full object-cover"
                            />
                            {/* 좌우 화살표 */}
                            {images.length > 1 && (
                              <>
                                <button
                                  onClick={() => prevImage(review.reviewId, images.length - 1)}
                                  className="absolute left-0 top-1/2 -translate-y-1/2 bg-black bg-opacity-40 text-white px-1 py-0.5 rounded-r hover:bg-black"
                                >
                                  ‹
                                </button>
                                <button
                                  onClick={() => nextImage(review.reviewId, images.length - 1)}
                                  className="absolute right-0 top-1/2 -translate-y-1/2 bg-black bg-opacity-40 text-white px-1 py-0.5 rounded-l hover:bg-black"
                                >
                                  ›
                                </button>
                              </>
                            )}
                          </>
                        ) : (
                          <span className="text-sm text-gray-500">사진 없음</span>
                        )}
                      </div>

                      <div className="flex-1 text-sm leading-6 whitespace-pre-line">
                        {review.content}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
