import { useEffect, useState } from "react";
import { useToastStore } from "@stores/toastStore";
import notificationService from "@services/notificationService";

export default function NotificationPermissionHandler() {
  const { showInfo, showWarning, showError } = useToastStore();
  const [hasRequestedPermission, setHasRequestedPermission] = useState(false);

  useEffect(() => {
    const initializeNotifications = async () => {
      if (hasRequestedPermission) return;

      if (!notificationService.isSupported) {
        return;
      }

      try {
        const registration = await notificationService.registerServiceWorker();

        const hasPermission =
          await notificationService.requestNotificationPermission();

        if (hasPermission) {
          if (registration) {
            await notificationService.subscribeToWebPush().catch(() => {});
          }
        } else {
          showWarning(
            "알림 권한이 필요합니다. 브라우저 설정에서 알림을 허용해주세요.",
          );
        }

        setHasRequestedPermission(true);
      } catch {
        showError("알림 설정 중 오류가 발생했습니다.");
        setHasRequestedPermission(true);
      }
    };

    const timer = setTimeout(initializeNotifications, 2000);

    return () => clearTimeout(timer);
  }, [hasRequestedPermission, showInfo, showWarning, showError]);

  return null;
}
