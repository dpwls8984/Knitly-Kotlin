'use client';

import { useAuthStore } from '@/lib/store/authStore';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import Link from 'next/link';

interface OrderItem {
  orderItemId: number;
  productId: number;
  productTitle: string;
  quantity: number;
  orderPrice: number;
  isReviewed: boolean;
}

interface Order {
  orderId: number;
  orderedAt: string;
  totalPrice: number;
  items: OrderItem[];
}

export default function OrderHistoryPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuthStore();
  
  const [orders, setOrders] = useState<Order[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isFetching, setIsFetching] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLastPage, setIsLastPage] = useState(false);

  // 비로그인 상태면 리다이렉트
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      console.log('로그인이 필요하여 메인 페이지로 이동합니다.');
      router.push('/');
    }
  }, [isAuthenticated, isLoading, router]);

  // 주문 내역 페칭
  useEffect(() => {
    if (isAuthenticated && user) {
      const fetchOrders = async () => {
        setIsFetching(true);
        setError(null);

        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
          setError("로그인이 필요합니다.");
          setIsFetching(false);
          return;
        }

        try {
          const response = await fetch(`http://localhost:8080/mypage/orders?page=${currentPage}&size=3`, {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${accessToken}`,
              'Content-Type': 'application/json',
            },
          });

          if (!response.ok) {
            if (response.status === 401) {
              throw new Error('인증에 실패했습니다. 다시 로그인해주세요.');
            }
            throw new Error('주문 내역을 불러오는데 실패했습니다.');
          }

          const data = await response.json();

          if (currentPage === 0) {
            setOrders(data.content);
          } else {
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
  }, [isAuthenticated, user, currentPage]);

  // 날짜 형식 변환
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
  };
  
  // 더보기 버튼
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
              {/* 주문 헤더 */}
              <div className="flex justify-between items-center mb-4 border-b border-gray-200 pb-3">
                <div>
                  <h2 className="text-lg font-semibold text-gray-800">
                    주문번호: {order.orderId}
                  </h2>
                  <p className="text-sm text-gray-500 mt-1">
                    {formatDate(order.orderedAt)}
                  </p>
                </div>
                
                {/* ⭐ 결제 내역 보기 버튼 추가 */}
                <Link
                  href={`/mypage/order/${order.orderId}/payment`}
                  className="bg-gray-100 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-200 transition-colors text-sm font-medium border border-gray-300"
                >
                  결제 내역 보기
                </Link>
              </div>

              {/* 주문 상품 목록 */}
              <div className="space-y-4">
                {order.items.map((item) => (
                  <div
                    key={item.orderItemId}
                    className="flex justify-between items-center"
                  >
                    <div>
                      <p className="font-medium text-gray-700">
                        {item.productTitle}
                      </p>
                      <p className="text-lg font-semibold text-gray-900">
                        {item.orderPrice.toLocaleString()}원
                      </p>
                    </div>

                    {item.isReviewed ? (
                      <button
                        className="bg-gray-300 text-gray-500 px-4 py-2 rounded-lg cursor-not-allowed text-sm font-medium"
                        disabled
                      >
                        리뷰 완료
                      </button>
                    ) : (
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

              {/* 총 결제 금액 */}
              <div className="mt-4 pt-4 border-t border-gray-200">
                <div className="flex justify-between items-center">
                  <span className="text-gray-600 font-medium">총 결제 금액</span>
                  <span className="text-xl font-bold text-[#925C4C]">
                    {order.totalPrice.toLocaleString()}원
                  </span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* 더보기 버튼 */}
      {!isLastPage && (
        <div className="mt-6 text-center">
          <button
            onClick={handleLoadMore}
            disabled={isFetching}
            className="bg-[#925C4C] text-white px-8 py-3 rounded-lg hover:bg-[#7a4c3e] transition-colors font-medium disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            {isFetching ? '로딩 중...' : '더보기'}
          </button>
        </div>
      )}
    </div>
  );
}