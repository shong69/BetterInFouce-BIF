import Logo from "@components/ui/Logo";
import { Link } from "react-router-dom";
import { CgProfile } from "react-icons/cg";

export default function Header() {
  return (
    <header className="sticky top-0 flex items-center justify-between bg-white px-3 py-3 shadow-sm">
      <Logo />
      <Link
        to=""
        className="bg-opacity-20 flex h-10 w-10 items-center justify-center rounded-full bg-gray-100"
      >
        <CgProfile size={25} color="#B1B1B1" />
      </Link>
    </header>
  );
}
