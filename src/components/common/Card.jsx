export default function Card({
  id = 1,
  title = "제목",
  category = "일상",
  duration = "10분",
  onClick,
}) {
  function getCategoryColor() {
    switch (category) {
      case "업무":
        return "text-warning";
      case "일상":
        return "text-[#F59E0B]";
      case "사회":
      default:
        return "text-[#0B70F5]";
    }
  }

  function getTagColor() {
    switch (category) {
      case "업무":
        return "bg-[#FEE2E2] text-warning";
      case "일상":
        return "bg-[#FEF3C7] text-[#F59E0B]";
      case "사회":
      default:
        return "bg-[#C2DCFF] text-[#0B70F5]";
    }
  }

  function handleClick() {
    if (onClick) {
      onClick(id);
    }
  }

  function handleKeyDown(event) {
    if (event.key === "Enter" || event.key === " ") {
      handleClick();
    }
  }

  return (
    <button
      className="w-full cursor-pointer rounded-xl bg-white p-3 pt-4 text-left shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
    >
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <h3
            className={`mb-1 text-[13px] font-semibold ${getCategoryColor()}`}
          >
            {title}
          </h3>
          <p className="text-lg text-[9px] text-[#515151]">약 {duration}분</p>
        </div>
        <div className="flex flex-col items-center gap-2">
          <span
            className={`rounded-xl px-3 py-1 text-[9px] font-medium ${getTagColor()}`}
          >
            {category}
          </span>
          <div className="text-primary font-sm text-lg">&#x276F;</div>
        </div>
      </div>
    </button>
  );
}
