export default function TabButton({
  activeTab,
  setActiveTab,
  leftTitle,
  rightTitle,
}) {
  return (
    <div className="flex w-full overflow-hidden rounded-lg bg-white shadow-sm">
      <button
        className={`flex-1 px-4 py-2 ${activeTab === "routine" ? "bg-toggle rounded-s-lg font-medium text-white" : ""}`}
        onClick={() => setActiveTab("routine")}
      >
        {leftTitle}
      </button>
      <button
        className={`flex-1 px-4 py-2 ${activeTab === "todo" ? "bg-toggle rounded-r-lg font-medium text-white" : ""}`}
        onClick={() => setActiveTab("todo")}
      >
        {rightTitle}
      </button>
    </div>
  );
}
