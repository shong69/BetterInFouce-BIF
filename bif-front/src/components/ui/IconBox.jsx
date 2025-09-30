import { Link } from "react-router-dom";

export default function IconBox({ icon, title, titleStyle, to, isActive }) {
  return (
    <Link
      to={to}
      className={`group flex items-center rounded-full p-3 transition-all ${isActive ? "bg-primary/10 px-5" : ""}`}
    >
      {icon}
      {title && (
        <span className={`${titleStyle} ml-2 text-sm whitespace-nowrap`}>
          {title}
        </span>
      )}
    </Link>
  );
}
