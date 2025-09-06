import { Link, useLocation, useNavigate } from "react-router-dom";
import { CgProfile } from "react-icons/cg";
import { TbBell } from "react-icons/tb";
import { BiPlus } from "react-icons/bi";
import { FaMedal } from "react-icons/fa";
import { IoPerson } from "react-icons/io5";
import { useUserStore, useNotificationStore } from "@stores";
import { useState, useEffect } from "react";
import NotificationSettingsModal from "@components/notifications/NotificationSettingsModal";
import BackButton from "@components/ui/BackButton";
import DiaryBackButton from "@components/ui/DiaryBackButton";
import { formatDateToDisplay } from "@utils/dateUtils";

export default function Header({
  showTodoButton = false,
  rightActions = null,
  onBackClick = null,
  onBadgeClick = null,
  onEditProfileClick = null,
}) {
  const { user } = useUserStore();
  const location = useLocation();
  const navigate = useNavigate();
  const [showNotificationModal, setShowNotificationModal] = useState(false);
  const { initialize, getStats } = useNotificationStore();

  useEffect(() => {
    initialize();
  }, [initialize]);

  const stats = getStats();

  const profilePath =
    user?.userRole === "GUARDIAN" ? "/guardian-profile" : "/bif-profile";

  useEffect(() => {
    const handleOpenNotificationSettings = () => {
      setShowNotificationModal(true);
    };

    window.addEventListener(
      "openNotificationSettings",
      handleOpenNotificationSettings,
    );
    return () => {
      window.removeEventListener(
        "openNotificationSettings",
        handleOpenNotificationSettings,
      );
    };
  }, []);

  const getPageTitle = () => {
    const path = location.pathname;
    if (path === "/" || path.startsWith("/todo")) return "할 일";
    if (path.startsWith("/diaries")) return "감정 일기";
    if (path.startsWith("/simulation")) return "시뮬레이션";
    if (path.includes("profile")) return "마이페이지";
    if (path.includes("stats")) return "통계";
    return "할 일";
  };

  const isProfilePage = location.pathname.includes("profile");

  const isMainPage = () => {
    const path = location.pathname;
    const mainPages = [
      "/",
      "/diaries",
      "/simulations",
      "/guardian-stats",
      "/bif-profile",
      "/guardian-profile",
    ];
    return mainPages.includes(path);
  };

  const isDiarySubPage = () => {
    const path = location.pathname;
    return path.startsWith("/diaries/") && path !== "/diaries";
  };

  const handleCreateTodo = () => {
    navigate("/todo/new");
  };

  const today = new Date();
  const dayOfWeek = ["일", "월", "화", "수", "목", "금", "토"][today.getDay()];

  return (
    <>
      <header className="flex w-full justify-center pt-3 pb-2">
        <div className="w-full max-w-4xl px-4">
          <div className="mb-2 flex items-center justify-between">
            <h1 className="text-xl font-bold">{getPageTitle()}</h1>
            <div className="flex items-center gap-2">
              {(user?.userRole === "BIF" || user?.userRole === "GUARDIAN") && (
                <button
                  onClick={() =>
                    isProfilePage && onBadgeClick
                      ? onBadgeClick()
                      : user?.userRole === "BIF"
                        ? setShowNotificationModal(true)
                        : null
                  }
                  className="relative flex h-10 w-10 items-center justify-center rounded-full bg-white"
                  title={
                    isProfilePage
                      ? "뱃지 보기"
                      : user?.userRole === "BIF" && stats.unread > 0
                        ? `읽지 않은 알림 ${stats.unread}개`
                        : user?.userRole === "BIF"
                          ? "알림 센터"
                          : "뱃지 보기"
                  }
                >
                  {isProfilePage ? (
                    <FaMedal size={20} color="#343434" />
                  ) : user?.userRole === "BIF" ? (
                    <>
                      <TbBell size={20} color="#343434" />
                      {stats.unread > 0 && (
                        <span className="bg-warning absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full text-xs font-bold text-white">
                          {stats.unread > 9 ? "9+" : stats.unread}
                        </span>
                      )}
                    </>
                  ) : (
                    <FaMedal size={20} color="#343434" />
                  )}
                </button>
              )}
              <Link
                to={profilePath}
                className="flex h-10 w-10 items-center justify-center rounded-full bg-white"
              >
                <CgProfile size={20} color="#343434" />
              </Link>
            </div>
          </div>

          <div className="flex items-center justify-between">
            {isMainPage() ? (
              <div className="flex flex-col">
                <p className="text-sm">{formatDateToDisplay(today)}</p>
                <p className="text-lg font-semibold">{dayOfWeek}요일</p>
              </div>
            ) : isDiarySubPage() ? (
              <DiaryBackButton title="뒤로가기" onBackClick={onBackClick} />
            ) : (
              <BackButton title="뒤로가기" />
            )}

            <div className="flex items-center gap-3">
              {rightActions}
              {isProfilePage && onEditProfileClick && (
                <button
                  onClick={onEditProfileClick}
                  className="flex items-center space-x-1 rounded border border-black bg-white px-3 py-2 text-sm font-bold text-gray-800"
                >
                  <IoPerson className="h-4 w-4" />
                  <span>회원정보수정</span>
                </button>
              )}
              {showTodoButton && user?.userRole === "BIF" && isMainPage() && (
                <button
                  onClick={handleCreateTodo}
                  className="flex items-center gap-1 rounded-xl bg-black px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-gray-700"
                >
                  <BiPlus size={18} />할 일 생성
                </button>
              )}
            </div>
          </div>
        </div>
      </header>

      <NotificationSettingsModal
        isOpen={showNotificationModal}
        onClose={() => setShowNotificationModal(false)}
      />
    </>
  );
}
