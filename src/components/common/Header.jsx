import Logo from "@components/ui/Logo";
import { Link } from "react-router-dom";
import { CgProfile } from "react-icons/cg";
import { useUserStore } from "@stores/userStore";

export default function Header() {
  const { user } = useUserStore();

  // 사용자 역할에 따른 프로필 페이지 경로 결정
  const getProfilePath = () => {
    if (!user) return "/bif-profile"; // 기본값

    switch (user.userRole) {
      case "BIF":
        return "/bif-profile";
      case "GUARDIAN":
        return "/guardian-profile";
      default:
        return "/bif-profile";
    }
  };

  return (
    <header className="sticky top-0 flex items-center justify-between bg-white px-3 py-3 shadow-sm">
      <Logo />
      <Link
        to={getProfilePath()}
        className="bg-opacity-20 flex h-10 w-10 items-center justify-center rounded-full bg-gray-100"
      >
        <CgProfile size={25} color="#B1B1B1" />
      </Link>
    </header>
  );
}
