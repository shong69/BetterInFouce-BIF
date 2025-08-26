import Logo from "@components/ui/Logo";
import { Link } from "react-router-dom";
import { CgProfile } from "react-icons/cg";
import { TbBell } from "react-icons/tb";
import { useUserStore, useNotificationStore } from "@stores";
import { useState, useEffect } from "react";
import NotificationSettingsModal from "@components/notifications/NotificationSettingsModal";

export default function Header() {
  const { user } = useUserStore();
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

  return (
    <>
      <header className="sticky top-0 z-40 flex items-center justify-between bg-white px-3 py-3 shadow-sm">
        <Logo />
        <div className="flex items-center gap-2">
          {user?.userRole === "BIF" && (
            <button
              onClick={() => setShowNotificationModal(true)}
              className="bg-opacity-20 relative flex h-10 w-10 items-center justify-center rounded-full bg-gray-100"
              title={
                stats.unread > 0
                  ? `읽지 않은 알림 ${stats.unread}개`
                  : "알림 센터"
              }
            >
              <TbBell size={25} color="#B1B1B1" />
              {stats.unread > 0 && (
                <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white">
                  {stats.unread > 9 ? "9+" : stats.unread}
                </span>
              )}
            </button>
          )}
          <Link
            to={profilePath}
            className="bg-opacity-20 flex h-10 w-10 items-center justify-center rounded-full bg-gray-100"
          >
            <CgProfile size={25} color="#B1B1B1" />
          </Link>
        </div>
      </header>

      <NotificationSettingsModal
        isOpen={showNotificationModal}
        onClose={() => setShowNotificationModal(false)}
      />
    </>
  );
}
