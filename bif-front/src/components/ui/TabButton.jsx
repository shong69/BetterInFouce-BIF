export default function TabButton({
  activeTab,
  setActiveTab,
  leftTitle,
  rightTitle,
  leftValue = "ROUTINE",
  rightValue = "TASK",
}) {
  return (
    <div className="inline-flex rounded-full bg-gray-100 p-1 shadow-sm">
      <button
        className={`rounded-full px-6 py-2 text-sm font-medium transition-all duration-200 ${
          activeTab === leftValue
            ? "bg-white text-black shadow-sm"
            : "text-gray-600 hover:text-black"
        }`}
        onClick={() => setActiveTab(leftValue)}
      >
        {leftTitle}
      </button>
      <button
        className={`rounded-full px-6 py-2 text-sm font-medium transition-all duration-200 ${
          activeTab === rightValue
            ? "bg-white text-black shadow-sm"
            : "text-gray-600 hover:text-black"
        }`}
        onClick={() => setActiveTab(rightValue)}
      >
        {rightTitle}
      </button>
    </div>
  );
}
