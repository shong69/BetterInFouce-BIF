import { create } from "zustand";

export const useTodoStore = create((set) => ({
  selectedDate: new Date(
    new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" }),
  ),
  currentTodo: null,

  setSelectedDate: (date) => set({ selectedDate: date }),
  setCurrentTodo: (todo) => set({ currentTodo: todo }),
  clearCurrentTodo: () => set({ currentTodo: null }),
}));
