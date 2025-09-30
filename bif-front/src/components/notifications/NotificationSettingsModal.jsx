import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Modal from "@components/ui/Modal";
import {
  IoNotificationsOutline,
  IoCheckmarkCircle,
  IoCloseCircle,
  IoTrashOutline,
  IoCheckmarkDoneOutline,
  IoTimeOutline,
} from "react-icons/io5";
import notificationService from "@services/notificationService";
import { useToastStore, useNotificationStore } from "@stores";

export default function NotificationSettingsModal({ isOpen, onClose }) {
  const navigate = useNavigate();
  const [notificationStatus, setNotificationStatus] = useState("checking");
  const [webPushStatus, setWebPushStatus] = useState("checking");
  const [isSettingUp, setIsSettingUp] = useState(false);
  const [setupStep, setSetupStep] = useState("");
  const { showSuccess, showError } = useToastStore();
  const {
    notifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    clearAllNotifications,
    getStats,
    initialize,
  } = useNotificationStore();

  const stats = getStats();

  useEffect(() => {
    if (isOpen) {
      initialize();
      checkNotificationStatus();
    }
  }, [isOpen, initialize]);

  async function checkNotificationStatus() {
    try {
      const permission = Notification.permission;
      setNotificationStatus(permission);

      if (permission === "granted") {
        const registration = await navigator.serviceWorker.getRegistration();
        if (registration) {
          const subscription = await registration.pushManager.getSubscription();
          setWebPushStatus(subscription ? "subscribed" : "not-subscribed");
        } else {
          setWebPushStatus("no-service-worker");
        }
      } else {
        setWebPushStatus("permission-denied");
      }
    } catch {
      setNotificationStatus("error");
      setWebPushStatus("error");
    }
  }

  async function requestNotificationPermission() {
    try {
      const currentPermission = Notification.permission;

      if (currentPermission === "denied") {
        showError("알림 차단됨. 설정에서 허용해주세요.");
        return;
      }

      setIsSettingUp(true);

      if (currentPermission === "granted") {
        setSetupStep("웹 푸시 설정 중...");
        const registration = await navigator.serviceWorker.getRegistration();
        if (registration) {
          const subscription = await registration.pushManager.getSubscription();
          if (!subscription) {
            try {
              await notificationService.subscribeToWebPush();
              showSuccess("알림이 설정되었습니다!");
            } catch {
              showError("설정 실패");
            }
          }
        }
        checkNotificationStatus();
        setIsSettingUp(false);
        setSetupStep("");
        return;
      }

      setSetupStep("알림 권한 요청 중...");
      const hasPermission =
        await notificationService.requestNotificationPermission();

      if (hasPermission) {
        setSetupStep("웹 푸시 설정 중...");
        const registration = await notificationService.registerServiceWorker();
        if (registration) {
          try {
            await notificationService.subscribeToWebPush();
            showSuccess("알림이 설정되었습니다!");
          } catch {
            showError("설정 실패");
          }
        }
        checkNotificationStatus();
      } else {
        showError("알림 거부됨. 설정에서 변경하세요.");
      }
    } catch {
      showError("설정 실패");
    } finally {
      setIsSettingUp(false);
      setSetupStep("");
    }
  }

  function handleNotificationClick(notification) {
    if (!notification.read) {
      markAsRead(notification.id);
    }

    if (notification.todoId) {
      onClose();
      navigate(`/todo/${notification.todoId}`);
    }
  }

  function handleNotificationKeyDown(event, notification) {
    if (event.key === "Enter" || event.key === " ") {
      handleNotificationClick(notification);
    }
  }

  function getRelativeTime(dateString) {
    const now = new Date();
    const date = new Date(dateString);
    const diffInMs = now - date;
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInMinutes < 1) {
      return "방금 전";
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes}분 전`;
    } else if (diffInHours < 24) {
      return `${diffInHours}시간 전`;
    } else if (diffInDays < 7) {
      return `${diffInDays}일 전`;
    } else {
      return date.toLocaleDateString();
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="flex h-[500px] flex-col text-center">
        <div className="mb-2 flex justify-center">
          <IoNotificationsOutline size={40} className="text-secondary" />
        </div>

        <h2 className="mb-4 text-lg font-bold">알림</h2>

        <div className="mb-6 text-left">
          <div
            className={`flex items-center justify-between rounded-lg border p-3 ${
              notificationStatus === "granted" && webPushStatus === "subscribed"
                ? "border-green-300 bg-green-50"
                : "border-gray-300 bg-gray-50"
            }`}
          >
            <div className="flex items-center gap-2">
              {isSettingUp ? (
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600" />
              ) : notificationStatus === "granted" &&
                webPushStatus === "subscribed" ? (
                <IoCheckmarkCircle className="text-primary" size={16} />
              ) : (
                <IoCloseCircle className="text-gray-400" size={16} />
              )}
              <div>
                <h3 className="text-sm font-medium text-gray-800">
                  백그라운드 알림
                </h3>
                <p className="text-xs text-gray-600">
                  {isSettingUp
                    ? setupStep
                    : notificationStatus === "granted" &&
                        webPushStatus === "subscribed"
                      ? "앱이 꺼져도 알림 받기"
                      : "앱이 꺼져도 알림 받으려면 설정"}
                </p>
              </div>
            </div>
            <div>
              {notificationStatus === "granted" &&
              webPushStatus === "subscribed" ? (
                <span className="text-primary text-xs font-medium">✓ 완료</span>
              ) : (
                <button
                  onClick={requestNotificationPermission}
                  disabled={isSettingUp}
                  className="bg-primary hover:bg-secondary rounded px-2 py-1 text-xs text-white disabled:opacity-50"
                >
                  {isSettingUp
                    ? "설정 중..."
                    : notificationStatus === "denied"
                      ? "차단 해제"
                      : "설정하기"}
                </button>
              )}
            </div>
          </div>

          {notificationStatus === "denied" && !isSettingUp && (
            <div className="mt-2 rounded border border-yellow-200 bg-yellow-50 p-2 text-xs text-gray-500">
              💡 알림이 차단된 경우 설정 앱에서 직접 허용해주세요
            </div>
          )}
        </div>

        <div className="flex min-h-0 flex-1 flex-col space-y-3 text-left">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h3 className="text-xs font-medium">알림 히스토리</h3>
              <span className="rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-600">
                {stats.total}개
              </span>
            </div>
            {notifications.length > 0 && (
              <div className="flex items-center gap-1">
                <button
                  onClick={markAllAsRead}
                  className="bg-secondary text-tiny flex items-center gap-1 rounded px-2 py-1 text-white hover:bg-blue-600"
                >
                  <IoCheckmarkDoneOutline size={10} />
                  전체 읽음
                </button>
                <button
                  onClick={clearAllNotifications}
                  className="bg-warning text-tiny flex items-center gap-1 rounded px-2 py-1 text-white hover:bg-red-600"
                >
                  <IoTrashOutline size={10} />
                  전체 삭제
                </button>
              </div>
            )}
          </div>

          <div className="flex-1 space-y-2 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="py-6 text-center text-gray-500">
                <IoNotificationsOutline
                  size={24}
                  className="mx-auto mb-2 opacity-50"
                />
                <p className="text-sm">받은 알림이 없습니다</p>
              </div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  role="button"
                  tabIndex={0}
                  className={`w-full cursor-pointer rounded-lg border p-2 text-left transition-colors hover:bg-gray-100 ${
                    notification.read
                      ? "border-gray-100 bg-gray-50"
                      : "border-gray-300 bg-white shadow-sm"
                  }`}
                  onClick={() => handleNotificationClick(notification)}
                  onKeyDown={(e) => handleNotificationKeyDown(e, notification)}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0 flex-1">
                      <p className="text-xs break-words text-gray-600">
                        {notification.body}
                      </p>
                      <div className="mt-1 flex items-center gap-2 text-xs text-gray-500">
                        <IoTimeOutline size={10} />
                        <span className="text-xs">
                          {getRelativeTime(notification.receivedAt)}
                        </span>
                        {!notification.read && (
                          <span className="bg-primary h-1.5 w-1.5 rounded-full" />
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-0.5">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          deleteNotification(notification.id);
                        }}
                        className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-red-600"
                      >
                        <IoTrashOutline size={17} />
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <button
          onClick={onClose}
          className="bg-primary hover:bg-secondary mt-4 w-full rounded-3xl py-2 text-white"
        >
          닫기
        </button>
      </div>
    </Modal>
  );
}
