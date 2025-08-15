import { create } from "zustand";
import api from "@services/api";

export const useStatsStore = create((set) => ({
  stats: null,
  loading: false,
  error: null,

  fetchMonthlyStats: async (bifId) => {
    set({ loading: true, error: null });
    try {
      const response = await api.get(`/stats/test/stat`, { params: { bifId } });
      set({ stats: response.data.data, loading: false });
    } catch (error) {
      set({
        error:
          error.response?.data?.message ||
          "통계 데이터를 불러오는데 실패했습니다.",
        loading: false,
      });
    }
  },
}));
