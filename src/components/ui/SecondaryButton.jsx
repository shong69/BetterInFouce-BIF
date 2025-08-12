export default function SecondaryButton({ title, onClick, className = "" }) {
  return (
    <button
      onClick={onClick}
      className={`from-gradient-yellow w-full cursor-pointer rounded-lg bg-linear-to-r to-[#b1d827] px-2 py-3 font-medium text-white shadow-xl transition-all text-shadow-md/10 hover:bg-[#0A7B06] ${className}`}
    >
      {title}
    </button>
  );
}
