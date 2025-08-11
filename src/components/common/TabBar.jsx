import { GoTasklist } from "react-icons/go";
import { VscSettings } from "react-icons/vsc";
import { PiBookOpenText } from "react-icons/pi";
import IconBox from "@components/ui/IconBox";
import { useLocation } from "react-router-dom";

export default function TabBar() {
  const location = useLocation();
  const currentPath = location.pathname;

  const tabs = [
    {
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
        return path === "/" || path.startsWith("/todos");
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
    <div className="fixed right-0 bottom-0 left-0 border-t border-gray-100 bg-white shadow-lg">
      <div className="flex justify-around">
        {tabs.map(function (tab) {
          const isActive = tab.isActive(currentPath);

          const IconComponent = tab.icon;

          return (
            <IconBox
              key={tab.path}
              to={tab.path}
              icon={
                <IconComponent
                  size={30}
                  className={`${
                    isActive
                      ? "text-primary"
                      : "text-gray-400 group-hover:text-gray-600"
                  }`}
                />
              }
              title={tab.title}
              titleStyle={`${
                isActive
                  ? "text-primary font-bold"
                  : "text-gray-400 group-hover:text-gray-600"
              }`}
            />
          );
        })}
      </div>
    </div>
  );
}
