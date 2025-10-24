//주문내역 페이지

'use client';

import { useAuthStore } from '@/lib/store/authStore';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import Link from 'next/link'; // '리뷰하기' 버튼을 위해 Link 컴포넌트를 사용합니다.

// --- 백엔드 주니어님께 ---
// 1. 주문 내역 데이터 타입 (가정)
// 아래는 와이어프레임을 기반으로 제가 가정한 데이터 구조입니다.
// 실제 백엔드에서 받아오는 데이터 구조(Type)로 이 부분을 교체하셔야 합니다.

/** 개별 상품 아이템 타입 */
interface OrderItem {
  id: string; // 각 주문 항목(상품)의 고유 ID (리뷰 작성 시 필요할 것으로 예상)
  productId: string; // 상품 자체의 ID
  productName: string; // 상품명
  price: number; // 상품 가격 (와이어프레임에서 개별 금액을 요청하셨습니다)
  // productImage?: string; // 상품 이미지가 있다면 추가
}

/**
 * 주문 1건에 대한 타입 (주문 번호별로 나뉨)
 * 각 주문(order)은 여러 개의 주문 항목(items)을 포함합니다.
 */
interface Order {
  id: string; // 주문 고유 ID
  orderNumber: string; // 주문 번호
  orderDate: string; // 구매일 (예: "2023.10.25")
  items: OrderItem[]; // 해당 주문에 포함된 상품 목록
}

// 2. Mock Data (임시 데이터)
// API 연동 전 UI를 확인하기 위한 임시 데이터입니다.
// 실제로는 이 데이터를 API로 받아와서 상태(state)에 저장하여 사용해야 합니다.
const mockOrders: Order[] = [
  {
    id: 'order123',
    orderNumber: '20231025-0001',
    orderDate: '2023.10.25',
    items: [
      {
        id: 'itemA1',
        productId: 'prod_abc',
        productName: '따뜻한 겨울 스웨터 도안',
        price: 15000,
      },
      {
        id: 'itemA2',
        productId: 'prod_def',
        productName: '아가일 패턴 양말 도안',
        price: 7000,
      },
    ],
  },
  {
    id: 'order124',
    orderNumber: '20231023-0007',
    orderDate: '2023.10.23',
    items: [
      {
        id: 'itemB1',
        productId: 'prod_ghi',
        productName: '초보자용 목도리 도안',
        price: 5000,
      },
    ],
  },
];
// --- 여기까지 임시 데이터 및 타입 정의입니다. ---

export default function OrderHistoryPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuthStore();

  // 비로그인 상태면 로그인 페이지로 리다이렉트
  // 기존 mypage/page.tsx의 로직을 그대로 사용합니다.
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      // alert('로그인이 필요합니다.'); // alert는 피하는 것이 좋습니다.
      console.log('로그인이 필요하여 메인 페이지로 이동합니다.');
      router.push('/'); // 또는 로그인 페이지 '/login'
    }
  }, [isAuthenticated, isLoading, router]);

  // 로딩 중일 때
  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  // 비로그인 상태 (리다이렉트 전) 또는 유저 정보가 없을 때
  if (!user) {
    return null; // 리다이렉트가 처리할 것입니다.
  }

  // --- 백엔드 주니어님께 ---
  // 3. 실제 데이터 페칭(Fetching) 로직
  // 현재는 `mockOrders`를 사용하고 있지만,
  // 실제로는 여기에 `useEffect`와 `fetch` (또는 SWR, React-Query 등)를 사용하여
  // '/api/mypage/orders' 같은 백엔드 API로부터 데이터를 받아와야 합니다.
  //
  // 예시:
  // const [orders, setOrders] = useState<Order[]>([]);
  // useEffect(() => {
  //   if (user) {
  //     fetch('/api/v1/orders/my') // 실제 API 엔드포인트
  //       .then(res => res.json())
  //       .then(data => setOrders(data.orders))
  //       .catch(err => console.error('주문 내역을 불러오는데 실패했습니다:', err));
  //   }
  // }, [user]);
  //
  // ... 그리고 아래 map 함수에서는 `orders.map(...)`을 사용합니다.
  // ---

  return (
    <div>
      {/* 페이지 제목 */}
      <h1 className="text-3xl font-bold mb-6">주문 내역(아직 mock 데이터입니다)</h1>

      {/* 주문 목록 */}
      <div className="space-y-6">
        {/*
          현재 mockOrders를 map 돌리고 있습니다.
          실제 API 연동 시에는 API로부터 받아온 데이터(state)를 map 돌려야 합니다.
        */}
        {mockOrders.length === 0 ? (
          <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500">
            주문 내역이 없습니다.
          </div>
        ) : (
          mockOrders.map((order) => (
            // 각 주문 카드 (기존 마이페이지 스타일과 통일)
            <div
              key={order.id}
              className="bg-white shadow-lg rounded-lg p-6"
            >
              {/* 주문 번호 및 날짜 헤더 */}
              <div className="flex justify-between items-center mb-4 border-b border-gray-200 pb-3">
                <h2 className="text-lg font-semibold text-gray-800">
                  주문번호: {order.orderNumber}
                </h2>
                <p className="text-sm text-gray-500">{order.orderDate}</p>
              </div>

              {/* 주문에 포함된 상품 목록 */}
              <div className="space-y-4">
                {order.items.map((item) => (
                  <div
                    key={item.id}
                    className="flex justify-between items-center"
                  >
                    {/* 상품 정보 (상품명, 가격) */}
                    <div>
                      <p className="font-medium text-gray-700">
                        {item.productName}
                      </p>
                      <p className="text-lg font-semibold text-gray-900">
                        {item.price.toLocaleString()}원
                      </p>
                    </div>

                    {/* --- 백엔드 주니어님께 ---
                        4. [리뷰하기] 버튼 경로
                        요청사항에 "리뷰 쓰는 페이지 경로가 src/app/mypage/review/page.tsx"라고 하셨습니다.
                        하지만 layout.tsx를 보면 이 경로는 "리뷰 목록" 페이지(/mypage/review)입니다.

                        와이어프레임상 '리뷰하기' 버튼은 *개별 상품*에 대해 작성하는 기능으로 보입니다.
                        따라서 '리뷰 목록' 페이지로 이동하는 것은 맞지 않아 보입니다.

                        가장 일반적인 방식은 '/mypage/review/write' 같은 별도 페이지로 이동하거나,
                        '/mypage/review' 페이지에서 쿼리 파라미터(예: ?write=true&itemId=...)를 받아
                        리뷰 작성 폼을 보여주는 것입니다.

                        일단 저는 리뷰 작성 페이지가 `/mypage/review/write` 라는 별도 경로에
                        존재한다고 *가정*하고, 어떤 상품에 대한 리뷰인지 알 수 있도록
                        query parameter로 `orderItemId`를 넘겨주도록 구현했습니다.

                        이 경로는 프론트엔드 라우팅(파일 구조)과 백엔드 기획에 따라
                        반드시 수정이 필요합니다!
                    --- */}
                    <Link
                      href={`/mypage/review/write?orderItemId=${item.id}&orderDate=${order.orderDate}`}
                      className="bg-[#925C4C] text-white px-4 py-2 rounded-lg hover:bg-[#7a4c3e] transition-colors text-sm font-medium"
                    >
                      리뷰하기
                    </Link>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
