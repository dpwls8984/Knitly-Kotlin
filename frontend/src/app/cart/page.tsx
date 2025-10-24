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
      fetch('http://localhost:8080/cart', { // 백엔드 주소 추가
        headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` }
      })
      .then(res => {
        if (!res.ok) throw new Error('장바구니 정보를 불러오는데 실패했습니다.');
        return res.json();
      })
      .then(data => {
        setCartItems(data.cartItems);
        const allItemIds = new Set(data.cartItems.map(item => item.id));
        setSelectedItems(allItemIds);
      })
      .catch(err => setError(err.message))
      .finally(() => setIsLoading(false));
      */

      // [Mock 데이터 로직]
      const timer = setTimeout(() => {
        setCartItems(mockCartItems);
        const allMockItemIds = new Set(mockCartItems.map((item) => item.id));
        setSelectedItems(allMockItemIds);
        setIsLoading(false);
      }, 500);

      return () => clearTimeout(timer);

    } else if (!isAuthLoading && !isAuthenticated) {
      setError('로그인이 필요합니다.');
      setIsLoading(false);
      // router.push('/login'); // 필요시 로그인 페이지 이동
    }
  }, [isAuthenticated, isAuthLoading, user, router]);

  // --- 5. 계산 로직 ---
  const selectedCartItems = cartItems.filter((item) =>
    selectedItems.has(item.id)
  );
  const totalAmount = selectedCartItems.reduce(
    (sum, item) => sum + item.price,
    0
  );

  // --- 6. 이벤트 핸들러 ---
  const handleCheckboxChange = (itemId: string) => {
    setSelectedItems((prevSelected) => {
      const newSelected = new Set(prevSelected);
      if (newSelected.has(itemId)) {
        newSelected.delete(itemId);
      } else {
        newSelected.add(itemId);
      }
      return newSelected;
    });
  };

  const handleSelectAll = () => {
    if (selectedItems.size === cartItems.length) {
      setSelectedItems(new Set());
    } else {
      const allItemIds = new Set(cartItems.map((item) => item.id));
      setSelectedItems(allItemIds);
    }
  };

  const handleDeleteItem = (itemIdToDelete: string) => {
    setCartItems((prevItems) =>
      prevItems.filter((item) => item.id !== itemIdToDelete)
    );
    setSelectedItems((prevSelected) => {
      const newSelected = new Set(prevSelected);
      newSelected.delete(itemIdToDelete);
      return newSelected;
    });

    // (가정) DELETE /cart/{cartItemId} API 호출
    /*
    fetch(`http://localhost:8080/cart/${itemIdToDelete}`, { // 백엔드 주소 추가
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` }
    }).catch(err => {
      console.error('삭제 실패:', err);
      alert('상품 삭제에 실패했습니다.');
      // TODO: 삭제 실패 시 UI 롤백
    });
    */
    console.log(`삭제 API 호출: DELETE /cart/${itemIdToDelete}`);
  };

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
      const accessToken = localStorage.getItem('accessToken');
      const response = await fetch('http://localhost:8080/orders', { // 백엔드 주소 추가
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`
        },
        body: JSON.stringify({ cartItemIds: itemIdsToOrder }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '주문 생성에 실패했습니다.');
      }
      // const result = await response.json(); // 성공 응답

      alert('주문이 완료되었습니다!'); // Modal 사용 권장
      router.push('/mypage/order');

    } catch (err: any) {
      console.error(err);
      setError(err.message);
    } finally {
        setIsProcessingOrder(false);
    }
    */

    // [Mock 주문 처리 로직]
    console.log('주문 생성 API 호출 (POST /orders):', { cartItemIds: itemIdsToOrder });
    setTimeout(() => {
      alert('주문이 완료되었습니다! (Mock)'); // Modal 사용 권장
      setIsProcessingOrder(false);
      router.push('/mypage/order');
    }, 1500);
  };

  // --- 7. 렌더링 로직 ---
  if (isAuthLoading) {
    return ( <div className="flex justify-center items-center min-h-[60vh]"><div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div></div> );
  }
  if (isLoading) {
    return ( <div className="flex justify-center items-center min-h-[60vh]"><p>장바구니를 불러오는 중...</p><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-[#925C4C] ml-3"></div></div> );
  }
  if (error) {
    return ( <div className="max-w-4xl mx-auto p-4 md:p-8"><h1 className="text-3xl font-bold mb-6">장바구니</h1><div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg"><p>오류: {error}</p></div></div> );
  }

  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8">
      <h1 className="text-3xl font-bold mb-8">장바구니</h1>
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
              <div key={item.id} className="flex items-center p-4 bg-white shadow rounded-lg border border-gray-200">
                <input type="checkbox" className="form-checkbox h-5 w-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C] mr-4" checked={selectedItems.has(item.id)} onChange={() => handleCheckboxChange(item.id)} />
                {item.imageUrl && ( <img src={item.imageUrl} alt={item.name} className="w-16 h-16 object-cover rounded mr-4" onError={(e) => { (e.target as HTMLImageElement).src = `https://placehold.co/100x100/CCCCCC/FFFFFF?text=No+Image`; }} /> )}
                <div className="flex-1"> <p className="font-medium text-gray-800">{item.name}</p> <p className="text-lg font-semibold text-gray-900"> {item.price.toLocaleString()}원 </p> </div>
                <button onClick={() => handleDeleteItem(item.id)} className="text-gray-400 hover:text-red-600 transition-colors ml-4 p-2" aria-label={`${item.name} 삭제`}> <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5"> <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" /> </svg> </button>
              </div>
            ))}
          </div>

          {/* 주문 요약 (오른쪽) */}
          <div className="lg:col-span-1">
            <div className="sticky top-8 bg-white shadow-lg rounded-lg border border-gray-200 p-6">
              <h2 className="text-xl font-semibold mb-4 border-b pb-3">주문 요약</h2>
              <div className="space-y-2 mb-4 max-h-60 overflow-y-auto pr-2">
                {selectedCartItems.length === 0 ? ( <p className="text-gray-500 text-sm">선택된 상품이 없습니다.</p> ) : ( selectedCartItems.map((item) => ( <div key={item.id} className="flex justify-between text-sm"> <span className="text-gray-700 truncate mr-2">{item.name}</span> <span className="font-medium text-gray-900 whitespace-nowrap"> {item.price.toLocaleString()}원 </span> </div> )) )}
              </div>
              <div className="border-t pt-4">
                <div className="flex justify-between items-baseline mb-4"> <span className="text-lg font-semibold text-gray-800">총 주문 금액</span> <span className="text-2xl font-bold text-[#925C4C]"> {totalAmount.toLocaleString()}원 </span> </div>
                <button onClick={handleCheckout} disabled={selectedItems.size === 0 || isProcessingOrder} className="w-full bg-[#925C4C] hover:bg-[#7a4c3e] text-white font-bold py-3 px-6 rounded-lg transition-colors text-lg disabled:bg-gray-400 disabled:cursor-not-allowed"> {isProcessingOrder ? '주문 처리 중...' : '결제하기'} </button>
                {error && !isLoading && ( <p className="text-red-600 text-sm mt-3 text-center">{error}</p> )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}