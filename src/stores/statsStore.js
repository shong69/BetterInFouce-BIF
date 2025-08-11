// import { create } from "zustand";
// import api from "@services/api";

// export const useStatsStore = create((set) => ({
//   stats: null,
//   loading: false,
//   error: null,

//   fetchMonthlyStats: async (bifId, year, month) => {
//     set({ loading: true, error: null });
//     try {
//       console.log("API 호출 시작:", { bifId, year, month });

//       // context-path /api를 포함한 경로로
//       const response = await api.get(`/api/stat?username=test_user`);

//       console.log("API 응답:", response);

//       // ApiResponse 구조에 맞춰 .data.data 사용
//       set({ stats: response.data.data, loading: false });
//     } catch (error) {
//       console.error("API 에러:", error);
//       set({
//         error: error.response?.data?.message || "통계 데이터를 불러오는데 실패했습니다.",
//         loading: false
//       });
//     }
//   }
// }));

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
