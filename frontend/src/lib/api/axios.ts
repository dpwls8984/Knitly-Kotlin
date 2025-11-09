//axios 설정
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/lib/store/authStore';

const api = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
    withCredentials: true, // 쿠키 포함
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request Interceptor: Access Token 자동 추가
api.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem('accessToken');

        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error: AxiosError) => {
        return Promise.reject(error);
    }
);

// Response Interceptor: Token Refresh 처리
api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & {
            _retry?: boolean;
        };

        // Access Token 만료 시 자동 갱신
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                console.log('🔄 Access Token 갱신 시도...');

                // Refresh Token으로 새 토큰 발급
                const { data } = await axios.post(
                    `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/auth/refresh`,
                    {},
                    { withCredentials: true } // RT 쿠키 포함
                );

                const newAccessToken = data.accessToken;

                // 새 토큰 저장
                useAuthStore.getState().setAccessToken(newAccessToken);

                // 실패한 요청 재시도
                if (originalRequest.headers) {
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                }

                console.log('✅ Token 갱신 성공');
                return api(originalRequest);

            } catch (refreshError) {
                console.error('❌ Token 갱신 실패:', refreshError);

                // Refresh Token도 만료 → 로그아웃
                useAuthStore.getState().logout();

                // 로그인 페이지로 리다이렉트
                if (typeof window !== 'undefined') {
                    window.location.href = '/login';
                }

                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default api;