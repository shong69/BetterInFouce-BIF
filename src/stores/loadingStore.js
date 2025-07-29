import { create } from "zustand";

export const useLoadingStore = create((set) => ({
  isLoading: false,
  message: "로딩 중...",

  showLoading: (message = "로딩 중...") => set({ isLoading: true, message }),

  hideLoading: () => set({ isLoading: false, message: "로딩 중..." }),

  setLoadingMessage: (message) => set({ message }),
}));
