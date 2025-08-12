import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useEffect } from "react";
import { useUserStore } from "@stores";

import Todo from "@pages/Todo";
import Login from "@pages/user/Login";
import LoginSelectRole from "@pages/user/LoginSelectRole";
import LoginInviteCode from "@pages/user/LoginInviteCode";
import Profile from "@pages/Profile";

import Diary from "@pages/diaries/Diary";
import DiaryCreate from "@pages/diaries/DiaryCreate";
import DiaryView from "@pages/diaries/DiaryView";
import DiaryEdit from "@pages/diaries/DiaryEdit";

import SimulationProgress from "@pages/simulation/SimulationProgress";
import Simulation from "@pages/simulation/Simulation";

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
            path="/diaries/create"
            element={
              <ProtectedRoute>
                <DiaryCreate />
              </ProtectedRoute>
            }
          />
          <Route
            path="/diaries/:id"
            element={
              <ProtectedRoute>
                <DiaryView />
              </ProtectedRoute>
            }
          />
          <Route
            path="/diaries/edit/:id"
            element={
              <ProtectedRoute>
                <DiaryEdit />
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
            path="/simulations/:id"
            element={
              <ProtectedRoute>
                <SimulationProgress />
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
