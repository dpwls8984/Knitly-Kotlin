import { create } from 'zustand';

// 장바구니 아이템 타입 (quantity 없음)
export interface CartStoreItem {
  productId: number;
  title: string;
  price: number;
  imageUrl?: string;
}

// CartState 타입 (clearCart 추가)
interface CartState {
  items: CartStoreItem[];
  addToCart: (item: CartStoreItem) => void;
  removeFromCart: (productId: number) => void;
  isInCart: (productId: number) => boolean;
  clearCart: () => void; // 장바구니 비우기 함수 추가
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],

  // addToCart 함수 (중복 방지)
  addToCart: (newItem) => set((state) => {
    const existingItem = state.items.find(
      (item) => item.productId === newItem.productId
    );
    if (existingItem) {
      console.log('이미 장바구니에 있는 상품입니다:', newItem.title);
      return {}; // 상태 변경 없음
    } else {
      const updatedItems = [...state.items, newItem];
      console.log('장바구니에 새 상품 추가:', updatedItems);
      return { items: updatedItems };
    }
  }),

  // removeFromCart 함수 (productId 기준)
  removeFromCart: (productIdToRemove) => set((state) => {
      console.log('장바구니에서 상품 제거:', productIdToRemove);
      return {
          items: state.items.filter((item) => item.productId !== productIdToRemove),
      };
  }),


  // isInCart 함수
  isInCart: (productId) => {
    const state = get();
    return state.items.some(item => item.productId === productId);
  },

  // clearCart 함수 추가
  clearCart: () => set({ items: [] }), // items 배열을 빈 배열로 설정

}));