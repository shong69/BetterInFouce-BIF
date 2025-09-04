import { create } from "zustand";
import { formatDateToYMD } from "@utils/dateUtils";

export const useTodoStore = create((set) => ({
  selectedDate: formatDateToYMD(
    new Date(new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" })),
  ),
  needsRefresh: false,

  setSelectedDate: (date) => set({ selectedDate: date }),
  setNeedsRefresh: (status) => set({ needsRefresh: status }),
}));
