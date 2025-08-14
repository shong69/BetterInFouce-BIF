import { HiOutlineTrash } from "react-icons/hi";

export default function DeleteButton({ onClick }) {
  return (
    <button
      onClick={onClick}
      className="bg-warning hover:bg-warning/80 flex h-10 w-10 items-center justify-center rounded-xl text-white shadow-md transition-colors"
    >
      <HiOutlineTrash size={25} />
    </button>
  );
}
