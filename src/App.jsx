import { BrowserRouter, Routes, Route } from "react-router-dom";
import Todo from "@pages/Todo";
import Login from "@pages/user/Login";
import LoginSelectRole from "@pages/user/LoginSelectRole";
import LoginInviteCode from "@pages/user/LoginInviteCode";
import Diary from "@pages/Diary";
import Profile from "@pages/Profile";
import Simulation from "@pages/Simulation";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import ToastNotification from "@components/ui/ToastNotification";

function App() {
  return (
    <>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Todo />} />
          <Route path="/login" element={<Login />} />
          <Route path="/login/select-role" element={<LoginSelectRole />} />
          <Route path="/login/invite-code" element={<LoginInviteCode />} />
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
