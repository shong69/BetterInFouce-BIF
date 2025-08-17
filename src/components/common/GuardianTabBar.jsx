import { useNavigate, useLocation } from "react-router-dom";
import { IoBook, IoCalendar, IoBarChart } from "react-icons/io5";

export default function GuardianTabBar() {
  const navigate = useNavigate();
  const location = useLocation();

  function isActive(path) {
    if (path === "/guardian-stats" && location.pathname === "/guardian-stats")
      return true;
    if (path === "/" && location.pathname === "/") return true;
    return (
      path === "/simulations" && location.pathname.startsWith("/simulations")
    );
  }

  return (
    <div className="fixed right-0 bottom-0 left-0 z-50 border-t border-gray-200 bg-white shadow-lg">
      <div className="flex justify-around py-2">
        <button
          onClick={() => navigate("/guardian-stats")}
          className={`flex flex-col items-center transition-colors ${
            isActive("/guardian-stats")
              ? "text-green-600"
              : "text-gray-400 hover:text-gray-600"
          }`}
        >
          <div className="relative">
            <IoBook className="h-6 w-6" />
            {isActive("/guardian-stats") && (
              <div className="absolute -top-1 -left-1 h-2 w-2 rounded-sm bg-green-600" />
            )}
          </div>
          <span className="mt-1 text-xs font-medium">통계 보기</span>
        </button>

        {/* 할 일
         보호자용 할일 화면 추가하기 */}
        <button
          onClick={() => navigate("/")}
          className={`flex flex-col items-center transition-colors ${
            isActive("/")
              ? "text-green-600"
              : "text-gray-400 hover:text-gray-600"
          }`}
        >
          <IoCalendar className="h-6 w-6" />
          <span className="mt-1 text-xs">할 일</span>
        </button>

        {/* 시뮬레이션 
        보호자용 시뮬레이션 화면 추가하기 */}
        <button
          onClick={() => navigate("/simulations")}
          className={`flex flex-col items-center transition-colors ${
            isActive("/simulations")
              ? "text-green-600"
              : "text-gray-400 hover:text-gray-600"
          }`}
        >
          <IoBarChart className="h-6 w-6" />
          <span className="mt-1 text-xs">시뮬레이션</span>
        </button>
      </div>
    </div>
  );
}
