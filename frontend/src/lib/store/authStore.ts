// 리액트 애플리케이션을 위한 상태 관리 라이브러리
// 매우 가볍고 빠르며, 복잡하지 않은 API를 사용하여 전역 상태(Global State)를 관리할 수 있게 해줌

import { create } from 'zustand';
import { AuthStore, User } from '@/types/auth.types';
import api from '@/lib/api/axios';

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

    logout: async () => {
        try {
            // 백엔드 로그아웃 API 호출
            await api.post('/users/logout');
        } catch (error) {
            console.error('로그아웃 API 에러:', error);
        } finally {
            // 로컬 상태 초기화
            localStorage.removeItem('accessToken');
            localStorage.removeItem('user');

            set({
                user: null,
                accessToken: null,
                isAuthenticated: false,
                isLoading: false,
            });

            console.log('✅ 로그아웃 완료');
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