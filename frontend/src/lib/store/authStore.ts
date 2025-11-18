import { create } from 'zustand';
import { AuthStore, User } from '@/types/auth.types';
import api from '@/lib/api/axios';
import { logOut as logOutApi } from '@/lib/api/user.api';

export const useAuthStore = create<AuthStore>((set, get) => ({
    // State
    user: null,
    accessToken: null,
    isLoading: true,
    isAuthenticated: false,

    // Actions
    login: (accessToken: string, user: User) => {
        // localStorage에 저장
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('user', JSON.stringify(user));

        set({
            user,
            accessToken,
            isAuthenticated: true,
            isLoading: false,
        });

        console.log('✅ 로그인 완료:', user);
    },

    logout: async (): Promise<void> => {
        try {
            // 1. 백엔드 로그아웃 API 호출 (refresh token 쿠키 삭제)
            await logOutApi();
            console.log('✅ 서버 로그아웃 완료');
        } catch (error) {
            console.error('❌ 로그아웃 API 호출 실패:', error);
            // API 실패해도 클라이언트 로그아웃은 진행
        } finally {
            // 2. 로컬 상태 초기화 (API 성공/실패 관계없이 실행)
            localStorage.removeItem('accessToken');
            localStorage.removeItem('user');

            set({
                user: null,
                accessToken: null,
                isAuthenticated: false,
                isLoading: false,
            });

            console.log('✅ 클라이언트 로그아웃 완료');
        }
    },

    setUser: (user: User | null) => {
        set({ user });
    },

    setAccessToken: (token: string | null) => {
        if (token) {
            localStorage.setItem('accessToken', token);
        } else {
            localStorage.removeItem('accessToken');
        }
        set({ accessToken: token });
    },

    // 초기 인증 상태 확인
    initAuth: () => {
        try {
            const token = localStorage.getItem('accessToken');
            const userStr = localStorage.getItem('user');

            if (token && userStr) {
                const user: User = JSON.parse(userStr);
                set({
                    user,
                    accessToken: token,
                    isAuthenticated: true,
                    isLoading: false,
                });
                console.log('✅ 기존 로그인 세션 복구:', user);
            } else {
                set({ isLoading: false });
            }
        } catch (error) {
            console.error('인증 초기화 에러:', error);
            set({ isLoading: false });
        }
    },
}));