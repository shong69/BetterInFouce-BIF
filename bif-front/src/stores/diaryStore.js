import { create } from "zustand";
import {
  fetchMonthlyDiaries as fetchMonthlyDiariesService,
  fetchDiary as fetchDiaryService,
  createDiary as createDiaryService,
  updateDiary as updateDiaryService,
  deleteDiary as deleteDiaryService,
} from "@services/diaryService";
import { useLoadingStore } from "./loadingStore";

export const useDiaryStore = create((set, get) => {
  return {
    diaries: [],
    currentDiary: null,
    selectedEmotion: localStorage.getItem("selectedEmotion") || null,
    diaryCache: new Map(),

    setSelectedEmotion: (emotion) => {
      localStorage.setItem("selectedEmotion", emotion);
      set({ selectedEmotion: emotion });
    },

    clearSelectedEmotion: () => {
      localStorage.removeItem("selectedEmotion");
      set({ selectedEmotion: null });
    },

    fetchMonthlyDiaries: async (year, month) => {
      const diaries = await fetchMonthlyDiariesService(year, month);
      set({ diaries });
      return diaries;
    },

    fetchDiary: async (id) => {
      const cached = get().diaryCache.get(id);
      if (cached) {
        set({ currentDiary: cached });
        return cached;
      }

      useLoadingStore.getState().showLoading("일기를 불러오는 중...");
      try {
        const diary = await fetchDiaryService(id);
        get().diaryCache.set(id, diary);
        set({ currentDiary: diary });
        useLoadingStore.getState().hideLoading();
        return diary;
      } catch (error) {
        useLoadingStore.getState().hideLoading();
        throw error;
      }
    },

    createDiary: async (diaryData) => {
      useLoadingStore.getState().showLoading("일기를 생성하는 중...");
      try {
        const newDiary = await createDiaryService(diaryData);
        set((state) => {
          const currentDiaries = Array.isArray(state.diaries)
            ? state.diaries
            : [];

          return {
            diaries: [...currentDiaries, newDiary],
          };
        });
        useLoadingStore.getState().hideLoading();
        return newDiary;
      } catch (error) {
        useLoadingStore.getState().hideLoading();
        throw error;
      }
    },

    updateDiary: async (id, data) => {
      useLoadingStore.getState().showLoading("일기 수정 중...");
      try {
        const updatedDiary = await updateDiaryService(id, data);

        get().diaryCache.set(id, updatedDiary);

        set({
          currentDiary: updatedDiary,
        });

        useLoadingStore.getState().hideLoading();
        return updatedDiary;
      } catch (error) {
        useLoadingStore.getState().hideLoading();
        throw error;
      }
    },

    deleteDiary: async (id) => {
      await deleteDiaryService(id);

      get().diaryCache.delete(id);

      set((state) => {
        const currentDiaries = Array.isArray(state.diaries)
          ? state.diaries
          : [];

        return {
          diaries: currentDiaries.filter((d) => {
            return d.id !== parseInt(id);
          }),
          currentDiary: null,
        };
      });
    },
  };
});
