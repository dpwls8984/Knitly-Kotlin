'use client';

import { useAuthStore } from '@/lib/store/authStore';
import { useRouter, useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import Link from 'next/link';

interface PaymentDetail {
  orderName: string;
  orderId: string;
  method: 'CARD' | 'EASY_PAY';
  totalAmount: number;
  status: string;
  requestedAt: string;
  approvedAt?: string;
  canceledAt?: string;
  cancelReason?: string;
  buyerName: string;
}

export default function PaymentDetailPage() {
  const router = useRouter();
  const params = useParams();
  const orderId = params.orderId as string;
  const { user, isAuthenticated, isLoading } = useAuthStore();
  
  const [payment, setPayment] = useState<PaymentDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFetching, setIsFetching] = useState(true);

  // 비로그인 상태면 리다이렉트
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/');
    }
  }, [isAuthenticated, isLoading, router]);

  // 결제 내역 조회
  useEffect(() => {
    if (isAuthenticated && user && orderId) {
      const fetchPaymentDetail = async () => {
        setIsFetching(true);
        setError(null);

        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
          setError("로그인이 필요합니다.");
          setIsFetching(false);
          return;
        }

        try {
          const response = await fetch(`http://localhost:8080/mypage/orders/${orderId}/payment`, {
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
            if (response.status === 403) {
              throw new Error('본인의 결제 내역만 조회할 수 있습니다.');
            }
            if (response.status === 404) {
              throw new Error('결제 정보를 찾을 수 없습니다.');
            }
            throw new Error('결제 내역을 불러오는데 실패했습니다.');
          }

          const data = await response.json();
          setPayment(data);

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

      fetchPaymentDetail();
    }
  }, [isAuthenticated, user, orderId, router]);

  // 날짜 포맷팅
  const formatDateTime = (dateString?: string) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  // 결제 수단 한글 변환
  const getPaymentMethodText = (method: string) => {
    const methodMap: { [key: string]: string } = {
      'CARD': '카드',
      'EASY_PAY': '간편결제',
    };
    return methodMap[method] || method;
  };

  // 결제 상태 한글 변환
  const getStatusText = (status: string) => {
    const statusMap: { [key: string]: string } = {
      'READY': '결제 준비',
      'IN_PROGRESS': '결제 진행 중',
      'WAITING_FOR_DEPOSIT': '입금 대기',
      'DONE': '결제 완료',
      'CANCELED': '결제 취소',
      'FAILED': '결제 실패',
    };
    return statusMap[status] || status;
  };

  // 결제 상태별 색상
  const getStatusColor = (status: string) => {
    const colorMap: { [key: string]: string } = {
      'READY': 'text-yellow-600 bg-yellow-50',
      'IN_PROGRESS': 'text-blue-600 bg-blue-50',
      'WAITING_FOR_DEPOSIT': 'text-orange-600 bg-orange-50',
      'DONE': 'text-green-600 bg-green-50',
      'CANCELED': 'text-red-600 bg-red-50',
      'FAILED': 'text-red-600 bg-red-50',
    };
    return colorMap[status] || 'text-gray-600 bg-gray-50';
  };

  if (isLoading || isFetching) {
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
      <div className="max-w-3xl mx-auto px-4 py-8">
        <div className="bg-white shadow-lg rounded-lg p-10 text-center">
          <div className="text-red-500 mb-4">
            <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p className="text-lg text-gray-700 mb-6">{error}</p>
          <Link
            href="/mypage/order"
            className="inline-block bg-[#925C4C] text-white px-6 py-3 rounded-lg hover:bg-[#7a4c3e] transition-colors"
          >
            주문 내역으로 돌아가기
          </Link>
        </div>
      </div>
    );
  }

  if (!payment) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <p className="text-gray-500">결제 정보를 불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      {/* 헤더 */}
      <div className="mb-6">
        <Link
          href="/mypage/order"
          className="text-[#925C4C] hover:underline mb-4 inline-flex items-center"
        >
          <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          주문 내역으로 돌아가기
        </Link>
        <h1 className="text-3xl font-bold text-gray-800 mt-2">결제 내역</h1>
      </div>

      <div className="bg-white shadow-lg rounded-lg overflow-hidden">
        {/* 결제 상태 배너 */}
        <div className={`p-6 ${getStatusColor(payment.status)}`}>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium mb-1">결제 상태</p>
              <p className="text-2xl font-bold">{getStatusText(payment.status)}</p>
            </div>
            {payment.status === 'DONE' && (
              <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            )}
          </div>
        </div>

        {/* 결제 정보 */}
        <div className="p-6 space-y-6">
          {/* 기본 정보 */}
          <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4 pb-2 border-b">기본 정보</h2>
            <div className="space-y-3">
              <div className="flex justify-between py-2">
                <span className="text-gray-600">주문 상품</span>
                <span className="font-medium text-gray-800">{payment.orderName}</span>
              </div>
              <div className="flex justify-between py-2">
                <span className="text-gray-600">주문 번호</span>
                <span className="font-medium text-gray-800">{payment.orderId}</span>
              </div>
              <div className="flex justify-between py-2">
                <span className="text-gray-600">주문자</span>
                <span className="font-medium text-gray-800">{payment.buyerName}</span>
              </div>
            </div>
          </div>

          {/* 결제 금액 */}
          <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4 pb-2 border-b">결제 금액</h2>
            <div className="bg-gray-50 rounded-lg p-4">
              <div className="flex justify-between items-center">
                <span className="text-gray-600 text-lg">총 결제 금액</span>
                <span className="text-3xl font-bold text-[#925C4C]">
                  {payment.totalAmount.toLocaleString()}원
                </span>
              </div>
            </div>
          </div>

          {/* 결제 수단 */}
          <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4 pb-2 border-b">결제 수단</h2>
            <div className="space-y-3">
              <div className="flex justify-between py-2">
                <span className="text-gray-600">결제 방법</span>
                <span className="font-medium text-gray-800">{getPaymentMethodText(payment.method)}</span>
              </div>
            </div>
          </div>

          {/* 결제 일시 */}
          <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4 pb-2 border-b">결제 일시</h2>
            <div className="space-y-3">
              <div className="flex justify-between py-2">
                <span className="text-gray-600">결제 요청</span>
                <span className="font-medium text-gray-800">{formatDateTime(payment.requestedAt)}</span>
              </div>
              {payment.approvedAt && (
                <div className="flex justify-between py-2">
                  <span className="text-gray-600">결제 승인</span>
                  <span className="font-medium text-gray-800">{formatDateTime(payment.approvedAt)}</span>
                </div>
              )}
              {payment.canceledAt && (
                <div className="flex justify-between py-2">
                  <span className="text-gray-600">결제 취소</span>
                  <span className="font-medium text-red-600">{formatDateTime(payment.canceledAt)}</span>
                </div>
              )}
            </div>
          </div>

          {/* 취소 정보 */}
          {payment.status === 'CANCELED' && payment.cancelReason && (
            <div>
              <h2 className="text-lg font-semibold text-gray-800 mb-4 pb-2 border-b">취소 정보</h2>
              <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                <p className="text-gray-600 text-sm mb-1">취소 사유</p>
                <p className="text-gray-800">{payment.cancelReason}</p>
              </div>
            </div>
          )}
        </div>

        {/* 버튼 영역 */}
        <div className="p-6 bg-gray-50 border-t">
          <div className="flex gap-3">
            <Link
              href="/mypage/order"
              className="flex-1 bg-gray-200 text-gray-700 py-3 rounded-lg hover:bg-gray-300 transition-colors font-medium text-center"
            >
              주문 내역으로
            </Link>
            <Link
              href="/"
              className="flex-1 bg-[#925C4C] text-white py-3 rounded-lg hover:bg-[#7a4c3e] transition-colors font-medium text-center"
            >
              쇼핑 계속하기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}