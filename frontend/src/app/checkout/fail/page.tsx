'use client';

import { useSearchParams } from 'next/navigation';
import Link from 'next/link';

export default function PaymentFailPage() {
  const searchParams = useSearchParams();
  
  const errorCode = searchParams.get('code');
  const errorMessage = searchParams.get('message');
  const orderId = searchParams.get('orderId');

  // 에러 메시지 매핑
  const getErrorDisplayMessage = (code: string | null) => {
    if (!code) return errorMessage || '알 수 없는 오류가 발생했습니다.';

    const errorMessages: { [key: string]: string } = {
      'PAY_PROCESS_CANCELED': '사용자가 결제를 취소했습니다.',
      'PAY_PROCESS_ABORTED': '결제 진행 중 오류가 발생했습니다.',
      'REJECT_CARD_COMPANY': '카드사에서 승인을 거부했습니다.',
      'EXCEED_MAX_CARD_INSTALLMENT_PLAN': '설정 가능한 최대 할부 개월수를 초과했습니다.',
      'INVALID_CARD_EXPIRATION': '유효하지 않은 카드 유효기간입니다.',
      'NOT_SUPPORTED_CARD': '지원하지 않는 카드입니다.',
      'INCORRECT_BASIC_AUTH_FORMAT': '잘못된 인증 정보입니다.',
      'NOT_FOUND_PAYMENT': '존재하지 않는 결제 정보입니다.',
      'NOT_FOUND_PAYMENT_SESSION': '결제 시간이 만료되었습니다.',
      'FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING': '결제가 완료되지 않았습니다.',
    };

    return errorMessages[code] || errorMessage || '결제 처리 중 오류가 발생했습니다.';
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white shadow-lg rounded-lg p-8">
        {/* 실패 아이콘 */}
        <div className="text-center mb-6">
          <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg
              className="w-10 h-10 text-red-600"
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
          <h1 className="text-3xl font-bold text-gray-800 mb-2">결제 실패</h1>
          <p className="text-gray-600 mb-4">
            {getErrorDisplayMessage(errorCode)}
          </p>
        </div>

        {/* 에러 상세 정보 */}
        {(errorCode || orderId) && (
          <div className="bg-gray-50 rounded-lg p-4 mb-6 space-y-2 text-sm">
            {orderId && (
              <div className="flex justify-between">
                <span className="text-gray-600">주문번호</span>
                <span className="font-medium text-gray-800">{orderId}</span>
              </div>
            )}
            {errorCode && (
              <div className="flex justify-between">
                <span className="text-gray-600">에러 코드</span>
                <span className="font-medium text-gray-800">{errorCode}</span>
              </div>
            )}
          </div>
        )}

        {/* 안내 메시지 */}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-yellow-800">
            💡 결제가 진행되지 않았으며, 금액이 청구되지 않았습니다.
          </p>
        </div>

        {/* 버튼 영역 */}
        <div className="space-y-3">
          <Link
            href="/cart"
            className="block w-full bg-[#925C4C] text-white py-3 rounded-lg hover:bg-[#7a4c3e] transition-colors font-medium text-center"
          >
            장바구니로 돌아가기
          </Link>
          <Link
            href="/"
            className="block w-full bg-gray-200 text-gray-700 py-3 rounded-lg hover:bg-gray-300 transition-colors font-medium text-center"
          >
            홈으로 가기
          </Link>
        </div>

        {/* 고객센터 안내 */}
        <div className="mt-6 pt-6 border-t text-center">
          <p className="text-sm text-gray-600">
            문제가 계속 발생하시나요?
          </p>
          <p className="text-sm text-gray-800 font-medium mt-1">
            고객센터: 1234-5678
          </p>
        </div>
      </div>
    </div>
  );
}