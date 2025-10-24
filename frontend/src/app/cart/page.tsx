'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
// (가정) 로그인 상태 관리를 위해 authStore 사용
import { useAuthStore } from '@/lib/store/authStore';

// ▼▼▼ [수정] Zustand 스토어 및 타입 임포트 ▼▼▼
import { useCartStore, CartStoreItem } from '@/lib/store/cartStore';
// ▲▲▲ [수정] Zustand 스토어 및 타입 임포트 ▲▲▲

// --- CartItem 타입은 이제 cartStore에서 가져오므로 제거 ---
// interface CartItem { ... } // 제거

export default function CartPage() {
  const router = useRouter();
  // 로그인 상태는 여전히 확인 (선택 사항: 비로그인 시 접근 제어 등)
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuthStore();

  // ▼▼▼ [수정] Zustand 스토어에서 상태와 함수 직접 가져오기 ▼▼▼
  const cartItems = useCartStore((state) => state.items);
  const removeFromCart = useCartStore((state) => state.removeFromCart);
  const clearCart = useCartStore((state) => state.clearCart);
  // ▲▲▲ [수정] Zustand 스토어에서 상태와 함수 직접 가져오기 ▲▲▲

  // --- 3. 상태 관리 (useState) ---
  // selectedItems는 페이지 내에서 관리 (productId 저장)
  const [selectedItems, setSelectedItems] = useState<Set<number>>(new Set());
  // isLoading, error 상태 제거 (백엔드 cart API 호출 없으므로)
  // const [isLoading, setIsLoading] = useState(true); // 제거
  const [error, setError] = useState<string | null>(null); // 주문 시 에러 표시는 유지
  const [isProcessingOrder, setIsProcessingOrder] = useState(false);

  // --- 4. 데이터 페칭 제거 ---
  // useEffect(() => { ... fetch('/cart')... }, []); // 제거

  // --- [수정] 페이지 로드/cartItems 변경 시 모든 아이템 선택 ---
  useEffect(() => {
    // Zustand 스토어에서 가져온 cartItems 기준으로 selectedItems 초기화
    const allProductIds = new Set(cartItems.map((item) => item.productId));
    setSelectedItems(allProductIds);
  }, [cartItems]); // cartItems 배열 참조가 바뀔 때마다 실행
  // ---

  // --- 5. 계산 로직 ---
  // (수정) selectedItems (Set<number>)에 productId가 있는지 확인
  const selectedCartItems = cartItems.filter((item) =>
    selectedItems.has(item.productId)
  );
  const totalAmount = selectedCartItems.reduce(
    (sum, item) => sum + item.price,
    0
  );

  // --- 6. 이벤트 핸들러 ---

  // (수정) productId 타입 number
  const handleCheckboxChange = (productId: number) => {
    setSelectedItems((prevSelected) => {
      const newSelected = new Set(prevSelected);
      if (newSelected.has(productId)) {
        newSelected.delete(productId);
      } else {
        newSelected.add(productId);
      }
      return newSelected;
    });
  };

  const handleSelectAll = () => {
    if (selectedItems.size === cartItems.length) {
      setSelectedItems(new Set());
    } else {
      const allProductIds = new Set(cartItems.map((item) => item.productId)); // productId 사용
      setSelectedItems(allProductIds);
    }
  };

  // (수정) handleDeleteItem - Zustand의 removeFromCart 호출
  const handleDeleteItem = (productIdToDelete: number) => {
    // Zustand 스토어 상태 변경 함수 호출
    removeFromCart(productIdToDelete);

    // selectedItems에서도 제거 (선택 사항)
    // setSelectedItems((prevSelected) => {
    //   const newSelected = new Set(prevSelected);
    //   newSelected.delete(productIdToDelete);
    //   return newSelected;
    // });
    // => useEffect [cartItems] 의존성 때문에 자동으로 처리될 것임

    // 백엔드 API 호출 제거
    // fetch(...).then(...).catch(...); // 제거
  };

  // (수정) handleCheckout - 성공 시 clearCart 호출
  const handleCheckout = async () => {
    if (selectedItems.size === 0) {
      alert('결제할 상품을 선택해주세요.');
      return;
    }

    // (추가) 비로그인 시 처리
    if (!isAuthenticated) {
        alert('로그인이 필요합니다.');
        // router.push('/login'); // 필요시 로그인 페이지 이동
        return;
    }

    setIsProcessingOrder(true);
    setError(null);

    // 선택된 productId 목록 추출 (selectedItems Set 사용)
    const productIdsToOrder = Array.from(selectedItems);

    // 실제 API 연동 (POST /orders)
    try {
      const accessToken = localStorage.getItem('accessToken');
      const response = await fetch('http://localhost:8080/orders', { // 백엔드 주소 확인
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`
        },
        body: JSON.stringify({ productIds: productIdsToOrder }),
      });

      if (!response.ok) {
        let errorMsg = '주문 생성에 실패했습니다.';
        try { const errorData = await response.json(); errorMsg = errorData.message || errorMsg; } catch (e) {}
        throw new Error(errorMsg);
      }
      // const result = await response.json();

      alert('주문이 완료되었습니다!');
      // ▼▼▼ [수정] 주문 성공 시 Zustand 장바구니 비우기 ▼▼▼
      clearCart();
      // ▲▲▲ [수정] 주문 성공 시 Zustand 장바구니 비우기 ▲▲▲
      router.push('/mypage/order');

    } catch (err: any) {
      console.error(err);
      setError(err.message);
    } finally {
        setIsProcessingOrder(false);
    }
  };


  // --- 7. 렌더링 로직 ---
  // (수정) isLoading 제거, isAuthLoading은 선택적으로 사용
  if (isAuthLoading) { // 인증 로딩 중 표시 (선택 사항)
      return ( <div className="flex justify-center items-center min-h-[60vh]"><div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div></div> );
  }
  // if (isLoading) { ... } // 제거

  // (수정) 비로그인 시 메시지 또는 리다이렉트 처리 강화
  if (!isAuthenticated && !isAuthLoading) {
      // setError('로그인이 필요합니다.'); // useEffect에서 이미 설정됨
      return ( <div className="max-w-4xl mx-auto p-4 md:p-8"><h1 className="text-3xl font-bold mb-6">장바구니</h1><div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg"><p>오류: 장바구니를 보려면 로그인이 필요합니다.</p>{/* 로그인 버튼 추가 가능 */}</div></div> );
  }
  // 주문 생성 실패 시 에러 메시지
  if (error && !isProcessingOrder) {
      // return ( ... 에러 표시 UI ... ); // 이전 코드와 유사하게 표시
  }


  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8">
      <h1 className="text-3xl font-bold mb-8">장바구니</h1>
      {/* (수정) cartItems.length === 0 조건 */}
      {cartItems.length === 0 ? (
        <div className="bg-white shadow-lg rounded-lg p-10 text-center text-gray-500"> 장바구니가 비어 있습니다. </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* 장바구니 목록 (왼쪽) */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg border">
              <label className="flex items-center space-x-3 cursor-pointer">
                <input type="checkbox" className="form-checkbox h-5 w-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C]" checked={selectedItems.size === cartItems.length && cartItems.length > 0} onChange={handleSelectAll} />
                <span className="font-medium"> 전체 선택 ({selectedItems.size}/{cartItems.length}) </span>
              </label>
            </div>
            {cartItems.map((item) => (
              // (수정) key, checked, onChange 등에서 item.productId / item.title 사용
              // (수정) handleDeleteItem에 productId 전달
              <div key={item.productId} className="flex items-center p-4 bg-white shadow rounded-lg border border-gray-200">
                <input type="checkbox" className="form-checkbox h-5 w-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C] mr-4" checked={selectedItems.has(item.productId)} onChange={() => handleCheckboxChange(item.productId)} />
                {item.imageUrl && ( <img src={item.imageUrl} alt={item.title} className="w-16 h-16 object-cover rounded mr-4" onError={(e) => { (e.target as HTMLImageElement).src = `https://placehold.co/100x100/CCCCCC/FFFFFF?text=No+Image`; }} /> )}
                <div className="flex-1"> <p className="font-medium text-gray-800">{item.title}</p> <p className="text-lg font-semibold text-gray-900"> {item.price.toLocaleString()}원 </p> </div>
                <button onClick={() => handleDeleteItem(item.productId)} className="text-gray-400 hover:text-red-600 transition-colors ml-4 p-2" aria-label={`${item.title} 삭제`}> <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"> <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" /> </svg> </button>
              </div>
            ))}
          </div>

          {/* 주문 요약 (오른쪽) */}
          <div className="lg:col-span-1">
            <div className="sticky top-8 bg-white shadow-lg rounded-lg border border-gray-200 p-6">
              <h2 className="text-xl font-semibold mb-4 border-b pb-3">주문 요약</h2>
              <div className="space-y-2 mb-4 max-h-60 overflow-y-auto pr-2">
                {selectedCartItems.length === 0 ? ( <p className="text-gray-500 text-sm">선택된 상품이 없습니다.</p> ) : (
                  // (수정) key에 item.productId, text에 item.title 사용
                  selectedCartItems.map((item) => ( <div key={item.productId} className="flex justify-between text-sm"> <span className="text-gray-700 truncate mr-2">{item.title}</span> <span className="font-medium text-gray-900 whitespace-nowrap"> {item.price.toLocaleString()}원 </span> </div> ))
                )}
              </div>
              <div className="border-t pt-4">
                <div className="flex justify-between items-baseline mb-4"> <span className="text-lg font-semibold text-gray-800">총 주문 금액</span> <span className="text-2xl font-bold text-[#925C4C]"> {totalAmount.toLocaleString()}원 </span> </div>
                <button onClick={handleCheckout} disabled={selectedItems.size === 0 || isProcessingOrder} className="w-full bg-[#925C4C] hover:bg-[#7a4c3e] text-white font-bold py-3 px-6 rounded-lg transition-colors text-lg disabled:bg-gray-400 disabled:cursor-not-allowed"> {isProcessingOrder ? '주문 처리 중...' : '결제하기'} </button>
                {/* 주문 처리 에러 메시지 */}
                {error && !isProcessingOrder && ( <p className="text-red-600 text-sm mt-3 text-center">{error}</p> )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}