import { create } from "zustand";
import {
  fetchMonthlyDiaries as fetchMonthlyDiariesService,
  fetchDiary as fetchDiaryService,
  createDiary as createDiaryService,
  updateDiary as updateDiaryService,
  deleteDiary as deleteDiaryService,
} from "@services/diaryService";

export const useDiaryStore = create(function (set, _get) {
  return {
    diaries: [],
    currentDiary: null,
    loading: false,
    selectedEmotion: localStorage.getItem("selectedEmotion") || null,

    setSelectedEmotion: function (emotion) {
      localStorage.setItem("selectedEmotion", emotion);
      set({ selectedEmotion: emotion });
    },

    clearSelectedEmotion: function () {
      localStorage.removeItem("selectedEmotion");
      set({ selectedEmotion: null });
    },

    fetchMonthlyDiaries: async function (year, month) {
      set({ loading: true });
      try {
        const diaries = await fetchMonthlyDiariesService(year, month);
        set({ diaries, loading: false });
        return diaries;
      } catch (error) {
        console.error("월별 일기 목록 가져오기 실패:", error);
        set({ loading: false });
        throw error;
      }
    },

    fetchDiary: async function (id) {
      set({ loading: true });
      try {
        const diary = await fetchDiaryService(id);
        set({ currentDiary: diary, loading: false });
        return diary;
      } catch (error) {
        console.error("일기 가져오기 실패:", error);
        set({ loading: false });
        throw error;
      }
    },

    createDiary: async (diaryData) => {
      set({ loading: true });
      try {
        const newDiary = await createDiaryService(diaryData);

        set(function (state) {
          const currentDiaries = Array.isArray(state.diaries)
            ? state.diaries
            : [];

          return {
            diaries: [...currentDiaries, newDiary],
            loading: false,
          };
        });

        return newDiary;
      } catch (error) {
        console.error("일기 생성 실패:", error);
        set({ loading: false });
        throw error;
      }
    },

    updateDiary: async (id, data) => {
      set({ loading: true });
      try {
        const updatedDiary = await updateDiaryService(id, data);
        set({
          currentDiary: updatedDiary,
          loading: false,
        });
        return updatedDiary;
      } catch (error) {
        console.error("일기 수정 실패:", error);
        set({ loading: false });
        throw error;
      }
    },

    deleteDiary: async (id) => {
      set({ loading: true });
      try {
        await deleteDiaryService(id);
        set(function (state) {
          const currentDiaries = Array.isArray(state.diaries)
            ? state.diaries
            : [];

          return {
            diaries: currentDiaries.filter(function (d) {
              return d.id !== parseInt(id);
            }),
            currentDiary: null,
            loading: false,
          };
        });
      } catch (error) {
        console.error("일기 삭제 실패:", error);
        set({ loading: false });
        throw error;
      }
    },
  };
});
