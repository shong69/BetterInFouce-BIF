import { create } from "zustand";

const useTabStore = create((set) => ({
  activeTab: "simulations", // 기본값을 시뮬레이션으로 설정
  setActiveTab: (tab) => set({ activeTab: tab }),
}));

export default useTabStore;
