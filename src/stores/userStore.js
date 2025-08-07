import { create } from "zustand";
import api from "@services/api";

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

    const savedToken = sessionStorage.getItem("accessToken");
    if (savedToken) {
      set({
        accessToken: savedToken,
        isLoading: false,
        registrationInfo: null,
      });
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/session-info`, {
        credentials: "include",
      });

      if (response.ok) {
        const result = await response.json();
        const { data } = result;

        if (data.accessToken) {
          set({
            accessToken: data.accessToken,
            user: {
              providerUniqueId: data.providerUniqueId,
              userRole: data.userRole,
              bifId: data.bifId,
              nickname: data.nickname,
              provider: data.provider,
            },
            registrationInfo: null,
            isLoading: false,
          });
          sessionStorage.setItem("accessToken", data.accessToken);
        } else if (data.registrationInfo) {
          set({
            registrationInfo: data.registrationInfo,
            accessToken: null,
            user: null,
            isLoading: false,
          });
        } else {
          set({
            accessToken: null,
            user: null,
            registrationInfo: null,
            isLoading: false,
          });
        }
      } else {
        set({
          accessToken: null,
          user: null,
          registrationInfo: null,
          isLoading: false,
        });
      }
    } catch {
      set({
        accessToken: null,
        user: null,
        registrationInfo: null,
        isLoading: false,
      });
    }
  },

  logout: async () => {
    try {
      await api.post("/api/auth/logout");
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
          set({ user: result.data.bif });
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
          set({ user: result.data.guardian });
          return { success: true };
        }
      }

      return { success: false };
    } catch (error) {
      return { success: false, error };
    }
  },
}));
