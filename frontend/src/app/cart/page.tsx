'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
// (가정) 로그인 상태 관리를 위해 authStore 사용
import { useAuthStore } from '@/lib/store/authStore';

// --- 백엔드 주니어님께 ---
// 1. (가정) 장바구니 아이템 타입
// GET /cart API 응답의 개별 아이템 타입입니다.
interface CartItem {
  id: string; // 장바구니 항목의 고유 ID (주문 생성, 삭제 시 필요)
  productId: string; // 상품 ID
  name: string; // 상품명
  price: number; // 상품 가격
  imageUrl?: string; // (선택) 상품 이미지 URL
}

// 2. (추가) Mock 데이터
// GET /cart API 연동 전 UI 확인용 임시 데이터입니다.
const mockCartItems: CartItem[] = [
  {
    id: 'cartItem-1',
    productId: 'prod-abc-123',
    name: '따뜻한 겨울 스웨터 도안',
    price: 15000,
    imageUrl: 'https://placehold.co/100x100/925C4C/white?text=Sweater',
  },
  {
    id: 'cartItem-2',
    productId: 'prod-def-456',
    name: '아가일 패턴 양말 도안',
    price: 7000,
    imageUrl: 'https://placehold.co/100x100/EAD9D5/white?text=Socks',
  },
  {
    id: 'cartItem-3',
    productId: 'prod-ghi-789',
    name: '초보자용 목도리 도안',
    price: 5000,
    imageUrl: 'https://placehold.co/100x100/D5E0EA/white?text=Scarf',
  },
];
// ---

// 페이지 이름을 CartPage 등으로 변경하는 것이 더 명확합니다.
export default function CartPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuthStore();

  // --- 3. 상태 관리 ---
  const [cartItems, setCartItems] = useState<CartItem[]>([]); // 장바구니 목록
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set()); // 선택된 아이템 ID Set
  const [isLoading, setIsLoading] = useState(true); // 장바구니 로딩 상태
  const [error, setError] = useState<string | null>(null); // 에러 상태
  const [isProcessingOrder, setIsProcessingOrder] = useState(false); // 주문 처리 중 상태

  // --- 4. 데이터 페칭 (Mock 사용) ---
  useEffect(() => {
    // 로그인 상태 확인 후 장바구니 데이터 로드
    if (!isAuthLoading && isAuthenticated && user) {
      setIsLoading(true);
      setError(null);

      // (가정) GET /cart API 호출
      // 실제 API 연동 시 이 주석을 해제하고 Mock 로직을 제거하세요.
      /*
      fetch('/cart', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` } // 예시: JWT 토큰 포함
      })
      .then(res => {
        if (!res.ok) throw new Error('장바구니 정보를 불러오는데 실패했습니다.');
        return res.json();
      })
      .then(data => {
        setCartItems(data.cartItems);
        // 처음에는 모든 상품을 선택 상태로 초기화 (선택 사항)
        const allItemIds = new Set(data.cartItems.map(item => item.id));
        setSelectedItems(allItemIds);
      })
      .catch(err => setError(err.message))
      .finally(() => setIsLoading(false));
      */

      // [Mock 데이터 로직]
      const timer = setTimeout(() => {
        setCartItems(mockCartItems);
        // Mock 데이터로 모든 아이템 선택 상태 초기화
        const allMockItemIds = new Set(mockCartItems.map((item) => item.id));
        setSelectedItems(allMockItemIds);
        setIsLoading(false);
      }, 500);

      return () => clearTimeout(timer);

    } else if (!isAuthLoading && !isAuthenticated) {
      // 비로그인 시 로그인 페이지로 리다이렉트 (선택 사항)
      // router.push('/login');
      setError('로그인이 필요합니다.');
      setIsLoading(false);
    }
  }, [isAuthenticated, isAuthLoading, user, router]);

  // --- 5. 계산 로직 ---
  // 선택된 상품 목록 계산
  const selectedCartItems = cartItems.filter((item) =>
    selectedItems.has(item.id)
  );

  // 총 주문 금액 계산
  const totalAmount = selectedCartItems.reduce(
    (sum, item) => sum + item.price,
    0
  );

  // --- 6. 이벤트 핸들러 ---

  // 체크박스 핸들러
  const handleCheckboxChange = (itemId: string) => {
    setSelectedItems((prevSelected) => {
      const newSelected = new Set(prevSelected);
      if (newSelected.has(itemId)) {
        newSelected.delete(itemId); // 이미 선택 -> 해제
      } else {
        newSelected.add(itemId); // 미선택 -> 선택
      }
      return newSelected;
    });
  };

  // 전체 선택/해제 핸들러 (선택 사항)
  const handleSelectAll = () => {
    if (selectedItems.size === cartItems.length) {
      setSelectedItems(new Set()); // 모두 선택 -> 모두 해제
    } else {
      const allItemIds = new Set(cartItems.map((item) => item.id));
      setSelectedItems(allItemIds); // 일부 또는 미선택 -> 모두 선택
    }
  };

  // 상품 삭제 핸들러
  const handleDeleteItem = (itemIdToDelete: string) => {
    // 낙관적 업데이트: UI 먼저 변경
    setCartItems((prevItems) =>
      prevItems.filter((item) => item.id !== itemIdToDelete)
    );
    setSelectedItems((prevSelected) => {
      const newSelected = new Set(prevSelected);
      newSelected.delete(itemIdToDelete);
      return newSelected;
    });

    // (가정) DELETE /cart/{cartItemId} API 호출
    // 실제 API 연동 시 에러 처리 및 롤백 로직 추가 필요
    /*
    fetch(`/cart/${itemIdToDelete}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` }
    }).catch(err => {
      console.error('삭제 실패:', err);
      // TODO: 삭제 실패 시 UI 롤백 (예: 원래 목록 다시 불러오기)
      alert('상품 삭제에 실패했습니다.');
    });
    */
    console.log(`삭제 API 호출: DELETE /cart/${itemIdToDelete}`);
  };

  // 결제하기 핸들러
  const handleCheckout = async () => {
    if (selectedItems.size === 0) {
      alert('결제할 상품을 선택해주세요.');
      return;
    }

    setIsProcessingOrder(true);
    setError(null);

    const itemIdsToOrder = Array.from(selectedItems);

    // [실제 API 연동] POST /orders 사용
    /*
    try {
      const response = await fetch('/orders', { // API 경로 수정됨
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}` // 예시: 토큰 포함
        },
        body: JSON.stringify({ cartItemIds: itemIdsToOrder }), // 백엔드가 받을 데이터 형식 확인 필요
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '주문 생성에 실패했습니다.');
      }

      const result = await response.json(); // 성공 응답 처리 (예: orderId)

      // [중요] alert 대신 Modal 사용 권장
      alert('주문이 완료되었습니다!');
      router.push('/mypage/order'); // 주문 내역 페이지로 이동

    } catch (err: any) {
      console.error(err);
      setError(err.message);
    } finally {
        setIsProcessingOrder(false); // 로딩 종료 (성공/실패 모두)
    }
    */

    // [Mock 주문 처리 로직]
    console.log('주문 생성 API 호출 (POST /orders):', { cartItemIds: itemIdsToOrder });
    setTimeout(() => {
      // [중요] alert 대신 Modal 사용 권장
      alert('주문이 완료되었습니다! (Mock)');
      setIsProcessingOrder(false);
      router.push('/mypage/order'); // 주문 내역 페이지로 이동
    }, 1500); // 1.5초 후 완료 처리
  };


  // --- 7. 렌더링 로직 ---

  // 인증 로딩
  if (isAuthLoading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  // 장바구니 로딩
  if (isLoading) {
'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

interface CartItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  image?: string;
}

export default function CartPage() {
  const router = useRouter();
  const [cartItems, setCartItems] = useState<CartItem[]>([
    {
      id: '1',
      name: '도안 상품 1',
      price: 15000,
      quantity: 1,
      image: '/logo.png'
    },
    {
      id: '2',
      name: '도안 상품 2',
      price: 25000,
      quantity: 2,
      image: '/logo.png'
    }
  ]);

  const totalAmount = cartItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);

  const handleQuantityChange = (id: string, newQuantity: number) => {
    if (newQuantity <= 0) return;

    setCartItems(prev =>
      prev.map(item =>
        item.id === id ? { ...item, quantity: newQuantity } : item
      )
    );
  };

  const handleRemoveItem = (id: string) => {
    setCartItems(prev => prev.filter(item => item.id !== id));
  };

  const handleOrder = () => {
    // 장바구니 데이터를 localStorage에 저장하고 주문 페이지로 이동
    localStorage.setItem('cartItems', JSON.stringify(cartItems));
    router.push('/order');
  };

  if (cartItems.length === 0) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <h1 className="text-2xl font-bold mb-6">장바구니</h1>
        <div className="text-center py-12">
          <div className="text-gray-400 text-6xl mb-4">🛒</div>
          <h2 className="text-xl font-semibold text-gray-600 mb-2">장바구니가 비어있습니다</h2>
          <p className="text-gray-500 mb-6">원하는 상품을 장바구니에 담아보세요.</p>
          <Link
            href="/product"
            className="inline-block bg-[#925C4C] text-white px-6 py-3 rounded-md hover:bg-[#7a4a3a] font-semibold"
          >
            쇼핑 계속하기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">장바구니</h1>

      <div className="space-y-4">
        {cartItems.map((item) => (
          <div key={item.id} className="flex items-center space-x-4 p-4 border rounded-lg">
            <img
              src={item.image || '/logo.png'}
              alt={item.name}
              className="w-20 h-20 object-cover rounded"
            />
            <div className="flex-1">
              <h3 className="font-medium text-lg">{item.name}</h3>
              <p className="text-gray-600">개당 {item.price.toLocaleString()}원</p>
            </div>

            {/* 수량 조절 */}
            <div className="flex items-center space-x-2">
              <button
                onClick={() => handleQuantityChange(item.id, item.quantity - 1)}
                className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100"
              >
                -
              </button>
              <span className="w-8 text-center">{item.quantity}</span>
              <button
                onClick={() => handleQuantityChange(item.id, item.quantity + 1)}
                className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100"
              >
                +
              </button>
            </div>

            <div className="text-right">
              <p className="font-semibold text-lg">{(item.price * item.quantity).toLocaleString()}원</p>
            </div>

            <button
              onClick={() => handleRemoveItem(item.id)}
              className="text-red-500 hover:text-red-700 p-2"
            >
              🗑️
            </button>
          </div>
        ))}
      </div>

      {/* 총 금액 및 주문 버튼 */}
      <div className="mt-8 p-6 bg-gray-50 rounded-lg">
        <div className="flex justify-between items-center mb-4">
          <span className="text-lg font-semibold">총 결제금액</span>
          <span className="text-2xl font-bold text-[#925C4C]">{totalAmount.toLocaleString()}원</span>
        </div>

        <div className="flex space-x-4">
          <Link
            href="/product"
            className="flex-1 bg-gray-200 text-gray-800 text-center py-3 px-6 rounded-md hover:bg-gray-300 font-semibold"
          >
            쇼핑 계속하기
          </Link>
          <button
            onClick={handleOrder}
            className="flex-1 bg-[#925C4C] text-white py-3 px-6 rounded-md hover:bg-[#7a4a3a] font-semibold"
          >
            주문하기
          </button>
        </div>
      </div>
    </div>
  );
}
      <div className="flex justify-center items-center min-h-[60vh]">
        <p>장바구니를 불러오는 중...</p>
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-[#925C4C] ml-3"></div>
      </div>
    );
  }

  // 에러 발생
  if (error) {
    return (
      <div className="max-w-4xl mx-auto p-4 md:p-8">
        <h1 className="text-3xl font-bold mb-6">장바구니</h1>
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg">
          <p>오류: {error}</p>
        </div>
      </div>
    );
  }

  // --- 메인 UI 렌더링 ---
  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8">
      <h1 className="text-3xl font-bold mb-8">장바구니</h1>

      {cartItems.length === 0 ? (
        <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500">
          장바구니가 비어 있습니다.
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* 장바구니 목록 (왼쪽) */}
          <div className="lg:col-span-2 space-y-4">
            {/* (선택 사항) 전체 선택 체크박스 */}
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg border">
              <label className="flex items-center space-x-3 cursor-pointer">
                <input
                  type="checkbox"
                  className="form-checkbox h-5 w-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C]"
                  checked={selectedItems.size === cartItems.length && cartItems.length > 0}
                  onChange={handleSelectAll}
                />
                <span className="font-medium">
                  전체 선택 ({selectedItems.size}/{cartItems.length})
                </span>
              </label>
              {/* (선택 사항) 선택 삭제 버튼 */}
              {/* <button className="text-sm text-red-600 hover:underline">선택 삭제</button> */}
            </div>

            {/* 개별 상품 */}
            {cartItems.map((item) => (
              <div
                key={item.id}
                className="flex items-center p-4 bg-white shadow rounded-lg border border-gray-200"
              >
                {/* 체크박스 */}
                <input
                  type="checkbox"
                  className="form-checkbox h-5 w-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C] mr-4"
                  checked={selectedItems.has(item.id)}
                  onChange={() => handleCheckboxChange(item.id)}
                />
                {/* 상품 이미지 (선택 사항) */}
                {item.imageUrl && (
                   <img
                    src={item.imageUrl}
                    alt={item.name}
                    className="w-16 h-16 object-cover rounded mr-4"
                    onError={(e) => {
                      // 이미지 로드 실패 시 대체 이미지
                      (e.target as HTMLImageElement).src = `https://placehold.co/100x100/CCCCCC/FFFFFF?text=No+Image`;
                    }}
                   />
                 )}
                {/* 상품 정보 */}
                <div className="flex-1">
                  <p className="font-medium text-gray-800">{item.name}</p>
                  <p className="text-lg font-semibold text-gray-900">
                    {item.price.toLocaleString()}원
                  </p>
                </div>
                {/* 삭제 버튼 */}
                <button
                  onClick={() => handleDeleteItem(item.id)}
                  className="text-gray-400 hover:text-red-600 transition-colors ml-4 p-2"
                  aria-label={`${item.name} 삭제`}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            ))}
          </div>

          {/* 주문 요약 (오른쪽) */}
          <div className="lg:col-span-1">
            <div className="sticky top-8 bg-white shadow-lg rounded-lg border border-gray-200 p-6">
              <h2 className="text-xl font-semibold mb-4 border-b pb-3">주문 요약</h2>

              {/* 선택된 상품 목록 */}
              <div className="space-y-2 mb-4 max-h-60 overflow-y-auto pr-2">
                {selectedCartItems.length === 0 ? (
                  <p className="text-gray-500 text-sm">선택된 상품이 없습니다.</p>
                ) : (
                  selectedCartItems.map((item) => (
                    <div key={item.id} className="flex justify-between text-sm">
                      <span className="text-gray-700 truncate mr-2">{item.name}</span>
                      <span className="font-medium text-gray-900 whitespace-nowrap">
                        {item.price.toLocaleString()}원
                      </span>
                    </div>
                  ))
                )}
              </div>

              {/* 총 주문 금액 */}
              <div className="border-t pt-4">
                <div className="flex justify-between items-baseline mb-4">
                  <span className="text-lg font-semibold text-gray-800">총 주문 금액</span>
                  <span className="text-2xl font-bold text-[#925C4C]">
                    {totalAmount.toLocaleString()}원
                  </span>
                </div>

                {/* 결제하기 버튼 */}
                <button
                  onClick={handleCheckout}
                  disabled={selectedItems.size === 0 || isProcessingOrder}
                  className="w-full bg-[#925C4C] hover:bg-[#7a4c3e] text-white font-bold py-3 px-6 rounded-lg transition-colors text-lg disabled:bg-gray-400 disabled:cursor-not-allowed"
                >
                  {isProcessingOrder ? '주문 처리 중...' : '결제하기'}
                </button>

                {/* 주문 처리 에러 메시지 */}
                {error && !isLoading && ( // 로딩 에러 말고 주문 에러만 표시
                  <p className="text-red-600 text-sm mt-3 text-center">{error}</p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}