import { BrowserRouter, Routes, Route } from "react-router-dom";
import Todo from "@pages/Todo";
import Login from "@pages/Login";
import Diary from "@pages/Diary";
import Profile from "@pages/Profile";
import Simulation from "./pages/simulation/Simulation";
import SimulationProgress from "./pages/simulation/SimulationProgress";
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
          <Route path="/simulations" element={<Simulation />} />
          <Route path="/simulation/:id" element={<SimulationProgress />} />
          <Route path="/todo" element={<Todo />} />
          <Route path="/bif-profile" element={<Profile />} />
        </Routes>
      </BrowserRouter>

      <LoadingSpinner />
      <ToastNotification />
    </>
  );
}

export default App;
