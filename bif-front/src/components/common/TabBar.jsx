import { GoTasklist } from "react-icons/go";
import { VscSettings } from "react-icons/vsc";
import { PiBookOpenText } from "react-icons/pi";
import { IoBook } from "react-icons/io5";
import IconBox from "@components/ui/IconBox";
import { useLocation } from "react-router-dom";
import { useUserStore } from "@stores";

export default function TabBar() {
  const location = useLocation();
  const currentPath = location.pathname;
  const { user } = useUserStore();

  const tabs = [
    user?.userRole === "GUARDIAN"
      ? {
          path: "/guardian-stats",
          icon: IoBook,
          title: "통계 보기",
          isActive: function (path) {
            return path === "/guardian-stats";
          },
        }
      : {
          path: "/diaries",
          icon: PiBookOpenText,
          title: "감정 일기",
          isActive: function (path) {
            return path === "/diaries" || path.startsWith("/diaries");
          },
        },
    {
      path: "/",
      icon: GoTasklist,
      title: "할 일",
      isActive: function (path) {
        return path === "/" || path.startsWith("/todo");
      },
    },
    {
      path: "/simulations",
      icon: VscSettings,
      title: "시뮬레이션",
      isActive: function (path) {
        return path === "/simulations" || path.startsWith("/simulation");
      },
    },
  ];

  return (
    <div className="fixed bottom-4 left-1/2 z-40 flex w-full -translate-x-1/2 transform justify-center">
      <div className="w-full max-w-4xl px-4">
        <div className="rounded-full border border-gray-200/50 bg-white/90 px-8 py-1.5 shadow-lg backdrop-blur-sm">
          <div className="flex items-center justify-between">
            {tabs.map(function (tab) {
              const isActive = tab.isActive(currentPath);
              const IconComponent = tab.icon;

              return (
                <IconBox
                  key={tab.path}
                  to={tab.path}
                  icon={
                    <IconComponent
                      size={28}
                      className={`${
                        isActive
                          ? "text-primary"
                          : "text-gray-400 group-hover:text-gray-600"
                      }`}
                    />
                  }
                  title=""
                  titleStyle={`${isActive ? "text-primary font-medium" : ""}`}
                  isActive={isActive}
                />
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
