// app/mypage/quit/page.tsx

'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { deleteAccount } from '@/lib/api/user.api';

export default function QuitPage() {
    const router = useRouter();
    const { user, logout } = useAuthStore();
    const [isChecked, setIsChecked] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [confirmText, setConfirmText] = useState('');

    const handleDeleteAccount = async () => {
        // 입력 확인
        if (confirmText !== '회원탈퇴') {
            alert('정확히 "회원탈퇴"를 입력해주세요.');
            return;
        }

        if (!isChecked) {
            alert('탈퇴 안내사항을 확인해주세요.');
            return;
        }

        // 최종 확인
        const finalConfirm = window.confirm(
            '정말로 탈퇴하시겠습니까?\n\n탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.'
        );

        if (!finalConfirm) return;

        try {
            setIsLoading(true);

            // 회원 탈퇴 API 호출
            await deleteAccount();

            alert('회원탈퇴가 완료되었습니다.\n그동안 이용해주셔서 감사합니다.');
            console.log("회원탈퇴 성공");

            // 로그아웃 처리
            logout();
            console.log("로그아웃 처리");

            // 홈으로 이동
            router.push('/');
            console.log("홈으로 이동");


        } catch (error: any) {
            console.error('회원 탈퇴 실패:', error);

            if (error.response?.status === 401) {
                alert('로그인이 필요합니다.');
                //router.push('/login');
            } else {
                alert('회원 탈퇴 중 오류가 발생했습니다.\n잠시 후 다시 시도해주세요.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="container mx-auto px-4 py-8 max-w-2xl">
            {/* 헤더 */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold mb-2">회원 탈퇴</h1>
                <p className="text-gray-600">
                    탈퇴 전 아래 내용을 꼭 확인해주세요.
                </p>
            </div>

            {/* 경고 박스 */}
            <div className="bg-red-50 border-2 border-red-200 rounded-lg p-6 mb-6">
                <h2 className="text-xl font-bold text-red-700 mb-4 flex items-center">
                    ⚠️ 탈퇴 시 주의사항
                </h2>
                <ul className="space-y-3 text-gray-700">
                    <li className="flex items-start">
                        <span className="text-red-500 mr-2">•</span>
                        <span>회원 정보 및 구매 내역이 <strong>모두 삭제</strong>됩니다.</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-red-500 mr-2">•</span>
                        <span>보유하신 포인트 및 쿠폰은 <strong>소멸</strong>됩니다.</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-red-500 mr-2">•</span>
                        <span>작성한 리뷰 및 댓글은 <strong>삭제되지 않으며</strong>, 탈퇴 후 수정/삭제가 불가능합니다.</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-red-500 mr-2">•</span>
                        <span>판매자인 경우, 판매 중인 상품이 <strong>모두 삭제</strong>됩니다.</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-red-500 mr-2">•</span>
                        <span>탈퇴 후 <strong>같은 이메일로 재가입이 불가능</strong>할 수 있습니다.</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-red-500 mr-2">•</span>
                        <span>탈퇴 처리 후에는 <strong>데이터 복구가 불가능</strong>합니다.</span>
                    </li>
                </ul>
            </div>

            {/* 사용자 정보 */}
            <div className="bg-gray-50 rounded-lg p-6 mb-6">
                <h2 className="text-lg font-bold mb-3">탈퇴할 계정 정보</h2>
                <div className="space-y-2 text-gray-700">
                    <p>이메일: <strong>{user?.email}</strong></p>
                    <p>이름: <strong>{user?.name}</strong></p>
                </div>
            </div>

            {/* 확인 입력 */}
            <div className="bg-white border rounded-lg p-6 mb-6">
                <label className="block mb-4">
                    <span className="text-gray-700 font-medium mb-2 block">
                        탈퇴를 원하시면 아래에 <strong className="text-red-600">"회원탈퇴"</strong>를 입력해주세요.
                    </span>
                    <input
                        type="text"
                        value={confirmText}
                        onChange={(e) => setConfirmText(e.target.value)}
                        placeholder="회원탈퇴"
                        className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                        disabled={isLoading}
                    />
                </label>

                <label className="flex items-start cursor-pointer">
                    <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={(e) => setIsChecked(e.target.checked)}
                        className="mt-1 mr-3 w-5 h-5"
                        disabled={isLoading}
                    />
                    <span className="text-gray-700">
                        위 내용을 모두 확인하였으며, 탈퇴에 동의합니다.
                    </span>
                </label>
            </div>

            {/* 버튼 */}
            <div className="flex gap-4">
                <button
                    onClick={() => router.back()}
                    disabled={isLoading}
                    className="flex-1 px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    취소
                </button>
                <button
                    onClick={handleDeleteAccount}
                    disabled={isLoading || !isChecked || confirmText !== '회원탈퇴'}
                    className="flex-1 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium"
                >
                    {isLoading ? '처리 중...' : '회원 탈퇴'}
                </button>
            </div>

            {/* 문의 안내 */}
            <div className="mt-8 text-center text-sm text-gray-500">
                <p>탈퇴 관련 문의사항이 있으시면 고객센터로 연락주세요.</p>
                <p className="mt-1">이메일: support@example.com</p>
            </div>
        </div>
    );
}