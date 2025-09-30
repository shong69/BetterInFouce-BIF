import { useEffect, useState } from "react";
import { useToastStore } from "@stores/toastStore";
import notificationService from "@services/notificationService";

const INITIAL_NOTIFICATION_SHOWN_KEY = "initial-notification-shown";

export default function NotificationPermissionHandler() {
  const { showInfo, showWarning, showError } = useToastStore();
  const [hasRequestedPermission, setHasRequestedPermission] = useState(false);

  useEffect(() => {
    const initializeNotifications = async () => {
      if (hasRequestedPermission) return;

      const hasShownInitialNotification = localStorage.getItem(
        INITIAL_NOTIFICATION_SHOWN_KEY,
      );
      if (hasShownInitialNotification) {
        setHasRequestedPermission(true);
        return;
      }

      if (!("Notification" in window)) {
        setHasRequestedPermission(true);
        return;
      }

      if (!("serviceWorker" in navigator)) {
        setHasRequestedPermission(true);
        return;
      }

      if (Notification.permission === "granted") {
        const registration = await notificationService.registerServiceWorker();
        if (registration) {
          await notificationService.subscribeToWebPush().catch((error) => {
            throw error;
          });
        }

        setHasRequestedPermission(true);
        return;
      }

      if (Notification.permission === "denied") {
        setHasRequestedPermission(true);
        return;
      }

      if (Notification.permission === "default") {
        const registration = await notificationService.registerServiceWorker();

        const permission = await Notification.requestPermission();

        if (permission === "granted") {
          if (registration) {
            await notificationService.subscribeToWebPush();
            showInfo("알림이 설정되었습니다! 🎉");
          }
          localStorage.setItem(INITIAL_NOTIFICATION_SHOWN_KEY, "true");
        } else if (permission === "denied") {
          localStorage.setItem(INITIAL_NOTIFICATION_SHOWN_KEY, "true");
        } else {
          localStorage.setItem(INITIAL_NOTIFICATION_SHOWN_KEY, "true");
        }
      }

      setHasRequestedPermission(true);
    };

    const timer = setTimeout(initializeNotifications, 2000);

    return () => clearTimeout(timer);
  }, [hasRequestedPermission, showInfo, showWarning, showError]);

  return null;
}
