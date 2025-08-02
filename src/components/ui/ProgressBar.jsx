export default function ProgressBar({ progress = 0, label = "진행도" }) {
  return (
    <div className="flex flex-col gap-0.5">
      <div className="flex items-center justify-between">
        <span className="text-[9px] font-medium text-gray-500">{label}</span>
        <span className="text-[9px] font-medium text-gray-500">
          {Math.round(progress)}%
        </span>
      </div>
      <div className="h-2 rounded-full bg-[#D3EACA]">
        <div
          className="bg-toggle h-2 rounded-full transition-all duration-300"
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
}
