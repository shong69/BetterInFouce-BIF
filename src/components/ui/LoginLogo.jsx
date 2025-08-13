import logo from "@assets/logo.png";
import { Link } from "react-router-dom";

export default function LoginLogo() {
  return (
    <Link to="/" className="text-primary flex items-center text-2xl font-bold">
      <img src={logo} alt="BIF logo" className="mr-0.5 w-14" />
      Better In Focus
    </Link>
  );
}
