import { Navigate } from "react-router-dom";
import { useUserStore } from "@stores";

export default function ProtectedRoute({ children }) {
  const { isAuthenticated } = useUserStore();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
