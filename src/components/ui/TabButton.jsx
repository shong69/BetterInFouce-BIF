export default function TabButton({
  activeTab,
  setActiveTab,
  leftTitle,
  rightTitle,
  leftValue = "ROUTINE",
  rightValue = "TASK",
}) {
  return (
    <div className="flex w-full overflow-hidden rounded-lg bg-white shadow-sm">
      <button
        className={`flex-1 px-4 py-2 ${activeTab === leftValue ? "bg-toggle rounded-s-lg font-medium text-white" : ""}`}
        onClick={() => setActiveTab(leftValue)}
      >
        {leftTitle}
      </button>
      <button
        className={`flex-1 px-4 py-2 ${activeTab === rightValue ? "bg-toggle rounded-r-lg font-medium text-white" : ""}`}
        onClick={() => setActiveTab(rightValue)}
      >
        {rightTitle}
      </button>
    </div>
  );
}
