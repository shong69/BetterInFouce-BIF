export default function SecondaryButton({ title, onClick, className = "" }) {
  return (
    <button
      onClick={onClick}
      className={`from-gradient-yellow w-full cursor-pointer rounded-3xl bg-linear-to-r to-[#FFB347] px-2 py-3 font-medium text-gray-900 shadow-xl transition-all hover:from-[#ffed84] hover:to-[#ffa629] ${className}`}
    >
      {title}
    </button>
  );
}
