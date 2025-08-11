import { BrowserRouter, Routes, Route } from "react-router-dom";
import Todo from "@pages/Todo";
import Login from "@pages/Login";
import Diary from "@pages/diaries/Diary";
import DiaryCreate from "@pages/diaries/DiaryCreate";
import DiaryView from "@pages/diaries/DiaryView";
import DiaryEdit from "@pages/diaries/DiaryEdit";
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
          <Route path="/diaries" element={<Diary />} />
          <Route path="/diaries/create" element={<DiaryCreate />} />
          <Route path="/diaries/:id" element={<DiaryView />} />
          <Route path="/diaries/edit/:id" element={<DiaryEdit />} />
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
