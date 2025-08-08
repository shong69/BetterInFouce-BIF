import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useEffect } from "react";
import { useUserStore } from "@stores";

import Todo from "@pages/Todo";
import Login from "@pages/user/Login";
import LoginSelectRole from "@pages/user/LoginSelectRole";
import LoginInviteCode from "@pages/user/LoginInviteCode";
import Diary from "@pages/Diary";
import Profile from "@pages/Profile";
import Simulation from "@pages/Simulation";
import ProtectedRoute from "@components/auth/ProtectedRoute";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import ToastNotification from "@components/ui/ToastNotification";

function App() {
  const { isAuthenticated, registrationInfo, isLoading, initializeAuth } =
    useUserStore();

  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <>
      <BrowserRouter>
        <Routes>
          <Route
            path="/"
            element={
              registrationInfo ? (
                <Navigate to="/login/select-role" replace />
              ) : isAuthenticated() ? (
                <Todo />
              ) : (
                <Navigate to="/login" replace />
              )
            }
          />
          <Route path="/login" element={<Login />} />
          <Route
            path="/login/select-role"
            element={
              registrationInfo ? (
                <LoginSelectRole />
              ) : (
                <Navigate to="/login" replace />
              )
            }
          />
          <Route path="/login/invite-code" element={<LoginInviteCode />} />
          <Route
            path="/login/invite-code"
            element={
              registrationInfo ? (
                <LoginInviteCode />
              ) : (
                <Navigate to="/login" replace />
              )
            }
          />
          <Route
            path="/diaries"
            element={
              <ProtectedRoute>
                <Diary />
              </ProtectedRoute>
            }
          />
          <Route
            path="/simulations"
            element={
              <ProtectedRoute>
                <Simulation />
              </ProtectedRoute>
            }
          />
          <Route
            path="/bif-profile"
            element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>

      <LoadingSpinner />
      <ToastNotification />
    </>
  );
}

export default App;
