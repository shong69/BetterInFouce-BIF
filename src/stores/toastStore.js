import { create } from "zustand";

export const useToastStore = create((set, get) => ({
  toasts: [],
  nextId: 1,

  addToast: (message, type = "info", options = {}) => {
    const id = get().nextId;
    const newToast = {
      id,
      message,
      type,
      position: options.position || "top-center",
      duration: options.duration !== undefined ? options.duration : 3000,
      onClick: options.onClick,
    };

    set((state) => ({
      toasts: [...state.toasts, newToast],
      nextId: state.nextId + 1,
    }));

    return id;
  },

  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id),
    })),

  clearAllToasts: () => set({ toasts: [] }),

  showSuccess: (message, options) => {
    return get().addToast(message, "success", options);
  },

  showError: (message, options) => {
    return get().addToast(message, "error", options);
  },

  showWarning: (message, options) => {
    return get().addToast(message, "warning", options);
  },

  showInfo: (message, options) => {
    return get().addToast(message, "info", options);
  },
}));
