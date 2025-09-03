export default function TextEditButton({ onClick }) {
  return (
    <button
      onClick={onClick}
      className="rounded-xl border bg-[#343434] px-3 py-1 text-sm font-medium text-white transition-colors duration-200 hover:bg-black"
    >
      수정
    </button>
  );
}
