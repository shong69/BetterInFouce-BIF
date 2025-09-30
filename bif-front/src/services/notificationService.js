class NotificationService {
  constructor() {
    this.swRegistration = null;
    this.isSupported = "serviceWorker" in navigator && "PushManager" in window;
  }

  async requestNotificationPermission() {
    if (!("Notification" in window)) {
      return false;
    }

    if (Notification.permission === "granted") {
      return true;
    }

    const permission = await Notification.requestPermission();
    return permission === "granted";
  }

  async registerServiceWorker() {
    if (!this.isSupported) {
      return null;
    }

    try {
      const registration = await navigator.serviceWorker.register("/sw.js");

      if (registration.installing) {
        await new Promise((resolve) => {
          registration.installing.addEventListener("statechange", function () {
            if (this.state === "activated") {
              resolve();
            }
          });
        });
      } else if (!registration.active) {
        await navigator.serviceWorker.ready;
      }

      this.swRegistration = registration;
      return registration;
    } catch {
      return null;
    }
  }

  async subscribeToWebPush() {
    if (!this.swRegistration) {
      await this.registerServiceWorker();
    }

    if (!this.swRegistration) {
      throw new Error("Service Worker 등록이 필요합니다.");
    }

    const vapidPublicKey = import.meta.env.VITE_VAPID_PUBLIC_KEY;

    if (!vapidPublicKey) {
      throw new Error("VAPID 공개 키가 설정되지 않았습니다.");
    }

    const subscription = await this.swRegistration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: this.urlBase64ToUint8Array(vapidPublicKey),
    });

    await this.sendSubscriptionToServer(subscription);

    return subscription;
  }

  async sendSubscriptionToServer(subscription) {
    const { default: api } = await import("./api.js");

    await api.post("/api/notifications/web-push/subscribe", {
      endpoint: subscription.endpoint,
      keys: {
        p256dh: this.arrayBufferToBase64(subscription.getKey("p256dh")),
        auth: this.arrayBufferToBase64(subscription.getKey("auth")),
      },
    });
  }

  async unsubscribeFromWebPush(endpoint) {
    const { default: api } = await import("./api.js");

    await api.delete(
      `/api/notifications/web-push/unsubscribe?endpoint=${encodeURIComponent(endpoint)}`,
    );
  }

  urlBase64ToUint8Array(base64String) {
    const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding)
      .replace(/-/g, "+")
      .replace(/_/g, "/");

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }

  arrayBufferToBase64(buffer) {
    const bytes = new Uint8Array(buffer);
    const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join(
      "",
    );
    return btoa(binary);
  }
}

export default new NotificationService();
