import { HiOutlineTrash } from "react-icons/hi";

export default function DeleteButton() {
  return (
    <button className="bg-warning flex h-10 w-10 items-center justify-center rounded-xl text-white shadow-md">
      <HiOutlineTrash size={25} />
    </button>
  );
}
