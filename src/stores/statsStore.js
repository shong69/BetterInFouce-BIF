import { create } from "zustand";
import api from "@services/api";

export const useStatsStore = create((set) => ({
  stats: null,
  loading: false,
  error: null,

  fetchMonthlyStats: async (bifId, year, month) => {
    set({ loading: true, error: null });
    try {
      // 백엔드의 실제 API 엔드포인트 사용
      const response = await api.get(`/api/stats/stats`, {
        params: { bifId, year, month },
      });
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
