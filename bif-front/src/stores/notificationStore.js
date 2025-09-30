import { create } from "zustand";

const NOTIFICATION_HISTORY_KEY = "notification-history";
const MAX_HISTORY_SIZE = 100;

export const useNotificationStore = create((set, get) => ({
  notifications: [],

  initialize: () => {
    const savedHistory = localStorage.getItem(NOTIFICATION_HISTORY_KEY);
    if (savedHistory) {
      const parsed = JSON.parse(savedHistory);
      set({ notifications: parsed });
    }
  },

  addNotification: (notification) => {
    const newNotification = {
      id: Date.now() + Math.random(),
      ...notification,
      receivedAt: new Date().toISOString(),
      read: false,
    };

    set((state) => {
      const updated = [newNotification, ...state.notifications].slice(
        0,
        MAX_HISTORY_SIZE,
      );
      localStorage.setItem(NOTIFICATION_HISTORY_KEY, JSON.stringify(updated));
      return { notifications: updated };
    });
  },

  markAsRead: (notificationId) => {
    set((state) => {
      const updated = state.notifications.map((notification) =>
        notification.id === notificationId
          ? { ...notification, read: true }
          : notification,
      );
      localStorage.setItem(NOTIFICATION_HISTORY_KEY, JSON.stringify(updated));
      return { notifications: updated };
    });
  },

  markAllAsRead: () => {
    set((state) => {
      const updated = state.notifications.map((notification) => ({
        ...notification,
        read: true,
      }));
      localStorage.setItem(NOTIFICATION_HISTORY_KEY, JSON.stringify(updated));
      return { notifications: updated };
    });
  },

  deleteNotification: (notificationId) => {
    set((state) => {
      const updated = state.notifications.filter(
        (notification) => notification.id !== notificationId,
      );
      localStorage.setItem(NOTIFICATION_HISTORY_KEY, JSON.stringify(updated));
      return { notifications: updated };
    });
  },

  clearAllNotifications: () => {
    set({ notifications: [] });
    localStorage.removeItem(NOTIFICATION_HISTORY_KEY);
  },

  getStats: () => {
    const notifications = get().notifications;
    return {
      total: notifications.length,
      unread: notifications.filter((n) => !n.read).length,
      read: notifications.filter((n) => n.read).length,
    };
  },
}));
