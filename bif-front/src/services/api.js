import axios from "axios";
import { useUserStore } from "@stores/userStore";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const { accessToken } = useUserStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      const tokenError = error.response.data?.errorCode;
      if (tokenError === "EXPIRED_TOKEN") {
        originalRequest._retry = true;

        try {
          const response = await axios.post(
            "/api/auth/refresh",
            {},
            {
              withCredentials: true,
              baseURL:
                import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
            },
          );

          const { accessToken } = response.data.data;

          useUserStore.getState().setAccessToken(accessToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;

          return axios(originalRequest);
        } catch (refreshError) {
          useUserStore.getState().logout();
          window.location.href = "/login";
          return Promise.reject(refreshError);
        }
      } else {
        useUserStore.getState().logout();
        window.location.href = "/login";
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  },
);

export default api;
