import { useState, useEffect } from "react";
import api from "@services/api";

// 전역 상태 (모든 컴포넌트가 공유)
let globalAuth = {
  isAuthenticated: false,
  isLoading: true,
  user: null,
  registrationInfo: null,
};

let globalSetters = [];
let hasInitialized = false;

export function useAuth() {
  const [auth, setAuth] = useState(globalAuth);

  // 컴포넌트 마운트 시 전역 setter 등록
  useEffect(() => {
    globalSetters.push(setAuth);

    // 아직 초기화되지 않았으면 한 번만 초기화
    if (!hasInitialized) {
      hasInitialized = true;
      initAuth();
    }

    return () => {
      globalSetters = globalSetters.filter((setter) => setter !== setAuth);
    };
  }, []);

  return auth;
}

// 전역 상태 업데이트 함수
function updateGlobalAuth(newAuth) {
  globalAuth = newAuth;
  globalSetters.forEach((setter) => setter(newAuth));
}

// 초기화 함수 (한 번만 실행)
async function initAuth() {
  console.log("🚀 인증 초기화 시작");

  try {
    // 1. 브라우저에 토큰이 있는지 확인
    const localToken = sessionStorage.getItem("accessToken");

    if (localToken) {
      console.log("✅ 로컬 토큰 있음");
      updateGlobalAuth({
        isAuthenticated: true,
        isLoading: false,
        user: null,
        registrationInfo: null,
      });
      return;
    }

    // 2. 서버 세션 확인
    console.log("🌐 서버 세션 확인");
    const response = await api.get("/api/auth/session-info");
    const { data } = response.data;

    if (data?.accessToken) {
      console.log("🔑 서버에서 토큰 받음");
      sessionStorage.setItem("accessToken", data.accessToken);

      updateGlobalAuth({
        isAuthenticated: true,
        isLoading: false,
        user: {
          providerUniqueId: data.providerUniqueId,
          userRole: data.userRole,
          bifId: data.bifId,
          nickname: data.nickname,
          provider: data.provider,
        },
        registrationInfo: null,
      });
    } else if (data?.registrationInfo) {
      console.log("📝 등록 정보 있음");
      updateGlobalAuth({
        isAuthenticated: false,
        isLoading: false,
        user: null,
        registrationInfo: data.registrationInfo,
      });
    } else {
      console.log("❌ 인증 정보 없음");
      updateGlobalAuth({
        isAuthenticated: false,
        isLoading: false,
        user: null,
        registrationInfo: null,
      });
    }
  } catch (error) {
    console.error("💥 인증 초기화 실패:", error);
    updateGlobalAuth({
      isAuthenticated: false,
      isLoading: false,
      user: null,
      registrationInfo: null,
    });
  }
}

// 로그아웃 함수
export function logout() {
  sessionStorage.removeItem("accessToken");
  updateGlobalAuth({
    isAuthenticated: false,
    isLoading: false,
    user: null,
    registrationInfo: null,
  });
}
