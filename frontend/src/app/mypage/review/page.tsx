'use client';

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getMyReviews, deleteReview, ReviewListItem } from "@/lib/api/review.api";

export default function MyReviewsPage() {
  const router = useRouter();

  const [reviews, setReviews] = useState<ReviewListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [openIds, setOpenIds] = useState<number[]>([]);
  const [currentImageIndex, setCurrentImageIndex] = useState<Record<number, number>>({});

  const fetchReviews = async (pageNumber: number) => {
    try {
      setLoading(true);
      const data = await getMyReviews(pageNumber, 10);
      setReviews(data.content);
      setTotalPages(data.totalPages);
      setPage(pageNumber);
    } catch (error) {
      console.error("리뷰 불러오기 실패:", error);
      alert("리뷰를 불러오는데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviews(0);
  }, []);

  const handleDelete = async (reviewId: number) => {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    try {
      await deleteReview(reviewId);
      alert("리뷰가 삭제되었습니다.");
      fetchReviews(page);
    } catch (error) {
      console.error("리뷰 삭제 실패:", error);
      alert("리뷰 삭제에 실패했습니다.");
    }
  };

  const toggleDetail = (id: number) => {
    setOpenIds(prev =>
      prev.includes(id) ? prev.filter(v => v !== id) : [...prev, id]
    );
  };

  const prevImage = (reviewId: number, maxIndex: number) => {
    setCurrentImageIndex(prev => ({
      ...prev,
      [reviewId]: prev[reviewId] === undefined ? 0 : (prev[reviewId] - 1 + maxIndex + 1) % (maxIndex + 1),
    }));
  };

  const nextImage = (reviewId: number, maxIndex: number) => {
    setCurrentImageIndex(prev => ({
      ...prev,
      [reviewId]: prev[reviewId] === undefined ? 0 : (prev[reviewId] + 1) % (maxIndex + 1),
    }));
  };

  // ✅ 날짜별로 리뷰 묶기
  const groupedReviews = reviews.reduce<Record<string, ReviewListItem[]>>((acc, review) => {
    const date = review.createdDate; // LocalDate 형태 (e.g. '2025-10-23')
    if (!acc[date]) acc[date] = [];
    acc[date].push(review);
    return acc;
  }, {});

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  return (
    <div className="font-sans p-4">
      <h2 className="text-[#925C4C] text-2xl font-bold mb-3">작성한 리뷰</h2>

      {reviews.length === 0 && (
        <div className="text-center text-gray-500 py-20">작성한 리뷰가 없습니다.</div>
      )}

      {/* ✅ 리뷰일별 그룹 렌더링 */}
      {Object.keys(groupedReviews)
  .sort((a, b) => (a < b ? 1 : -1)) // 최신 날짜가 위로
  .map(date => (
    <div key={date} className="mb-6">
      {/* ✅ 리뷰 작성일 헤더 (디자인 수정됨) */}
      <div className="text-gray-400 text-sm mb-2">{date}</div>

      {/* 해당 날짜 리뷰들 */}
      {groupedReviews[date].map(review => {
        const isOpen = openIds.includes(review.reviewId);
        const images = review.reviewImageUrls || [];
        const idx = currentImageIndex[review.reviewId] || 0;

        return (
          <div
            key={review.reviewId}
            className="border border-gray-200 rounded-lg mb-2 p-3 bg-white shadow-sm relative"
          >
            <div className="flex justify-between items-start">
              <div className="flex items-center">
                <img
                  src={review.productThumbnailUrl}
                  alt="상품 썸네일"
                  className="w-16 h-16 rounded-lg object-cover mr-3"
                />
                <div>
                  <div className="font-medium">{review.productTitle}</div>
                </div>
              </div>

              <div className="flex flex-col mt-2 items-end gap-1">
                <div className="flex items-center gap-4 mb-1">
                  <div className="flex items-center gap-1">
                    <span className="text-yellow-500 text-sm mt-1">★</span>
                    <span className="text-black text-sm">{review.rating.toFixed(1)}</span>
                  </div>

                  <button
                    onClick={() => handleDelete(review.reviewId)}
                    className="bg-[#925C4C] text-white rounded-md px-3 py-1 cursor-pointer w-fit"
                  >
                    삭제
                  </button>
                </div>

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

            {/* 상세 영역 */}
            {isOpen && (
              <div className="mt-3">
                <div className="text-xs text-gray-400 mb-1 ml-0">리뷰사진</div>
                <div className="flex items-start gap-4">
                  <div className="relative w-24 h-24 rounded-lg overflow-hidden bg-gray-100 flex items-center justify-center">
                    {images.length > 0 ? (
                      <>
                        <img
                          src={images[idx]}
                          alt={`review-${review.reviewId}-${idx}`}
                          className="w-full h-full object-cover"
                        />
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

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button
            onClick={() => fetchReviews(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-4 py-2 rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            이전
          </button>

          {Array.from({ length: totalPages }, (_, i) => (
            <button
              key={i}
              onClick={() => fetchReviews(i)}
              className={`px-4 py-2 rounded ${
                i === page
                  ? "bg-[#925C4C] text-white"
                  : "bg-gray-200 hover:bg-gray-300"
              }`}
            >
              {i + 1}
            </button>
          ))}

          <button
            onClick={() => fetchReviews(Math.min(totalPages - 1, page + 1))}
            disabled={page === totalPages - 1}
            className="px-4 py-2 rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
