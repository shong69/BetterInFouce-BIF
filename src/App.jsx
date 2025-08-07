import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import Todo from "@pages/Todo";
import TodoDetail from "@pages/TodoDetail";
import Login from "@pages/Login";
import Diary from "@pages/Diary";
import Profile from "@pages/Profile";
import Simulation from "@pages/Simulation";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import ToastNotification from "@components/ui/ToastNotification";
import CreateTodo from "@pages/CreateTodo";
import EditTodo from "@pages/EditTodo";
import ProtectedRoute from "@components/auth/ProtectedRoute";
import { useEffect } from "react";
import { useAuth } from "./hooks/useAuth";

function App() {
  const { registrationInfo, isLoading, checkAuthStatus } = useAuth();
  const location = useLocation();

  useEffect(() => {
    checkAuthStatus();
  }, [checkAuthStatus]);

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (registrationInfo && !location.pathname.startsWith("/login")) {
    return <Navigate to="/login/select-role" replace />;
  }

  return (
    <>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/login/select-role" element={<LoginSelectRole />} />
          <Route path="/login/invite-code" element={<LoginInviteCode />} />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Todo />
              </ProtectedRoute>
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
          <Route path="/diaries" element={<Diary />} />
          <Route path="/simulations" element={<Simulation />} />
          <Route path="/bif-profile" element={<Profile />} />
        </Routes>
      </BrowserRouter>

      <LoadingSpinner />
      <ToastNotification />
    </>
  );
}

export default App;
