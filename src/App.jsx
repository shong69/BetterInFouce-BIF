import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useEffect } from "react";
import { useUserStore } from "@stores";
import { useNotifications } from "@hooks/useNotifications";

import Login from "@pages/user/Login";
import LoginSelectRole from "@pages/user/LoginSelectRole";
import LoginInviteCode from "@pages/user/LoginInviteCode";

import Todo from "@pages/todo/Todo";
import EditTodo from "@pages/todo/EditTodo";
import TodoDetail from "@pages/todo/TodoDetail";
import CreateTodo from "@pages/todo/CreateTodo";

import Diary from "@pages/diaries/Diary";
import DiaryCreate from "@pages/diaries/DiaryCreate";
import DiaryView from "@pages/diaries/DiaryView";
import DiaryEdit from "@pages/diaries/DiaryEdit";

import SimulationProgress from "@pages/simulation/SimulationProgress";
import Simulation from "@pages/simulation/Simulation";

import BifProfile from "@pages/profile/BifProfile";
import GuardianProfile from "@pages/profile/GuardianProfile";
import GuardianStats from "@pages/profile/GuardianStats";

import ProtectedRoute from "@components/auth/ProtectedRoute";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import ToastNotification from "@components/ui/ToastNotification";
import NotificationPermissionHandler from "@components/notifications/NotificationPermissionHandler";

function AppContent() {
  const { isAuthenticated } = useUserStore();

  useNotifications();

  return (
    <>
      {isAuthenticated() && <NotificationPermissionHandler />}
      <LoadingSpinner />
      <ToastNotification />
    </>
  );
}

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
    <div className="min-h-screen text-black">
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
            path="/todo/:id"
            element={
              <ProtectedRoute>
                <TodoDetail />
              </ProtectedRoute>
            }
          />
          <Route
            path="/todo/new"
            element={
              <ProtectedRoute>
                <CreateTodo />
              </ProtectedRoute>
            }
          />
          <Route
            path="/todo/:id/edit"
            element={
              <ProtectedRoute>
                <EditTodo />
              </ProtectedRoute>
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
            path="/simulation/:id"
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
                <BifProfile />
              </ProtectedRoute>
            }
          />
          <Route
            path="/guardian-profile"
            element={
              <ProtectedRoute>
                <GuardianProfile />
              </ProtectedRoute>
            }
          />
          <Route
            path="/guardian-stats"
            element={
              <ProtectedRoute>
                <GuardianStats />
              </ProtectedRoute>
            }
          />
        </Routes>

        <AppContent />
      </BrowserRouter>
    </div>
  );
}

export default App;
