'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { loadTossPayments } from '@tosspayments/tosspayments-sdk';
import { useAuthStore } from '@/lib/store/authStore';

// 환경 변수
const clientKey = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY;
const API_URL = process.env.NEXT_PUBLIC_API_URL;

// 장바구니에서 전달받는 아이템 타입
interface CheckoutItem {
  productId: number;
  title: string;
  price: number;
  imageUrl?: string;
}

export default function CheckoutPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, isAuthenticated } = useAuthStore();
  
  const [ready, setReady] = useState(false);
  const [items, setItems] = useState<CheckoutItem[]>([]);
  const [tossOrderId, setTossOrderId] = useState<string>(''); // tossOrderId로 변경
  const paymentWidgetRef = useRef<any>(null);

  // 1. 인증 확인
  useEffect(() => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      router.push('/');
    }
  }, [isAuthenticated, router]);

  // 2. 장바구니 아이템 로드
  useEffect(() => {
    const itemsParam = searchParams.get('items');
    if (itemsParam) {
      try {
        const parsedItems = JSON.parse(decodeURIComponent(itemsParam));
        setItems(parsedItems);
      } catch (error) {
        console.error('아이템 파싱 실패:', error);
        alert('잘못된 결제 정보입니다.');
        router.push('/cart');
      }
    } else {
      alert('결제할 상품이 없습니다.');
      router.push('/cart');
    }
  }, [searchParams, router]);

  // 3. 주문 생성 및 토스페이먼츠 위젯 초기화
  useEffect(() => {
    if (!user || items.length === 0) return;

    const initializePayment = async () => {
      try {
        // 3-1. 주문 생성 (백엔드에서 tossOrderId 자동 생성)
        const accessToken = localStorage.getItem('accessToken');
        const productIds = items.map(item => item.productId);
        
        const orderResponse = await fetch(`${API_URL}/orders`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`,
          },
          body: JSON.stringify({ productIds }),
        });

        if (!orderResponse.ok) {
          throw new Error('주문 생성에 실패했습니다.');
        }

        const orderData = await orderResponse.json();
        
        // ✅ 백엔드에서 생성된 tossOrderId를 사용
        const generatedTossOrderId = orderData.tossOrderId;
        if (!generatedTossOrderId) {
          throw new Error('주문 번호(tossOrderId)를 받지 못했습니다.');
        }
        
        setTossOrderId(generatedTossOrderId);
        console.log('생성된 tossOrderId:', generatedTossOrderId);

        // 3-2. 토스페이먼츠 SDK 로드 및 위젯 초기화
        const tossPayments = await loadTossPayments(clientKey);
        
        const totalAmount = items.reduce((sum, item) => sum + item.price, 0);
        const orderName = items.length > 1 
          ? `${items[0].title} 외 ${items.length - 1}건`
          : items[0].title;

        // 결제위젯 렌더링
        const paymentWidget = tossPayments.widgets({
          customerKey: `customer_${user.userId}`,
        });

        await paymentWidget.setAmount({
          currency: 'KRW',
          value: totalAmount,
        });

        await Promise.all([
          // 결제 UI 렌더링
          paymentWidget.renderPaymentMethods({
            selector: '#payment-method',
            variantKey: 'DEFAULT',
          }),
          // 이용약관 UI 렌더링
          paymentWidget.renderAgreement({
            selector: '#agreement',
            variantKey: 'AGREEMENT',
          }),
        ]);

        paymentWidgetRef.current = paymentWidget;
        setReady(true);

      } catch (error) {
        console.error('결제 초기화 실패:', error);
        alert('결제 준비 중 오류가 발생했습니다.');
        router.push('/cart');
      }
    };

    initializePayment();
  }, [user, items, router]);

  // 4. 결제 요청
  const handlePayment = async () => {
    if (!paymentWidgetRef.current || !tossOrderId) {
      alert('결제 준비가 완료되지 않았습니다.');
      return;
    }

    try {
      const totalAmount = items.reduce((sum, item) => sum + item.price, 0);
      const orderName = items.length > 1 
        ? `${items[0].title} 외 ${items.length - 1}건`
        : items[0].title;

      // ✅ 백엔드에서 받은 tossOrderId를 사용
      console.log('결제 요청 - tossOrderId:', tossOrderId);
      
      await paymentWidgetRef.current.requestPayment({
        orderId: tossOrderId, // ✅ 백엔드에서 생성된 올바른 형식의 tossOrderId
        orderName: orderName,
        successUrl: `${window.location.origin}/checkout/success`,
        failUrl: `${window.location.origin}/checkout/fail`,
        customerEmail: user?.email,
        customerName: user?.username || user?.name,
      });
    } catch (error) {
      console.error('결제 요청 실패:', error);
      alert('결제 요청 중 오류가 발생했습니다.');
    }
  };

  // 로딩 중
  if (!isAuthenticated || items.length === 0) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">결제하기</h1>
      
      {/* 주문 상품 목록 */}
      <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
        <h2 className="text-lg font-semibold mb-4">주문 상품</h2>
        {items.map((item, index) => (
          <div key={index} className="flex justify-between items-center py-2">
            <span>{item.title}</span>
            <span className="font-medium">{item.price.toLocaleString()}원</span>
          </div>
        ))}
        <div className="border-t mt-4 pt-4">
          <div className="flex justify-between items-center font-bold text-lg">
            <span>총 결제금액</span>
            <span className="text-[#925C4C]">
              {items.reduce((sum, item) => sum + item.price, 0).toLocaleString()}원
            </span>
          </div>
        </div>
      </div>

      {/* 토스페이먼츠 결제 UI */}
      <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
        <div id="payment-method" />
        <div id="agreement" className="mt-4" />
      </div>

      {/* 결제하기 버튼 */}
      <button
        onClick={handlePayment}
        disabled={!ready}
        className={`w-full py-4 rounded-lg font-semibold text-lg transition-colors ${
          ready
            ? 'bg-[#925C4C] text-white hover:bg-[#7a4c3e] cursor-pointer'
            : 'bg-gray-300 text-gray-500 cursor-not-allowed'
        }`}
      >
        {ready ? '결제하기' : '결제 준비 중...'}
      </button>
    </div>
  );
}
