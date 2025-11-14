//axios 설정
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/lib/store/authStore';

const api = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
    withCredentials: true,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 🔥 Refresh 중복 호출 방지
let isRefreshing = false;
let failedQueue: Array<{
    resolve: (value?: any) => void;
    reject: (error?: any) => void;
}> = [];

const processQueue = (error: any = null, token: string | null = null) => {
    failedQueue.forEach((promise) => {
        if (error) {
            promise.reject(error);
        } else {
            promise.resolve(token);
        }
    });
    failedQueue = [];
};

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

        // 401이 아니거나 refresh API 자체가 실패한 경우
        if (
            error.response?.status !== 401 ||
            originalRequest.url?.includes('/auth/refresh')
        ) {
            return Promise.reject(error);
        }

        // 이미 재시도한 요청이면 로그아웃
        if (originalRequest._retry) {
            console.error('❌ Refresh Token도 만료됨 - 로그아웃');
            useAuthStore.getState().logout();
            if (typeof window !== 'undefined') {
                window.location.href = '/login';
            }
            return Promise.reject(error);
        }

        // 🔥 현재 refresh 중이면 대기열에 추가
        if (isRefreshing) {
            return new Promise((resolve, reject) => {
                failedQueue.push({ resolve, reject });
            })
                .then((token) => {
                    if (originalRequest.headers) {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                    }
                    return api(originalRequest);
                })
                .catch((err) => {
                    return Promise.reject(err);
                });
        }

        // 🔥 Refresh 시작
        originalRequest._retry = true;
        isRefreshing = true;

        try {
            console.log('🔄 Access Token 갱신 시도...');

            const { data } = await axios.post(
                `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/auth/refresh`,
                {},
                { withCredentials: true }
            );

            const newAccessToken = data.accessToken;
            useAuthStore.getState().setAccessToken(newAccessToken);

            console.log('✅ Token 갱신 성공');

            // 🔥 대기 중인 요청들에게 새 토큰 전달
            processQueue(null, newAccessToken);

            // 원래 요청 재시도
            if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
            }
            return api(originalRequest);

        } catch (refreshError) {
            console.error('❌ Token 갱신 실패:', refreshError);

            // 🔥 대기 중인 요청들도 모두 실패 처리
            processQueue(refreshError, null);

            useAuthStore.getState().logout();
            if (typeof window !== 'undefined') {
                window.location.href = '/login';
            }
            return Promise.reject(refreshError);

        } finally {
            isRefreshing = false;
        }
    }
);

export default api;