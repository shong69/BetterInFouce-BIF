import { HiBell } from "react-icons/hi";
import { Link } from "react-router-dom";
import { CgProfile } from "react-icons/cg";
import { useUserStore } from "@stores";

export default function PageHeader({ title = "감정일기" }) {
  const { user } = useUserStore();

  const profilePath =
    user?.userRole === "GUARDIAN" ? "/guardian-profile" : "/bif-profile";

  const handleNotificationClick = () => {};

  return (
    <header className="bg-transparent">
      <div style={{ paddingTop: "calc(env(safe-area-inset-top) + 16px)" }}>
        <div className="mx-auto max-w-2xl px-2 py-1 sm:px-4">
          <div className="flex items-center justify-between">
            <h1 className="pl-3 text-xl font-bold text-gray-800">{title}</h1>

            <div className="flex items-center space-x-3 pr-3">
              <button
                onClick={handleNotificationClick}
                className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100 p-1.5"
              >
                <HiBell className="h-10 w-10 text-gray-600" />
              </button>

              <Link
                to={profilePath}
                className="bg-opacity-20 flex h-8 w-8 items-center justify-center rounded-full bg-gray-100"
              >
                <CgProfile size={23} color="#B1B1B1" />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
