const NOTIFICATION_TYPES = {
  TODO_REMINDER: "todo-reminder",
  ROUTINE_REMINDER: "routine-reminder",
  ACHIEVEMENT: "achievement",
  SYSTEM: "system",
};

const API_BASE_URL =
  "https://bif-backend-mino.wittysky-992c3ba5.koreacentral.azurecontainerapps.io";
const DEFAULT_HEADERS = {
  "Content-Type": "application/json",
};

const DEFAULT_NOTIFICATION_OPTIONS = {
  icon: "/logo.png",
  badge: "/logo.png",
  requireInteraction: false,
  silent: false,
};

self.addEventListener("push", function (event) {
  let notificationData = {
    title: "BIF 알림",
    body: "새로운 알림이 있습니다.",
    type: NOTIFICATION_TYPES.SYSTEM,
  };

  if (event.data) {
    try {
      notificationData = event.data.json();
    } catch {
      notificationData.body = event.data.text() || notificationData.body;
    }
  }

  const options = createNotificationOptions(notificationData);

  event.waitUntil(
    self.registration.showNotification(
      notificationData.title || "BIF 알림",
      options,
    ),
  );
});

function createNotificationOptions(data) {
  const baseOptions = {
    ...DEFAULT_NOTIFICATION_OPTIONS,
    data: data,
    timestamp: Date.now(),
  };

  switch (data.type) {
    case NOTIFICATION_TYPES.TODO_REMINDER:
      return {
        ...baseOptions,
        body: `📋 ${data.body}`,
        tag: "todo-reminder",
        requireInteraction: true,
        actions: [
          {
            action: "complete",
            title: "완료 처리",
          },
          {
            action: "snooze",
            title: "10분 후 알림",
          },
          {
            action: "open",
            title: "할 일 보기",
          },
        ],
        vibrate: [200, 100, 200],
        renotify: true,
      };

    case NOTIFICATION_TYPES.ROUTINE_REMINDER:
      return {
        ...baseOptions,
        body: `🔄 루틴: ${data.body}`,
        tag: "routine-reminder",
        requireInteraction: true,
        actions: [
          {
            action: "complete",
            title: "완료",
          },
          {
            action: "skip",
            title: "오늘 건너뛰기",
          },
          {
            action: "postpone",
            title: "1시간 후",
          },
        ],
        vibrate: [100, 50, 100, 50, 100],
      };

    case NOTIFICATION_TYPES.ACHIEVEMENT:
      return {
        ...baseOptions,
        body: `🎉 ${data.body}`,
        tag: "achievement",
        requireInteraction: false,
        actions: [
          {
            action: "view",
            title: "확인하기",
          },
        ],
        vibrate: [200, 100, 200, 100, 200],
      };

    case NOTIFICATION_TYPES.SYSTEM:
    default:
      return {
        ...baseOptions,
        body: data.body || "새로운 알림이 있습니다.",
        tag: "system",
        actions: [
          {
            action: "open",
            title: "확인",
          },
        ],
      };
  }
}

self.addEventListener("notificationclick", function (event) {
  event.notification.close();

  const data = event.notification.data || {};
  const action = event.action;

  switch (action) {
    case "complete":
      event.waitUntil(handleTodoComplete(data));
      break;

    case "snooze":
      event.waitUntil(handleSnooze(data, 10));
      break;

    case "skip":
      event.waitUntil(handleTodoSkip(data));
      break;

    case "postpone":
      event.waitUntil(handlePostpone(data, 60));
      break;

    case "view":
      event.waitUntil(openPage("/achievements"));
      break;

    case "open":
    default: {
      const url = getUrlByNotificationType(data.type, data);
      event.waitUntil(openPage(url));
      break;
    }
  }
});

function getUrlByNotificationType(type, data) {
  switch (type) {
    case NOTIFICATION_TYPES.TODO_REMINDER:
      return data.todoId ? `/todo/${data.todoId}` : "/todo";
    case NOTIFICATION_TYPES.ROUTINE_REMINDER:
      return data.todoId ? `/todo/${data.todoId}` : "/todo?filter=routine";
    case NOTIFICATION_TYPES.ACHIEVEMENT:
      return "/achievements";
    default:
      return "/";
  }
}

async function openPage(url) {
  const clients = await self.clients.matchAll({
    type: "window",
    includeUncontrolled: true,
  });

  for (const client of clients) {
    if (client.url.includes(self.location.origin)) {
      await client.focus();
      await client.navigate(url);
      return;
    }
  }

  return self.clients.openWindow(url);
}

async function makeApiRequest(url, options = {}) {
  const token = await getStoredToken();

  const requestOptions = {
    ...options,
    headers: {
      ...DEFAULT_HEADERS,
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
  };

  try {
    const response = await fetch(`${API_BASE_URL}${url}`, requestOptions);

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error("인증이 필요합니다.");
      }
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response;
  } catch (error) {
    if (error.name === "TypeError" && error.message.includes("fetch")) {
      throw new Error("네트워크 연결을 확인해주세요.");
    }
    throw error;
  }
}

async function showResultNotification(success, successMsg, errorMsg) {
  if (success) {
    await self.registration.showNotification("성공", {
      body: successMsg,
      icon: "/logo.png",
      tag: "action-success",
      requireInteraction: false,
      vibrate: [100, 50, 100],
    });
  } else {
    await self.registration.showNotification("오류", {
      body: errorMsg,
      icon: "/logo.png",
      tag: "action-error",
      requireInteraction: false,
    });
  }
}

async function handleTodoComplete(data) {
  if (!data.todoId) {
    await showResultNotification(false, "", "todoId가 없습니다.");
    return;
  }

  try {
    await makeApiRequest(`/todos/${data.todoId}/complete`, {
      method: "PATCH",
    });

    await showResultNotification(true, "할 일이 완료되었습니다. 🎉", "");
  } catch (error) {
    if (error.message.includes("네트워크")) {
      await saveOfflineAction("complete", { todoId: data.todoId });
      await showResultNotification(
        true,
        "오프라인 상태입니다. 온라인이 되면 자동으로 처리됩니다.",
        "",
      );
    } else {
      await showResultNotification(
        false,
        "",
        error.message || "완료 처리 중 오류가 발생했습니다.",
      );
    }
  }
}

async function handleSnooze(data, minutes) {
  if (!data.todoId) {
    await showResultNotification(false, "", "todoId가 없습니다.");
    return;
  }

  try {
    await makeApiRequest(`/todos/${data.todoId}/snooze`, {
      method: "PATCH",
      body: JSON.stringify({ minutes }),
    });

    await showResultNotification(
      true,
      `${minutes}분 후에 다시 알려드릴게요.`,
      "",
    );
  } catch (error) {
    if (error.message.includes("네트워크")) {
      await saveOfflineAction("snooze", { todoId: data.todoId, minutes });
      await showResultNotification(
        true,
        "오프라인 상태입니다. 온라인이 되면 자동으로 처리됩니다.",
        "",
      );
    } else {
      await showResultNotification(
        false,
        "",
        error.message || "스누즈 처리 중 오류가 발생했습니다.",
      );
    }
  }
}

async function handleTodoSkip(data) {
  if (!data.todoId) {
    await showResultNotification(false, "", "todoId가 없습니다.");
    return;
  }

  try {
    await makeApiRequest(`/todos/${data.todoId}/skip-today`, {
      method: "PATCH",
    });

    await showResultNotification(true, "오늘은 건너뛰기로 했어요.", "");
  } catch (error) {
    if (error.message.includes("네트워크")) {
      await saveOfflineAction("skip", { todoId: data.todoId });
      await showResultNotification(
        true,
        "오프라인 상태입니다. 온라인이 되면 자동으로 처리됩니다.",
        "",
      );
    } else {
      await showResultNotification(
        false,
        "",
        error.message || "건너뛰기 처리 중 오류가 발생했습니다.",
      );
    }
  }
}

async function handlePostpone(data, minutes) {
  if (!data.todoId) {
    await showResultNotification(false, "", "todoId가 없습니다.");
    return;
  }

  try {
    await makeApiRequest(`/todos/${data.todoId}/postpone`, {
      method: "PATCH",
      body: JSON.stringify({ minutes }),
    });

    await showResultNotification(true, `${minutes}분 후로 연기되었습니다.`, "");
  } catch (error) {
    if (error.message.includes("네트워크")) {
      await saveOfflineAction("postpone", { todoId: data.todoId, minutes });
      await showResultNotification(
        true,
        "오프라인 상태입니다. 온라인이 되면 자동으로 처리됩니다.",
        "",
      );
    } else {
      await showResultNotification(
        false,
        "",
        error.message || "연기 처리 중 오류가 발생했습니다.",
      );
    }
  }
}

async function getStoredToken() {
  try {
    const db = await openIndexedDB();
    const transaction = db.transaction(["auth"], "readonly");
    const store = transaction.objectStore("auth");
    const result = await new Promise((resolve, reject) => {
      const request = store.get("accessToken");
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    return result?.value;
  } catch {
    return null;
  }
}

async function openIndexedDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open("BifAppDB", 2);

    request.onupgradeneeded = (event) => {
      const db = event.target.result;

      if (!db.objectStoreNames.contains("auth")) {
        db.createObjectStore("auth");
      }

      if (!db.objectStoreNames.contains("offline_actions")) {
        db.createObjectStore("offline_actions", { keyPath: "id" });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

self.addEventListener("install", function () {
  self.skipWaiting();
});

self.addEventListener("activate", function (event) {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("sync", function (event) {
  if (event.tag === "background-sync") {
    event.waitUntil(doBackgroundSync());
  }
});

async function saveOfflineAction(action, data) {
  const db = await openIndexedDB();
  const transaction = db.transaction(["offline_actions"], "readwrite");
  const store = transaction.objectStore("offline_actions");

  const actionData = {
    id: Date.now() + Math.random(),
    action,
    data,
    timestamp: Date.now(),
    retryCount: 0,
  };

  await new Promise((resolve, reject) => {
    const request = store.add(actionData);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

  await self.registration.sync.register("background-sync");
}

async function doBackgroundSync() {
  const db = await openIndexedDB();
  const transaction = db.transaction(["offline_actions"], "readwrite");
  const store = transaction.objectStore("offline_actions");

  const actions = await new Promise((resolve, reject) => {
    const request = store.getAll();
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

  for (const actionItem of actions) {
    try {
      await processOfflineAction(actionItem);

      await new Promise((resolve, reject) => {
        const deleteRequest = store.delete(actionItem.id);
        deleteRequest.onsuccess = () => resolve();
        deleteRequest.onerror = () => reject(deleteRequest.error);
      });
    } catch {
      actionItem.retryCount = (actionItem.retryCount || 0) + 1;

      if (actionItem.retryCount >= 3) {
        await new Promise((resolve, reject) => {
          const deleteRequest = store.delete(actionItem.id);
          deleteRequest.onsuccess = () => resolve();
          deleteRequest.onerror = () => reject(deleteRequest.error);
        });
      } else {
        await new Promise((resolve, reject) => {
          const updateRequest = store.put(actionItem);
          updateRequest.onsuccess = () => resolve();
          updateRequest.onerror = () => reject(updateRequest.error);
        });
      }
    }
  }
}

async function processOfflineAction(actionItem) {
  const { action, data } = actionItem;

  switch (action) {
    case "complete":
      await makeApiRequest(`/todos/${data.todoId}/complete`, {
        method: "PATCH",
      });
      break;

    case "snooze":
      await makeApiRequest(`/todos/${data.todoId}/snooze`, {
        method: "PATCH",
        body: JSON.stringify({ minutes: data.minutes }),
      });
      break;

    case "skip":
      await makeApiRequest(`/todos/${data.todoId}/skip-today`, {
        method: "PATCH",
      });
      break;

    case "postpone":
      await makeApiRequest(`/todos/${data.todoId}/postpone`, {
        method: "PATCH",
        body: JSON.stringify({ minutes: data.minutes }),
      });
      break;

    default:
      throw new Error(`알 수 없는 액션: ${action}`);
  }
}

self.addEventListener("message", function (event) {
  if (event.data && event.data.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});
