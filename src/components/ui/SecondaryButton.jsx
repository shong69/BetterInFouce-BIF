export default function SecondaryButton({ title, onClick, className = "" }) {
  return (
    <button
      onClick={onClick}
      className={`from-yellow to-orange w-full cursor-pointer rounded-4xl bg-linear-to-r px-2 py-3 font-medium text-[#0d0d0d] shadow-xl transition-all text-shadow-md/10 hover:from-[#ffed84] hover:to-[#ffa629] ${className}`}
    >
      {title}
    </button>
  );
}
