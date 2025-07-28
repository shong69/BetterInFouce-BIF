import { BrowserRouter, Routes, Route } from "react-router-dom";
import Todo from "@pages/Todo";
import Login from "@pages/Login";
import Diary from "@pages/Diary";
import Simulation from "@pages/Simulation";
import Profile from "@pages/Profile";

function App() {
  return (
    <>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Todo />} />
          <Route path="/login" element={<Login />} />
          <Route path="/diaries" element={<Diary />} />
          <Route path="/simulations" element={<Simulation />} />
          <Route path="/bif-profile" element={<Profile />} />
        </Routes>
      </BrowserRouter>
    </>
  );
}

export default App;
