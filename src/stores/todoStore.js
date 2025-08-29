import { create } from "zustand";
import { formatDateToYMD } from "@utils/dateUtils";

export const useTodoStore = create((set) => ({
  selectedDate: formatDateToYMD(
    new Date(new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" })),
  ),

  setSelectedDate: (date) => set({ selectedDate: date }),
}));
