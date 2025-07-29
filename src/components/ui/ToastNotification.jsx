import { useToastStore } from "@stores/toastStore";
import { useEffect } from "react";

function ToastItem({ toast }) {
  const { removeToast } = useToastStore();

  useEffect(() => {
    if (toast.duration > 0) {
      const timer = setTimeout(() => {
        removeToast(toast.id);
      }, toast.duration);

      return () => clearTimeout(timer);
    }
  }, [toast.id, toast.duration, removeToast]);

  const typeStyles = {
    success: "bg-primary text-white",
    error: "bg-warning text-white",
    warning: "bg-gradient-yellow text-white",
    info: "bg-blue-500 text-white",
  };

  const icons = {
    success: "✓",
    error: "✕",
    warning: "⚠",
    info: "ℹ",
  };

  const positionStyles = {
    "top-right": "top-4 right-4",
    "top-left": "top-4 left-4",
    "top-center": "top-4 left-1/2 transform -translate-x-1/2",
    "bottom-right": "bottom-4 right-4",
    "bottom-left": "bottom-4 left-4",
    "bottom-center": "bottom-4 left-1/2 transform -translate-x-1/2",
  };

  return (
    <div
      className={`fixed z-[9999] animate-bounce ${positionStyles[toast.position]} transition-all duration-300 ease-in-out`}
    >
      <div
        className={`flex items-center space-x-3 rounded-lg px-4 py-3 shadow-lg ${typeStyles[toast.type]} min-w-[300px]`}
      >
        <span className="text-lg font-bold">{icons[toast.type]}</span>
        <span className="flex-1 text-sm font-medium">{toast.message}</span>
        <button
          onClick={() => removeToast(toast.id)}
          className="text-lg font-bold opacity-70 transition-opacity hover:opacity-100"
        >
          ×
        </button>
      </div>
    </div>
  );
}

export default function ToastNotification() {
  const { toasts } = useToastStore();

  return (
    <>
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} />
      ))}
    </>
  );
}
