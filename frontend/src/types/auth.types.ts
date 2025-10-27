// 사용자 정보
export interface User {
    userId: string;
    email: string;
    name: string;
    provider?: 'GOOGLE';
    createdAt?: string;
}

// 토큰 응답
export interface TokenResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
}

// 토큰 갱신 요청
export interface TokenRefreshRequest {
    refreshToken: string;
}

// 로그인 상태
export interface AuthState {
    user: User | null;
    accessToken: string | null;
    isLoading: boolean;
    isAuthenticated: boolean;
}

// 로그인 함수 타입
export interface AuthActions {
    login: (accessToken: string, user: User) => void;
    logout: () => Promise<void>;
    setUser: (user: User | null) => void;
    setAccessToken: (token: string | null) => void;
    initAuth: () => void;
}

// Auth Store 전체 타입
export type AuthStore = AuthState & AuthActions;