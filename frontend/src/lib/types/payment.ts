// 결제 관련 타입 정의

export interface CartItem {
    id: string;
    productId: string;
    productName: string;
    price: number;
    imageUrl: string;
  }
  
  export interface PaymentConfirmRequest {
    paymentKey: string;
    orderId: string;
    amount: number;
  }
  
  export interface PaymentConfirmResponse {
    paymentId: number;
    paymentKey: string;
    orderId: string;
    orderName?: string;
    method: PaymentMethod;
    totalAmount: number;
    status: PaymentStatus;
    requestedAt: string;
    approvedAt?: string;
    mid?: string;
  }
  
  export enum PaymentMethod {
    CARD = 'CARD',
    TRANSFER = 'TRANSFER',
    VIRTUAL_ACCOUNT = 'VIRTUAL_ACCOUNT',
    MOBILE_PHONE = 'MOBILE_PHONE',
    CULTURE_GIFT_CERTIFICATE = 'CULTURE_GIFT_CERTIFICATE',
    BOOK_CULTURE_GIFT_CERTIFICATE = 'BOOK_CULTURE_GIFT_CERTIFICATE',
    GAME_CULTURE_GIFT_CERTIFICATE = 'GAME_CULTURE_GIFT_CERTIFICATE',
  }
  
  export enum PaymentStatus {
    READY = 'READY',
    IN_PROGRESS = 'IN_PROGRESS',
    WAITING_FOR_DEPOSIT = 'WAITING_FOR_DEPOSIT',
    DONE = 'DONE',
    CANCELED = 'CANCELED',
    FAILED = 'FAILED',
  }
  
  export interface OrderCreateRequest {
    cartItemIds: string[];
  }
  
  export interface OrderCreateResponse {
    orderId: number;
    orderNumber: string;
    totalPrice: number;
    orderStatus: string;
    createdAt: string;
  }