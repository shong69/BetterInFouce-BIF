import { PiBookOpenText } from "react-icons/pi";
import { IoBook } from "react-icons/io5";
import { BiBarChartAlt2 } from "react-icons/bi";
import { BsFileText } from "react-icons/bs";
import { useLocation } from "react-router-dom";
import { useUserStore } from "@stores";
import { Link } from "react-router-dom";

export default function TabBar() {
  const location = useLocation();
  const currentPath = location.pathname;
  const { user } = useUserStore();

  const tabs = [
    user?.userRole === "GUARDIAN"
      ? {
          path: "/guardian-stats",
          icon: IoBook,
          isActive: function (path) {
            return path === "/guardian-stats";
          },
        }
      : {
          path: "/diaries",
          icon: PiBookOpenText,
          isActive: function (path) {
            return path === "/diaries" || path.startsWith("/diaries");
          },
        },
    {
      path: "/",
      icon: BsFileText,
      isActive: function (path) {
        return path === "/" || path.startsWith("/todo");
      },
    },
    {
      path: "/simulations",
      icon: BiBarChartAlt2,
      isActive: function (path) {
        return path === "/simulations" || path.startsWith("/simulation");
      },
    },
  ];

  return (
    <div className="fixed right-0 bottom-0 left-0 z-40 p-4">
      <div className="mx-auto max-w-md">
        <div className="flex overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-lg">
          {tabs.map(function (tab, index) {
            const isActive = tab.isActive(currentPath);
            const IconComponent = tab.icon;

            return (
              <Link
                key={tab.path}
                to={tab.path}
                className={`flex flex-1 items-center justify-center py-4 transition-all duration-300 hover:bg-gray-50 ${
                  index === 0 ? "rounded-l-2xl" : ""
                } ${index === tabs.length - 1 ? "rounded-r-2xl" : ""}`}
              >
                <IconComponent
                  size={20}
                  className={`transition-colors duration-200 ${
                    isActive ? "text-green-500" : "text-gray-400"
                  }`}
                />
              </Link>
            );
          })}
        </div>
      </div>
    </div>
  );
}
