import { HiChevronLeft } from "react-icons/hi";
import { useNavigate } from "react-router-dom";

export default function DiaryBackButton({ title = "뒤로가기", onBackClick }) {
  const navigate = useNavigate();

  function handleClick() {
    if (onBackClick) {
      onBackClick();
    } else {
      navigate("/diaries");
    }
  }

  return (
    <button
      onClick={handleClick}
      className="text-black-600 flex items-center border-none text-lg font-bold transition-colors hover:text-gray-900"
    >
      <HiChevronLeft size={20} />
      <span className="mr-2">{title}</span>
    </button>
  );
}
