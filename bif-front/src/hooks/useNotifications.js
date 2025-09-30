import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useToastStore, useUserStore, useNotificationStore } from "@stores";

const NOTIFICATION_TYPES = {
  TODO_REMINDER: "todo-reminder",
  ROUTINE_REMINDER: "routine-reminder",
  ACHIEVEMENT: "achievement",
  SYSTEM: "system",
};

const CONNECTION_STATUS = {
  DISCONNECTED: "disconnected",
  CONNECTING: "connecting",
  CONNECTED: "connected",
  RECONNECTING: "reconnecting",
  FAILED: "failed",
};

export function useNotifications() {
  const navigate = useNavigate();
  const { showSuccess, showInfo, showError, clearAllToasts } = useToastStore();
  const { user } = useUserStore();
  const { addNotification } = useNotificationStore();

  const eventSourceRef = useRef(null);
  const retryCountRef = useRef(0);
  const reconnectTimeoutRef = useRef(null);
  const isManuallyClosedRef = useRef(false);
  const connectSSERef = useRef(null);

  const [connectionStatus, setConnectionStatus] = useState(
    CONNECTION_STATUS.DISCONNECTED,
  );
  const [connectionError, setConnectionError] = useState(null);
  const [lastConnectedAt, setLastConnectedAt] = useState(null);
  const [notificationStats, setNotificationStats] = useState({
    totalReceived: 0,
    todoReminders: 0,
    routineReminders: 0,
  });

  const baseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
  const maxRetries = 5;
  const maxReconnectDelay = 30000;

  function getRetryDelay(retryCount) {
    return Math.min(1000 * Math.pow(2, retryCount), maxReconnectDelay);
  }

  function updateConnectionStatus(status, error = null) {
    setConnectionStatus(status);
    setConnectionError(error);

    if (status === CONNECTION_STATUS.CONNECTED) {
      setLastConnectedAt(Date.now());
      retryCountRef.current = 0;
    }
  }

  function getStatKey(type) {
    switch (type) {
      case NOTIFICATION_TYPES.TODO_REMINDER:
        return "todoReminders";
      case NOTIFICATION_TYPES.ROUTINE_REMINDER:
        return "routineReminders";
      default:
        return "other";
    }
  }

  const updateNotificationStats = useCallback((type) => {
    setNotificationStats((prev) => ({
      ...prev,
      totalReceived: prev.totalReceived + 1,
      [getStatKey(type)]: (prev[getStatKey(type)] || 0) + 1,
    }));
  }, []);

  const handleTodoReminder = useCallback(
    (notification) => {
      showError(`📋 ${notification.body}`, {
        duration: null,
        position: "top-center",
        onClick: () => {
          if (notification.todoId) {
            clearAllToasts();
            navigate(`/todo/${notification.todoId}`);
          }
        },
      });
    },
    [showError, clearAllToasts, navigate],
  );

  const handleRoutineReminder = useCallback(
    (notification) => {
      showInfo(`🔄 ${notification.body}`, {
        duration: null,
        position: "top-center",
        onClick: () => {
          if (notification.todoId) {
            clearAllToasts();
            navigate(`/todo/${notification.todoId}`);
          }
        },
      });
    },
    [showInfo, clearAllToasts, navigate],
  );

  const handleAchievement = useCallback(
    (notification) => {
      showSuccess(`🎉 ${notification.title}: ${notification.body}`, {
        duration: null,
        position: "top-center",
        onClick: () => {
          clearAllToasts();
          navigate("/achievements");
        },
      });
    },
    [showSuccess, clearAllToasts, navigate],
  );

  const handleNotification = useCallback(
    (notification) => {
      addNotification(notification);

      updateNotificationStats(notification.type);

      switch (notification.type) {
        case NOTIFICATION_TYPES.TODO_REMINDER:
          handleTodoReminder(notification);
          break;
        case NOTIFICATION_TYPES.ROUTINE_REMINDER:
          handleRoutineReminder(notification);
          break;
        case NOTIFICATION_TYPES.ACHIEVEMENT:
          handleAchievement(notification);
          break;
        default:
          showInfo(notification.body || "새로운 알림이 있습니다.", {
            duration: null,
          });
      }
    },
    [
      addNotification,
      updateNotificationStats,
      handleTodoReminder,
      handleRoutineReminder,
      handleAchievement,
      showInfo,
    ],
  );

  const setupEventSourceHandlers = useCallback(
    (eventSource) => {
      eventSource.onopen = function () {
        updateConnectionStatus(CONNECTION_STATUS.CONNECTED);

        if (retryCountRef.current > 0) {
          showSuccess("연결이 복구되었습니다! 🔄");
        }
      };

      eventSource.addEventListener("notification", function (event) {
        try {
          const notification = JSON.parse(event.data);
          handleNotification(notification);
        } catch {
          showError("알림 처리 중 오류가 발생했습니다.");
        }
      });

      eventSource.addEventListener("heartbeat", function () {
        setLastConnectedAt(Date.now());
      });

      eventSource.onerror = function () {
        if (eventSource.readyState === EventSource.CLOSED) {
          updateConnectionStatus(CONNECTION_STATUS.DISCONNECTED);

          if (
            retryCountRef.current < maxRetries &&
            !isManuallyClosedRef.current
          ) {
            const delay = getRetryDelay(retryCountRef.current);
            retryCountRef.current++;

            updateConnectionStatus(
              CONNECTION_STATUS.RECONNECTING,
              `연결 재시도 중... (${retryCountRef.current}/${maxRetries})`,
            );

            reconnectTimeoutRef.current = setTimeout(() => {
              connectSSERef.current?.();
            }, delay);
          } else if (!isManuallyClosedRef.current) {
            updateConnectionStatus(
              CONNECTION_STATUS.FAILED,
              "연결에 실패했습니다. 페이지를 새로고침해주세요.",
            );
          }
        }
      };

      eventSource.onclose = function () {
        if (!isManuallyClosedRef.current) {
          updateConnectionStatus(CONNECTION_STATUS.DISCONNECTED);
        }
      };
    },
    [handleNotification, showError, showSuccess],
  );

  const connectSSE = useCallback(() => {
    if (!user?.bifId) {
      return;
    }

    if (isManuallyClosedRef.current) {
      return;
    }

    if (!navigator.onLine) {
      updateConnectionStatus(
        CONNECTION_STATUS.FAILED,
        "네트워크 연결을 확인해주세요.",
      );
      return;
    }

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    try {
      updateConnectionStatus(CONNECTION_STATUS.CONNECTING);

      const { accessToken } = useUserStore.getState();
      if (accessToken) {
        document.cookie =
          "authenticatedUserToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT";

        const isProduction = window.location.protocol === "https:";
        const cookieOptions = [
          `authenticatedUserToken=${accessToken}`,
          "Path=/",
          "SameSite=Strict",
          ...(isProduction ? ["Secure"] : []),
        ].join("; ");

        document.cookie = cookieOptions;
      }

      const url = `${baseUrl}/api/notifications/sse/subscribe`;

      const eventSource = new EventSource(url, {
        withCredentials: true,
      });

      eventSourceRef.current = eventSource;
      setupEventSourceHandlers(eventSource);
    } catch {
      updateConnectionStatus(
        CONNECTION_STATUS.FAILED,
        "연결 생성에 실패했습니다.",
      );
    }
  }, [user?.bifId, baseUrl, setupEventSourceHandlers]);

  useEffect(() => {
    connectSSERef.current = connectSSE;
  }, [connectSSE]);

  const disconnect = useCallback(() => {
    isManuallyClosedRef.current = true;

    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    document.cookie =
      "authenticatedUserToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Strict";

    updateConnectionStatus(CONNECTION_STATUS.DISCONNECTED);
    setNotificationStats({
      totalReceived: 0,
      todoReminders: 0,
      routineReminders: 0,
    });
  }, []);

  const reconnect = useCallback(() => {
    isManuallyClosedRef.current = false;
    retryCountRef.current = 0;

    disconnect();

    setTimeout(() => {
      connectSSERef.current?.();
    }, 1000);
  }, [disconnect]);

  const handleOnline = useCallback(() => {
    if (connectionStatus !== CONNECTION_STATUS.CONNECTED && user?.bifId) {
      retryCountRef.current = 0;
      isManuallyClosedRef.current = false;
      connectSSERef.current?.();
    }
  }, [connectionStatus, user?.bifId]);

  const handleOffline = useCallback(() => {
    updateConnectionStatus(
      CONNECTION_STATUS.FAILED,
      "네트워크 연결이 끊어졌습니다.",
    );
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  }, []);

  const handleVisibilityChange = useCallback(() => {
    if (document.visibilityState === "visible") {
      if (connectionStatus === CONNECTION_STATUS.DISCONNECTED && user?.bifId) {
        connectSSERef.current?.();
      }
    }
  }, [connectionStatus, user?.bifId]);

  function getConnectionInfo() {
    return {
      status: connectionStatus,
      isConnected: connectionStatus === CONNECTION_STATUS.CONNECTED,
      isConnecting: connectionStatus === CONNECTION_STATUS.CONNECTING,
      isReconnecting: connectionStatus === CONNECTION_STATUS.RECONNECTING,
      isFailed: connectionStatus === CONNECTION_STATUS.FAILED,
      error: connectionError,
      lastConnectedAt,
      retryCount: retryCountRef.current,
      maxRetries,
      stats: notificationStats,
    };
  }

  useEffect(() => {
    if (!user?.bifId) {
      return;
    }

    isManuallyClosedRef.current = false;
    connectSSERef.current?.();

    return () => {
      disconnect();
    };
  }, [user?.bifId, disconnect]);

  useEffect(() => {
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);

    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, [handleOnline, handleOffline]);

  useEffect(() => {
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [handleVisibilityChange]);

  useEffect(() => {
    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  return {
    ...getConnectionInfo(),
    disconnect,
    reconnect,
    getConnectionInfo,
    isConnected: connectionStatus === CONNECTION_STATUS.CONNECTED,
    connectionError,
  };
}

export const NotificationHelpers = {
  TYPES: NOTIFICATION_TYPES,
  STATUS: CONNECTION_STATUS,

  isTodoReminder: (type) => type === NOTIFICATION_TYPES.TODO_REMINDER,
  isRoutineReminder: (type) => type === NOTIFICATION_TYPES.ROUTINE_REMINDER,
  isAchievement: (type) => type === NOTIFICATION_TYPES.ACHIEVEMENT,

  isConnected: (status) => status === CONNECTION_STATUS.CONNECTED,
  isConnecting: (status) => status === CONNECTION_STATUS.CONNECTING,
  isReconnecting: (status) => status === CONNECTION_STATUS.RECONNECTING,
  isFailed: (status) => status === CONNECTION_STATUS.FAILED,
};
