'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export default function PaymentSuccessPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isProcessing, setIsProcessing] = useState(true);
  const [paymentInfo, setPaymentInfo] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const confirmPayment = async () => {
      // URL 쿼리 파라미터에서 결제 정보 추출
      const paymentKey = searchParams.get('paymentKey');
      const orderId = searchParams.get('orderId');
      const amount = searchParams.get('amount');

      if (!paymentKey || !orderId || !amount) {
        setError('결제 정보가 올바르지 않습니다.');
        setIsProcessing(false);
        return;
      }

      try {
        // 백엔드 결제 승인 API 호출
        const response = await fetch(`${API_URL}/payments/confirm`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
          },
          body: JSON.stringify({
            paymentKey,
            orderId,
            amount: parseInt(amount),
          }),
        });

        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.message || '결제 승인에 실패했습니다.');
        }

        const data = await response.json();
        setPaymentInfo(data);
        setIsProcessing(false);

        // 장바구니에서 결제된 아이템 제거 (선택사항)
        // localStorage에서 장바구니 관리하는 경우
        // 실제 구현에서는 백엔드에서 처리하는 것이 좋습니다.

      } catch (error: any) {
        console.error('결제 승인 실패:', error);
        setError(error.message || '결제 승인 중 오류가 발생했습니다.');
        setIsProcessing(false);
      }
    };

    confirmPayment();
  }, [searchParams]);

  // 로딩 중
  if (isProcessing) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-[#925C4C] mx-auto mb-4"></div>
          <p className="text-lg text-gray-600">결제를 처리하고 있습니다...</p>
          <p className="text-sm text-gray-500 mt-2">잠시만 기다려주세요.</p>
        </div>
      </div>
    );
  }

  // 에러 발생
  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-white shadow-lg rounded-lg p-8 text-center">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg
              className="w-8 h-8 text-red-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">결제 실패</h1>
          <p className="text-gray-600 mb-6">{error}</p>
          <div className="flex gap-3">
            <Link
              href="/cart"
              className="flex-1 bg-gray-200 text-gray-700 py-3 rounded-lg hover:bg-gray-300 transition-colors font-medium"
            >
              장바구니로
            </Link>
            <Link
              href="/"
              className="flex-1 bg-[#925C4C] text-white py-3 rounded-lg hover:bg-[#7a4c3e] transition-colors font-medium"
            >
              홈으로
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // 결제 성공
  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="max-w-2xl w-full bg-white shadow-lg rounded-lg p-8">
        {/* 성공 아이콘 */}
        <div className="text-center mb-6">
          <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg
              className="w-10 h-10 text-green-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
          <h1 className="text-3xl font-bold text-gray-800 mb-2">결제가 완료되었습니다!</h1>
          <p className="text-gray-600">주문이 정상적으로 처리되었습니다.</p>
        </div>

        {/* 결제 정보 */}
        {paymentInfo && (
          <div className="bg-gray-50 rounded-lg p-6 mb-6 space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">주문번호</span>
              <span className="font-medium">{paymentInfo.orderId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">결제금액</span>
              <span className="font-bold text-lg text-[#925C4C]">
                {paymentInfo.totalAmount?.toLocaleString()}원
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">결제수단</span>
              <span className="font-medium">
                {paymentInfo.method === 'CARD' ? '카드' : 
                 paymentInfo.method === 'TRANSFER' ? '계좌이체' :
                 paymentInfo.method === 'VIRTUAL_ACCOUNT' ? '가상계좌' :
                 paymentInfo.method}
              </span>
            </div>
            {paymentInfo.approvedAt && (
              <div className="flex justify-between">
                <span className="text-gray-600">승인시간</span>
                <span className="font-medium">
                  {new Date(paymentInfo.approvedAt).toLocaleString('ko-KR')}
                </span>
              </div>
            )}
          </div>
        )}

        {/* 안내 메시지 */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-blue-800">
            💡 주문 내역은 마이페이지에서 확인하실 수 있습니다.
          </p>
        </div>

        {/* 버튼 영역 */}
        <div className="flex gap-3">
          <Link
            href="/mypage/order"
            className="flex-1 bg-gray-200 text-gray-700 py-3 rounded-lg hover:bg-gray-300 transition-colors font-medium text-center"
          >
            주문 내역 보기
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
  );
}