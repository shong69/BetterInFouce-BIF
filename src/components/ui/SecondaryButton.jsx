export default function Button({ title }) {
  return (
    <button className="from-gradient-yellow w-full rounded-lg bg-linear-to-r to-[#b1d827] px-2 py-3 font-medium text-white transition-all hover:bg-[#0A7B06]">
      {title}
    </button>
  );
}
