import { FiEdit } from "react-icons/fi";

export default function DeleteButton() {
  return (
    <button className="bg-secondary mr-2 flex h-10 w-10 items-center justify-center rounded-xl text-white shadow-md">
      <FiEdit size={20} />
    </button>
  );
}
