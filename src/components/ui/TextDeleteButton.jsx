export default function TextDeleteButton({ onClick }) {
  return (
    <button
      onClick={onClick}
      className="bg-warning rounded-xl px-3 py-1 text-sm font-medium text-white transition-colors duration-200 hover:bg-red-600"
    >
      삭제
    </button>
  );
}
