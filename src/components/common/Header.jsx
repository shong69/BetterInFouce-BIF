import Logo from "@components/ui/Logo";
import { Link } from "react-router-dom";
import { CgProfile } from "react-icons/cg";
import { useUserStore } from "@stores";

export default function Header() {
  const { user } = useUserStore();

  const profilePath =
    user?.userRole === "GUARDIAN" ? "/guardian-profile" : "/bif-profile";

  return (
    <header className="sticky top-0 z-40 flex items-center justify-between bg-white px-3 py-3 shadow-sm">
      <Logo />
      <Link
        to={profilePath}
        className="bg-opacity-20 flex h-10 w-10 items-center justify-center rounded-full bg-gray-100"
      >
        <CgProfile size={25} color="#B1B1B1" />
      </Link>
    </header>
  );
}
