export default function Button({ title, onClick }) {
  return (
    <button
      onClick={onClick}
      className="from-gradient-yellow w-full rounded-lg bg-linear-to-r to-[#b1d827] px-2 py-3 font-medium text-white transition-all text-shadow-md/10 hover:bg-[#0A7B06]"
    >
      {title}
    </button>
  );
}
