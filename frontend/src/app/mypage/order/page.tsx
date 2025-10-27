//주문내역 페이지

'use client';

import { useAuthStore } from '@/lib/store/authStore';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import Link from 'next/link';

// --- 백엔드 주니어님께 ---
// 1. 실제 페이징 API 응답에 맞춘 최종 타입 정의
// 보내주신 GET /mypage/orders 의 응답을 기반으로 타입을 확정했습니다.

/** 개별 주문 상품 정보 타입 ('OrderLine' DTO 기준) */
interface OrderItem {
  orderItemId: number;
  productId: number;
  productTitle: string;
  quantity: number;
  orderPrice: number;
  isReviewed: boolean;
  // --- 질문 1 (여전히 유효) ---
  // key prop 및 리뷰 기능의 정확성을 위해, 각 주문 상품을 구분할 고유 ID가 필요합니다.
  // 예를 들어 DB의 'order_item_id' 같은 값입니다. 이 값을 API 응답에 추가해주실 수 있을까요?
  // (예: "orderItemId": 101)
  // 우선은 productId와 반복문의 index를 조합하여 임시 key를 사용하겠습니다.
}

/** 주문 1건에 대한 타입 ('OrderCardResponse' DTO 기준) */
interface Order {
  orderId: number;
  orderedAt: string; // "2025-10-24T23:26:38.067534"와 같은 ISO 8601 형식
  totalPrice: number;
  items: OrderItem[];
}

// --- 여기까지 타입을 확정했습니다. ---

export default function OrderHistoryPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuthStore();
  
  // API로부터 받아온 주문 목록을 저장할 state
  const [orders, setOrders] = useState<Order[]>([]);
  const [error, setError] = useState<string | null>(null);
  
  const [isFetching, setIsFetching] = useState(true);

  // 2. 페이징 처리를 위한 상태 추가
  // API가 페이징을 지원하므로, 관련 상태들을 추가했습니다.
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLastPage, setIsLastPage] = useState(false);
  // ---

  // 비로그인 상태면 리다이렉트
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      console.log('로그인이 필요하여 메인 페이지로 이동합니다.');
      router.push('/');
    }
  }, [isAuthenticated, isLoading, router]);

  // 실제 데이터 페칭(Fetching) 로직
  useEffect(() => {
    if (isAuthenticated && user) {
      const fetchOrders = async () => {
        // ▼▼▼ [수정 2] setIsLoading -> setIsFetching 으로 변경 ▼▼▼
        setIsFetching(true);
        setError(null);

        // 1. localStorage에서 Access Token 가져오기
        const accessToken = localStorage.getItem('accessToken');

        // 2. 토큰이 없으면 로그인 페이지로 보내는 등의 처리가 필요합니다.
        if (!accessToken) {
          setError("로그인이 필요합니다.");
          setIsFetching(false);
          // router.push('/login'); // 필요에 따라 로그인 페이지로 리다이렉트
          return;
        }

        try {
          // 3. fetch 요청 시 Authorization 헤더에 토큰을 담아 보냅니다.
          const response = await fetch(`http://localhost:8080/mypage/orders?page=${currentPage}&size=3`, {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${accessToken}`,
              'Content-Type': 'application/json',
            },
            // credentials: 'include' 옵션은 이제 필요 없습니다.
          });

          if (!response.ok) {
            // 401 Unauthorized 와 같은 에러 처리를 위해 응답 상태 코드를 확인합니다.
            if (response.status === 401) {
              throw new Error('인증에 실패했습니다. 다시 로그인해주세요.');
            }
            throw new Error('주문 내역을 불러오는데 실패했습니다.');
          }

          const data = await response.json();

          if (currentPage === 0) {
            // 첫 페이지일 경우, 기존 목록을 완전히 교체합니다.
            setOrders(data.content);
          } else {
            // "더보기"로 불러온 다음 페이지부터는 기존 목록에 덧붙입니다.
            setOrders(prevOrders => [...prevOrders, ...data.content]);
          }
          setTotalPages(data.totalPages);
          setIsLastPage(data.last);

        } catch (err) {
          if (err instanceof Error) {
            setError(err.message);
          } else {
            setError('알 수 없는 오류가 발생했습니다.');
          }
          console.error(err);
        } finally {
          setIsFetching(false);
        }
      };

      fetchOrders();
    }
  }, [isAuthenticated, user, currentPage]); // currentPage가 바뀔 때마다 데이터를 추가로 불러옵니다.

  // 날짜 형식을 'YYYY.MM.DD'로 변환하는 헬퍼 함수
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
  };
  
  // "더보기" 버튼 핸들러
  const handleLoadMore = () => {
    if (!isLastPage) {
      setCurrentPage(prevPage => prevPage + 1);
    }
  };

  if (isLoading || (orders.length === 0 && isFetching)) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  if (!user) {
    return null;
  }
  
  if (error) {
    return (
      <div className="bg-white shadow-lg rounded-lg p-10 text-center text-red-500">
        {error}
      </div>
    );
  }


  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">주문 내역</h1>

      <div className="space-y-6">
        {orders.length === 0 ? (
          <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500">
            주문 내역이 없습니다.
          </div>
        ) : (
          orders.map((order) => (
            <div
              key={order.orderId}
              className="bg-white shadow-lg rounded-lg p-6"
            >
              <div className="flex justify-between items-center mb-4 border-b border-gray-200 pb-3">
                <h2 className="text-lg font-semibold text-gray-800">
                  {/* API의 orderId를 주문번호로 사용합니다. */}
                  주문번호: {order.orderId}
                </h2>
                {/* API의 orderedAt 필드를 사용합니다. */}
                <p className="text-sm text-gray-500">{formatDate(order.orderedAt)}</p>
              </div>

              <div className="space-y-4">
                {order.items.map((item) => (
                  <div
                    // 질문 1에 따라 임시로 key를 생성합니다.
                    key={item.orderItemId}
                    className="flex justify-between items-center"
                  >
                    <div>
                      {/* API 응답에 맞춰 productTitle, orderPrice로 변경합니다. */}
                      <p className="font-medium text-gray-700">
                        {item.productTitle}
                      </p>
                      <p className="text-lg font-semibold text-gray-900">
                        {item.orderPrice.toLocaleString()}원
                      </p>
                    </div>

                    {item.isReviewed ? (
                      // 리뷰가 작성된 경우: 비활성화된 "리뷰 완료" 버튼
                      <button
                        className="bg-gray-300 text-gray-500 px-4 py-2 rounded-lg cursor-not-allowed text-sm font-medium"
                        disabled
                      >
                        리뷰 완료
                      </button>
                    ) : (
                      // 리뷰가 작성되지 않은 경우: 기존 "리뷰하기" 버튼
                      <Link
                        href={`/mypage/review/write?orderItemId=${item.orderItemId}`}
                        className="bg-[#925C4C] text-white px-4 py-2 rounded-lg hover:bg-[#7a4c3e] transition-colors text-sm font-medium"
                      >
                        리뷰하기
                      </Link>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>

      {/* --- 백엔드 주니어님께 ---
          4. 페이징 UI ("더보기" 버튼)
          - API가 페이징을 지원하므로, 가장 간단한 방식인 "더보기" 버튼을 우선 구현했습니다.
          - 마지막 페이지가 아닐 경우에만 이 버튼이 보입니다.
          - 버튼을 누르면 다음 페이지의 데이터를 불러와 기존 목록 아래에 추가합니다.
          - UX 정책에 따라 페이지 번호(Pagination) UI로 변경할 수도 있습니다.
      --- */}
      {!isLastPage && orders.length > 0 && (
        <div className="mt-8 text-center">
          <button
            onClick={handleLoadMore}
            className="bg-gray-200 text-gray-800 px-6 py-3 rounded-lg hover:bg-gray-300 transition-colors font-semibold"
          >
            더보기
          </button>
        </div>
      )}

    </div>
  );
}