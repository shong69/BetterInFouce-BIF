import { Link } from "react-router-dom";

export default function IconBox({ icon, title, titleStyle, to }) {
  return (
    <Link to={to} className="group flex flex-col items-center px-6 py-3">
      {icon}
      <span className={`${titleStyle} mt-1 text-sm`}>{title}</span>
    </Link>
  );
}
