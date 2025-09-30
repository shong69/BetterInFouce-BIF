import { create } from "zustand";
import { formatDateToYMD } from "@utils/dateUtils";
import { updateTodoCompletion } from "@services/todoService";

export const useTodoStore = create((set, get) => ({
  selectedDate: formatDateToYMD(
    new Date(new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" })),
  ),
  routines: [],
  tasks: [],
  needsRefresh: false,

  setSelectedDate: (date) => set({ selectedDate: date }),

  setTodos: (todos) => {
    const routines = todos.filter((todo) => todo.type === "ROUTINE");
    const tasks = todos.filter((todo) => todo.type === "TASK");
    set({ routines, tasks });
  },

  setNeedsRefresh: (value) => set({ needsRefresh: value }),

  updateTodoCompletion: async (todoId, isCompleted, date) => {
    const originalRoutines = get().routines;
    const originalTasks = get().tasks;

    const update = (items) =>
      items.map((item) =>
        item.todoId === todoId ? { ...item, isCompleted } : item,
      );

    set((state) => ({
      routines: update(state.routines),
      tasks: update(state.tasks),
    }));

    try {
      await updateTodoCompletion(todoId, isCompleted, date);
    } catch (error) {
      set({ routines: originalRoutines, tasks: originalTasks });
      throw error;
    }
  },
}));
