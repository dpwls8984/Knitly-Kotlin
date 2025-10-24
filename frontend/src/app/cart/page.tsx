'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { useCartStore, CartStoreItem } from '@/lib/store/cartStore';

export default function CartPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuthStore();

  // Zustand 스토어에서 상태와 함수 가져오기
  const cartItems = useCartStore((state) => state.items);
  const removeFromCart = useCartStore((state) => state.removeFromCart);
  const clearCart = useCartStore((state) => state.clearCart);

  // 상태 관리
  const [selectedItems, setSelectedItems] = useState<Set<number>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [isProcessingOrder, setIsProcessingOrder] = useState(false);

  // 페이지 로드/cartItems 변경 시 모든 아이템 선택
  useEffect(() => {
    const allProductIds = new Set(cartItems.map((item) => item.productId));
    setSelectedItems(allProductIds);
  }, [cartItems]);

  // 계산 로직
  const selectedCartItems = cartItems.filter((item) =>
    selectedItems.has(item.productId)
  );
  const totalAmount = selectedCartItems.reduce(
    (sum, item) => sum + item.price,
    0
  );

  // 이벤트 핸들러
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
      const allProductIds = new Set(cartItems.map((item) => item.productId));
      setSelectedItems(allProductIds);
    }
  };

  const handleDeleteItem = (productIdToDelete: number) => {
    removeFromCart(productIdToDelete);
  };

  // ✨ 결제하기 핸들러 - 토스페이먼츠 결제 페이지로 이동
  const handleCheckout = () => {
    if (selectedItems.size === 0) {
      alert('결제할 상품을 선택해주세요.');
      return;
    }

    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      return;
    }

    // 선택된 아이템 정보를 결제 페이지로 전달
    const selectedItemsData = selectedCartItems.map(item => ({
      productId: item.productId,
      title: item.title,
      price: item.price,
      imageUrl: item.imageUrl,
    }));

    // 쿼리 파라미터로 인코딩하여 전달
    const itemsParam = encodeURIComponent(JSON.stringify(selectedItemsData));
    router.push(`/checkout?items=${itemsParam}`);
  };

  // 렌더링 로직
  if (isAuthLoading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#925C4C]"></div>
      </div>
    );
  }

  if (!isAuthenticated && !isAuthLoading) {
    return (
      <div className="max-w-4xl mx-auto p-4 md:p-8">
        <h1 className="text-3xl font-bold mb-6">장바구니</h1>
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg">
          <p>오류: 장바구니를 보려면 로그인이 필요합니다.</p>
        </div>
      </div>
    );
  }

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
            </div>

            {cartItems.map((item) => (
              <div
                key={item.productId}
                className="flex items-center p-4 bg-white shadow rounded-lg border border-gray-200"
              >
                <input
                  type="checkbox"
                  className="form-checkbox h-5 w-5 text-[#925C4C] rounded border-gray-300 focus:ring-[#925C4C] mr-4"
                  checked={selectedItems.has(item.productId)}
                  onChange={() => handleCheckboxChange(item.productId)}
                />
                {item.imageUrl && (
                  <img
                    src={item.imageUrl}
                    alt={item.title}
                    className="w-16 h-16 object-cover rounded mr-4"
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = 
                        `https://placehold.co/100x100/CCCCCC/FFFFFF?text=No+Image`;
                    }}
                  />
                )}
                <div className="flex-1">
                  <p className="font-medium text-gray-800">{item.title}</p>
                  <p className="text-lg font-semibold text-gray-900">
                    {item.price.toLocaleString()}원
                  </p>
                </div>
                <button
                  onClick={() => handleDeleteItem(item.productId)}
                  className="text-gray-400 hover:text-red-600 transition-colors ml-4 p-2"
                  aria-label={`${item.title} 삭제`}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    strokeWidth={1.5}
                    stroke="currentColor"
                    className="w-5 h-5"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M6 18 18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>
            ))}
          </div>

          {/* 주문 요약 (오른쪽) */}
          <div className="lg:col-span-1">
            <div className="sticky top-8 bg-white shadow-lg rounded-lg border border-gray-200 p-6">
              <h2 className="text-xl font-semibold mb-4 border-b pb-3">주문 요약</h2>
              <div className="space-y-2 mb-4 max-h-60 overflow-y-auto pr-2">
                {selectedCartItems.length === 0 ? (
                  <p className="text-gray-500 text-sm">선택된 상품이 없습니다.</p>
                ) : (
                  selectedCartItems.map((item) => (
                    <div key={item.productId} className="flex justify-between text-sm">
                      <span className="text-gray-700 truncate mr-2">{item.title}</span>
                      <span className="font-medium text-gray-900 whitespace-nowrap">
                        {item.price.toLocaleString()}원
                      </span>
                    </div>
                  ))
                )}
              </div>
              <div className="border-t pt-4">
                <div className="flex justify-between items-baseline mb-4">
                  <span className="text-lg font-semibold text-gray-800">총 주문 금액</span>
                  <span className="text-2xl font-bold text-[#925C4C]">
                    {totalAmount.toLocaleString()}원
                  </span>
                </div>
                <button
                  onClick={handleCheckout}
                  disabled={selectedItems.size === 0 || isProcessingOrder}
                  className="w-full bg-[#925C4C] hover:bg-[#7a4c3e] text-white font-bold py-3 px-6 rounded-lg transition-colors text-lg disabled:bg-gray-400 disabled:cursor-not-allowed"
                >
                  {isProcessingOrder ? '처리 중...' : '결제하기'}
                </button>
                {error && !isProcessingOrder && (
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