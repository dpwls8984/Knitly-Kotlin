// //인증 api
// import api from './axios';  // 🔥 './client' → './axios'로 변경
//
// /**
//  * 로그인 요청
//  */
// export const login = async (email: string, password: string) => {
//     const response = await api.post('/auth/login', {
//         email,
//         password,
//     });
//     return response.data; // { accessToken, user }
// };
//
// /**
//  * 로그아웃 요청
//  */
// export const logout = async () => {
//     const response = await api.post('/users/logout');
//     return response.data;
// };
//
// /**
//  * 회원가입 요청
//  */
// export const register = async (userData: {
//     email: string;
//     password: string;
//     name: string;
//     nickname: string;
// }) => {
//     const response = await api.post('/auth/register', userData);
//     return response.data;
// };
//
// /**
//  * Access Token 재발급 (RT를 이용)
//  * ⚠️ 일반적으로 직접 호출하지 않음 (Interceptor가 자동 처리)
//  */
// export const refreshAccessToken = async () => {
//     const response = await api.post('/auth/refresh');
//     return response.data; // { accessToken }
// };
//
