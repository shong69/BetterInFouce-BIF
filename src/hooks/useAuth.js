import { useState, useCallback } from "react";
import api from "@utils/api";

export function useAuth() {
  const [authState, setAuthState] = useState({
    isAuthenticated: false,
    isLoading: true,
    user: null,
    registrationInfo: null,
  });

  const getAccessToken = useCallback(() => {
    return sessionStorage.getItem("accessToken");
  }, []);

  const checkAuthStatus = useCallback(async () => {
    setAuthState((prev) => ({ ...prev, isLoading: true }));

    const localToken = getAccessToken();

    if (localToken) {
      setAuthState({
        isAuthenticated: true,
        isLoading: false,
        user: null,
        registrationInfo: null,
      });
      return true;
    }

    try {
      const response = await api.get("/api/auth/session-info");
      const { data } = response.data;

      if (data.accessToken) {
        sessionStorage.setItem("accessToken", data.accessToken);

        setAuthState({
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
        return true;
      } else if (data.registrationInfo) {
        setAuthState({
          isAuthenticated: false,
          isLoading: false,
          user: null,
          registrationInfo: data.registrationInfo,
        });
        return false;
      } else {
        setAuthState({
          isAuthenticated: false,
          isLoading: false,
          user: null,
          registrationInfo: null,
        });
        return false;
      }
    } catch {
      setAuthState({
        isAuthenticated: false,
        isLoading: false,
        user: null,
        registrationInfo: null,
      });
      return false;
    }
  }, [getAccessToken]);

  const logout = useCallback(async () => {
    try {
      await api.post("/api/auth/logout");
    } finally {
      sessionStorage.removeItem("accessToken");
      setAuthState({
        isAuthenticated: false,
        isLoading: false,
        user: null,
        registrationInfo: null,
      });
    }
  }, []);

  return {
    ...authState,
    checkAuthStatus,
    logout,
    getAccessToken,
  };
}
