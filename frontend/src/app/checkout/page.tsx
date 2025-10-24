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
  const [orderId, setOrderId] = useState<string>('');
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
        // 3-1. 주문 생성
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
        const createdOrderId = String(orderData.orderId || orderData.id);
        setOrderId(createdOrderId);

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
    if (!paymentWidgetRef.current || !orderId) {
      alert('결제 준비가 완료되지 않았습니다.');
      return;
    }

    try {
      const totalAmount = items.reduce((sum, item) => sum + item.price, 0);
      const orderName = items.length > 1 
        ? `${items[0].title} 외 ${items.length - 1}건`
        : items[0].title;

      await paymentWidgetRef.current.requestPayment({
        orderId: orderId,
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

  // 총 결제 금액 계산
  const totalAmount = items.reduce((sum, item) => sum + item.price, 0);

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">주문/결제</h1>

      {/* 주문 상품 정보 */}
      <div className="bg-white shadow-lg rounded-lg p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">주문 상품</h2>
        <div className="space-y-4">
          {items.map((item, index) => (
            <div key={`${item.productId}-${index}`} className="flex items-center justify-between border-b pb-4">
              <div className="flex items-center gap-4">
                {item.imageUrl && (
                  <img
                    src={item.imageUrl}
                    alt={item.title}
                    className="w-20 h-20 object-cover rounded"
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = 
                        `https://placehold.co/100x100/CCCCCC/FFFFFF?text=No+Image`;
                    }}
                  />
                )}
                <div>
                  <p className="font-medium">{item.title}</p>
                </div>
              </div>
              <p className="text-lg font-semibold">{item.price.toLocaleString()}원</p>
            </div>
          ))}
        </div>

        {/* 총 결제 금액 */}
        <div className="mt-6 pt-4 border-t">
          <div className="flex justify-between items-center">
            <span className="text-xl font-bold">총 결제 금액</span>
            <span className="text-2xl font-bold text-[#925C4C]">
              {totalAmount.toLocaleString()}원
            </span>
          </div>
        </div>
      </div>

      {/* 결제 위젯 영역 */}
      <div className="bg-white shadow-lg rounded-lg p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">결제 수단</h2>
        <div id="payment-method" className="min-h-[300px]"></div>
        <div id="agreement" className="mt-4"></div>
      </div>

      {/* 결제하기 버튼 */}
      <button
        onClick={handlePayment}
        disabled={!ready}
        className={`w-full py-4 rounded-lg text-white font-bold text-lg transition-colors ${
          ready
            ? 'bg-[#925C4C] hover:bg-[#7a4c3e]'
            : 'bg-gray-400 cursor-not-allowed'
        }`}
      >
        {ready ? '결제하기' : '결제 준비 중...'}
      </button>
    </div>
  );
}
