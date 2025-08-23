import { create } from "zustand";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const useUserStore = create((set, get) => ({
  accessToken: null,
  user: null,
  registrationInfo: null,
  isLoading: true,

  isAuthenticated: () => !!get().accessToken,

  setAccessToken: (token) => {
    sessionStorage.setItem("accessToken", token);
    set({
      accessToken: token,
      registrationInfo: null,
    });
  },

  setUser: (user) => set({ user }),

  setRegistrationInfo: (info) =>
    set({
      registrationInfo: info,
      accessToken: null,
    }),

  setLoading: (loading) =>
    set({
      isLoading: loading,
    }),

  initializeAuth: async () => {
    set({ isLoading: true });

    const isTokenExpired = (token) => {
      try {
        const base64Url = token.split(".")[1];
        if (!base64Url) return true;
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64.padEnd(
          base64.length + ((4 - (base64.length % 4)) % 4),
          "=",
        );
        const binary = atob(padded);
        const bytes = new Uint8Array([...binary].map((c) => c.charCodeAt(0)));
        const json = new TextDecoder("utf-8").decode(bytes);
        const payload = JSON.parse(json);
        const currentTime = Math.floor(Date.now() / 1000);
        return payload.exp < currentTime + 30;
      } catch {
        return true;
      }
    };

    const getTokenPayload = (token) => {
      try {
        const base64Url = token.split(".")[1];
        if (!base64Url) return null;
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64.padEnd(
          base64.length + ((4 - (base64.length % 4)) % 4),
          "=",
        );
        const binary = atob(padded);
        const bytes = new Uint8Array([...binary].map((c) => c.charCodeAt(0)));
        const json = new TextDecoder("utf-8").decode(bytes);
        return JSON.parse(json);
      } catch {
        return null;
      }
    };

    const savedToken = sessionStorage.getItem("accessToken");
    if (savedToken && !isTokenExpired(savedToken)) {
      const payload = getTokenPayload(savedToken);
      set({
        accessToken: savedToken,
        user: payload
          ? {
              userRole: payload.userRole || payload.role,
              bifId: payload.bifId,
              nickname: payload.nickname,
              provider: payload.provider,
              providerUniqueId: payload.sub,
            }
          : null,
        registrationInfo: null,
        isLoading: false,
      });
      return;
    } else if (savedToken) {
      sessionStorage.removeItem("accessToken");
    }

    let sessionInfoSuccess = false;

    try {
      const isMobile =
        /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
          navigator.userAgent,
        );

      const fetchOptions = {
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
      };

      if (isMobile) {
        fetchOptions.cache = "no-cache";
        fetchOptions.mode = "cors";
      }

      const response = await fetch(
        `${API_BASE_URL}/api/auth/session-info`,
        fetchOptions,
      );

      if (response.ok) {
        const result = await response.json();
        const { data } = result;

        if (data.accessToken) {
          const tokenPayload = getTokenPayload(data.accessToken);

          set({
            accessToken: data.accessToken,
            user: {
              providerUniqueId: data.providerUniqueId,
              userRole: data.userRole || data.role || tokenPayload?.role,
              bifId: data.bifId,
              nickname: data.nickname,
              provider: data.provider,
            },
            registrationInfo: null,
            isLoading: false,
          });
          sessionStorage.setItem("accessToken", data.accessToken);
          sessionInfoSuccess = true;
          return;
        } else if (data.registrationInfo) {
          set({
            registrationInfo: data.registrationInfo,
            accessToken: null,
            user: null,
            isLoading: false,
          });
          return;
        } else {
          set({
            accessToken: null,
            user: null,
            registrationInfo: null,
            isLoading: false,
          });
        }
      } else if (response.status === 401) {
        const result = await response.json();
        if (result.code === "EXISTING_USER_LOGOUT_REQUIRED") {
          set({
            accessToken: null,
            user: null,
            registrationInfo: null,
            isLoading: false,
          });
          return;
        }
      }
    } catch {
      set({
        accessToken: null,
        user: null,
        registrationInfo: null,
        isLoading: false,
      });
    }

    if (!sessionInfoSuccess) {
      try {
        const refreshResponse = await fetch(
          `${API_BASE_URL}/api/auth/refresh`,
          {
            method: "POST",
            credentials: "include",
          },
        );

        if (refreshResponse.ok) {
          const result = await refreshResponse.json();
          if (result.data?.accessToken) {
            const tokenPayload = getTokenPayload(result.data.accessToken);

            set({
              accessToken: result.data.accessToken,
              user: {
                providerUniqueId: result.data.providerUniqueId,
                userRole:
                  result.data.userRole ||
                  result.data.role ||
                  tokenPayload?.role,
                bifId: result.data.bifId,
                nickname: result.data.nickname,
                provider: result.data.provider,
              },
              isLoading: false,
              registrationInfo: null,
            });
            sessionStorage.setItem("accessToken", result.data.accessToken);
            return;
          }
        }
      } catch (refreshError) {
        throw ("Refresh failed:", refreshError);
      }
    }
    set({
      accessToken: null,
      user: null,
      registrationInfo: null,
      isLoading: false,
    });
  },

  logout: async () => {
    try {
      await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
      });
    } finally {
      sessionStorage.removeItem("accessToken");
      set({
        accessToken: null,
        user: null,
        registrationInfo: null,
        isLoading: false,
      });
    }
  },

  registerBif: async (socialId, email) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/register/bif`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ socialId, email }),
      });

      if (response.ok) {
        const result = await response.json();
        if (result.data?.accessToken) {
          get().setAccessToken(result.data.accessToken);
          const bifData = result.data.bif;
          set({
            user: {
              userRole: "BIF",
              bifId: bifData.bifId,
              nickname: bifData.nickname,
            },
            registrationInfo: null,
          });
          return { success: true };
        }
      }

      return { success: false };
    } catch (error) {
      return { success: false, error };
    }
  },

  registerGuardian: async (socialId, email, connectionCode) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/auth/register/guardian`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({ socialId, email, connectionCode }),
        },
      );

      if (response.ok) {
        const result = await response.json();
        if (result.data?.accessToken) {
          get().setAccessToken(result.data.accessToken);

          const guardianData = result.data.guardian;
          set({
            user: {
              userRole: "GUARDIAN",
              bifId: guardianData.bifId,
              nickname: guardianData.nickname,
            },
            registrationInfo: null,
          });
          return { success: true };
        }
      }

      return { success: false };
    } catch (error) {
      return { success: false, error };
    }
  },

  changeNickname: async (newNickname) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/changenickname`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${get().accessToken}`,
        },
        credentials: "include",
        body: JSON.stringify({ nickname: newNickname }),
      });

      if (response.ok) {
        const result = await response.json();
        const { data } = result;

        set({
          accessToken: data.accessToken,
          user: {
            ...get().user,
            nickname: data.nickname,
          },
        });

        sessionStorage.setItem("accessToken", data.accessToken);

        return {
          success: true,
          message: data.message || "닉네임이 변경되었습니다.",
        };
      } else {
        const errorData = await response.json();
        return {
          success: false,
          message: errorData.message || "닉네임 변경 중 오류가 발생했습니다.",
        };
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error("닉네임 변경 실패:", error);
      return {
        success: false,
        message: "닉네임 변경 중 오류가 발생했습니다.",
      };
    }
  },

  withdraw: async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/withdraw`, {
        method: "DELETE",
        credentials: "include",
        headers: {
          Authorization: `Bearer ${get().accessToken}`,
        },
      });
      if (response.ok) {
        sessionStorage.removeItem("accessToken");

        set({
          accessToken: null,
          user: null,
          registrationInfo: null,
          isLoading: false,
        });

        return { success: true, message: "회원탈퇴가 완료되었습니다." };
      } else {
        return {
          success: false,
          message: "회원탈퇴 중 오류가 발생했습니다.",
        };
      }
    } catch {
      return {
        success: false,
        message: "회원탈퇴 중 오류가 발생했습니다.",
      };
    }
  },
}));
