import api from './axios';
/**
 * 회원 탈퇴
 */
export const deleteAccount = async () => {
    const response = await api.delete('/users/me');
    return response.data;
};

/**
 * 사용자 정보 조회
 */
export const getUserInfo = async () => {
    const response = await api.get('/users/me');
    return response.data;
};

/**
 * 로그아웃
 */
export const logOut = async () => {
    const response = await api.post('/users/logout');
    return response.data;
};