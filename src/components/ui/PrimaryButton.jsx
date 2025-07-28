export default function Button({ title }) {
  return (
    <button className="bg-primary w-full rounded-lg px-2 py-3 font-medium text-white transition-all hover:bg-[#0A7B06]">
      {title}
    </button>
  );
}
