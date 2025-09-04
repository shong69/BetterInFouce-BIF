export default function SimulationCard({
  id = 1,
  title = "제목",
  onClick,
  showThumbsUpButton = false,
  onThumbsUp = null,
  isThumbsUp = false,
}) {
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

  function handleThumbsUpClick(event) {
    event.stopPropagation();
    if (onThumbsUp) {
      onThumbsUp(id);
    }
  }

  return (
    <div className="relative">
      <div
        className={`w-full cursor-pointer rounded-xl bg-white p-3 pt-4 text-left shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg ${
          isThumbsUp ? "border-primary/60 border-2" : ""
        }`}
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        role="button"
        tabIndex={0}
      >
        <div className="flex h-full items-center justify-between">
          <div className="flex flex-1 items-center">
            <h3 className="text-[13px] font-extrabold text-black">{title}</h3>
          </div>
          <div className="flex items-center gap-3">
            {isThumbsUp && (
              <span className="text-primary/80 bg-primary/10 rounded-full px-2 py-0.5 text-xs font-medium">
                추천
              </span>
            )}
            {showThumbsUpButton && (
              <button
                onClick={handleThumbsUpClick}
                className={`h-4 w-4 transition-colors duration-200 ${
                  isThumbsUp
                    ? "text-primary scale-110"
                    : "text-gray-400 hover:text-gray-600"
                }`}
                title={isThumbsUp ? "추천 취소" : "추천"}
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
          </div>
        </div>
      </div>
    </div>
  );
}
