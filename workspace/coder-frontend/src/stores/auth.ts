/*****************************************************************************************
	Program ID   : auth
	Program Type : ts
	Program Name : 인증 스토어
	Description  : JWT 파싱/검증 유틸과 로그인 상태(토큰, 사용자 정보)를 관리하는 Pinia 인증 스토어
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { msg } from '@/utils/message';
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { loginApi, type UserProfile as ApiUserProfile } from '@/api/auth';
import { useAlarmStore } from '@/stores/alarm';
import { useComponentAuthStore } from '@/stores/componentAuth';

export type UserProfile = ApiUserProfile;

const TOKEN_KEY = 'coder_token';
const USER_KEY = 'coder_user';

/**
 * parseJwt : JWT 토큰의 페이로드를 디코딩
 * @param   token - JWT 문자열
 * @return  페이로드 객체 (형식이 올바르지 않으면 null)
 */
export function parseJwt(token: string): any | null {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;

  try {
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (err) {
    console.warn('[JWT] 유효하지 않은 토큰 페이로드:', err);
    return null;
  }
}

/**
 * isValidJwt : JWT 토큰이 올바른 형식이고 만료되지 않았는지 검증
 * @param   token - JWT 문자열
 * @return  유효하면 true
 */
export function isValidJwt(token: string | null | undefined): boolean {
  if (!token) return false;
  const payload = parseJwt(token);
  if (!payload) return false;

  if (payload.exp) {
    const nowInSeconds = Math.floor(Date.now() / 1000);
    if (payload.exp < nowInSeconds) {
      console.warn('[JWT] 토큰이 만료되었습니다.');
      return false;
    }
  }

  return true;
}

/**
 * toUserProfile : JWT 페이로드로 사용자 정보를 구성 (값이 없는 권한은 부여하지 않음)
 * @param   payload - parseJwt 결과
 * @return  사용자 정보
 */
function toUserProfile(payload: any): UserProfile {
  return {
    id: payload?.sub,
    username: payload?.sub || '',
    name: payload?.name || payload?.sub || '',
    role: payload?.role || 'GUEST',
    email: payload?.email,
    permissions: payload?.permissions || [],
  };
}

/**
 * useAuthStore : 인증 Pinia 스토어 정의: 토큰·사용자 정보 상태와 로그인/로그아웃 동작 제공
 * @return  N/A
 */
export const useAuthStore = defineStore('auth', () => {
  const initialToken = localStorage.getItem(TOKEN_KEY);
  const token = ref<string | null>(initialToken && isValidJwt(initialToken) ? initialToken : null);
  
  const initialUser = localStorage.getItem(USER_KEY);
  const user = ref<UserProfile | null>(
    token.value && initialUser ? JSON.parse(initialUser) : (
      token.value ? toUserProfile(parseJwt(token.value)) : null
    )
  );

  const isAuthenticated = computed(() => !!token.value && isValidJwt(token.value));
  const currentUser = computed(() => user.value);
  const userRole = computed(() => user.value?.role || 'GUEST');

  /**
   * setAuth : 토큰과 사용자 정보를 상태 및 localStorage 에 저장 (사용자 정보가 없으면 토큰 페이로드로 구성)
   * @param   newToken - 저장할 JWT 토큰
   * @param   newUser  - 저장할 사용자 정보 (생략 가능)
   * @return  N/A (토큰이 유효하지 않으면 Error 발생)
   */
  function setAuth(newToken: string, newUser?: UserProfile) {
    if (!isValidJwt(newToken)) {
      throw new Error(msg('sy.login.token.invalid'));
    }
    token.value = newToken;
    localStorage.setItem(TOKEN_KEY, newToken);
    useComponentAuthStore().clearComponentAuth();

    user.value = newUser || toUserProfile(parseJwt(newToken));
    localStorage.setItem(USER_KEY, JSON.stringify(user.value));
  }

  /**
   * loginWithCredentials : 아이디/비밀번호로 로그인 API 를 호출하고 인증 정보를 저장
   * @param   username - 사용자명
   * @param   password - 비밀번호
   * @return  로그인한 사용자 프로필 (실패 시 Error 발생)
   */
  async function loginWithCredentials(username: string, password: string): Promise<UserProfile> {
    const res = await loginApi({ username, password });
    if (!res.success || !res.data?.accessToken) {
      throw new Error(res.message || msg('sy.login.auth.failed'));
    }

    const { accessToken, user: backendUser } = res.data;
    setAuth(accessToken, backendUser);
    return user.value!;
  }

  /**
   * login : 발급된 토큰과 사용자 정보로 로그인 상태를 설정
   * @param   newToken - JWT 토큰
   * @param   newUser  - 사용자 정보 (생략 가능)
   * @return  N/A
   */
  function login(newToken: string, newUser?: UserProfile) {
    setAuth(newToken, newUser);
  }

  /**
   * logout : 로그아웃: 알림 SSE 연결을 끊고 상태와 localStorage 의 토큰/사용자 정보를 삭제
   * @return  N/A
   */
  function logout() {
    useAlarmStore().disconnect();
    useComponentAuthStore().clearComponentAuth();
    token.value = null;
    user.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  return {
    token,
    user,
    currentUser,
    isAuthenticated,
    userRole,
    setAuth,
    login,
    loginWithCredentials,
    logout,
  };
});

export const useAuth = useAuthStore;
