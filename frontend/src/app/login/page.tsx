'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';

export default function LoginPage() {
    const router = useRouter();
    const { user, isLoading } = useAuthStore();

    // 이미 로그인된 경우 홈으로 리다이렉트
    useEffect(() => {
        if (user) {
            router.push('/');
        }
    }, [user, router]);

    const handleGoogleLogin = () => {
        const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
        window.location.href = `${backendUrl}/oauth2/authorization/google`;
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
            </div>
        );
    }
    return (
      <div>
        <h1>로그인 페이지 (구글, 카카오 선택)</h1>
        src/app/product/login/page.tsx
          <div className="flex items-center justify-center min-h-screen bg-gray-50">
              <div className="bg-white shadow-lg rounded-lg p-8 max-w-md w-full">
                  <h1 className="text-3xl font-bold text-center mb-8 text-gray-800">
                      로그인
                  </h1>

                  <div className="space-y-4">
                      <button
                          onClick={handleGoogleLogin}
                          className="w-full flex items-center justify-center gap-3 bg-white border-2 border-gray-300 text-gray-700 px-6 py-3 rounded-lg hover:bg-gray-50 transition-colors font-medium"
                      >
                          <svg className="w-6 h-6" viewBox="0 0 24 24">
                              <path
                                  fill="#4285F4"
                                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                              />
                              <path
                                  fill="#34A853"
                                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                              />
                              <path
                                  fill="#FBBC05"
                                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                              />
                              <path
                                  fill="#EA4335"
                                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                              />
                          </svg>
                          Google로 로그인
                      </button>

                      <p className="text-center text-sm text-gray-500 mt-6">
                          로그인하면 서비스 약관에 동의하는 것으로 간주됩니다.
                      </p>
                  </div>
              </div>
          </div>
      </div>
    );
}