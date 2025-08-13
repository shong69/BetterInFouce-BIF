export default function SimulationCard({
  id = 1,
  title = "제목",
  category = "일상",
  duration = "10분",
  onClick,
  showRecommendButton = false,
  onRecommend = null,
  isRecommended = false,
  showThumbsUpButton = false,
  onThumbsUp = null,
  isThumbsUp = false,
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

  function handleRecommendClick(event) {
    event.stopPropagation();
    if (onRecommend) {
      onRecommend(id);
    }
  }

  function handleThumbsUpClick(event) {
    event.stopPropagation();
    if (onThumbsUp) {
      onThumbsUp(id);
    }
  }

  return (
    <div className="relative">
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
          <div className="flex items-start gap-1">
            {showThumbsUpButton && (
              <button
                onClick={handleThumbsUpClick}
                className={`rounded-full p-1 transition-colors ${
                  isThumbsUp ? "text-primary" : "text-gray-400"
                }`}
                title={isThumbsUp ? "좋아요 취소" : "좋아요"}
              >
                <svg
                  className="h-4 w-4"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
                </svg>
              </button>
            )}
            <div className="flex flex-col items-center gap-2">
              <span
                className={`rounded-xl px-3 py-1 text-[9px] font-medium ${getTagColor()}`}
              >
                {category}
              </span>
              <div className="text-primary font-sm text-lg">&#x276F;</div>
            </div>
          </div>
        </div>
      </button>

      {showRecommendButton && (
        <button
          onClick={handleRecommendClick}
          className={`absolute -top-2 -right-2 rounded-full px-2 py-1 text-xs font-medium transition-colors ${
            isRecommended
              ? "bg-primary text-white"
              : "hover:bg-primary bg-gray-200 text-gray-700 hover:text-white"
          }`}
          title={isRecommended ? "추천됨" : "추천하기"}
        >
          {isRecommended ? "✓" : "추천"}
        </button>
      )}
    </div>
  );
}
