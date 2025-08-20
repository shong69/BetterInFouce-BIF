import { Navigate, useLocation } from "react-router-dom";
import { useUserStore } from "@stores";

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, user } = useUserStore();
  const location = useLocation();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (user?.userRole === "GUARDIAN") {
    const restrictedPaths = [/^\/todo\/new$/, /^\/todo\/\d+$/];

    if (restrictedPaths.some((pattern) => pattern.test(location.pathname))) {
      return <Navigate to="/" replace />;
    }
  }

  return children;
}
