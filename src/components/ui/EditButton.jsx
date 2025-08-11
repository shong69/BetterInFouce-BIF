import { FiEdit } from "react-icons/fi";

export default function EditButton({ onClick }) {
  return (
    <button
      onClick={onClick}
      className="bg-secondary hover:bg-secondary/80 mr-2 flex h-10 w-10 items-center justify-center rounded-xl text-white shadow-md transition-colors"
    >
      <FiEdit size={20} />
    </button>
  );
}
