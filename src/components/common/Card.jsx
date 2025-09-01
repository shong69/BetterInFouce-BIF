import { BsListCheck } from "react-icons/bs";
import { MdOutlineRepeat } from "react-icons/md";
import { BiTime, BiCheckSquare, BiChevronRight } from "react-icons/bi";
import { MdFormatListNumbered } from "react-icons/md";

export default function Card({
  id,
  title,
  hasOrder,
  isCompleted = false,
  todoType = "TASK",
  startTime,
  endTime,
  onClick,
}) {
  function formatTime(time) {
    if (!time) return null;
    const [hours, minutes] = time.split(":");
    const hour = parseInt(hours);
    const ampm = hour >= 12 ? "오후" : "오전";
    const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
    return `${ampm} ${displayHour}:${minutes}`;
  }

  function timeDisplay() {
    if (startTime && endTime) {
      return `${formatTime(startTime)} - ${formatTime(endTime)}`;
    } else if (startTime) {
      return formatTime(startTime);
    }
    return null;
  }

  function handleCardClick() {
    if (onClick) {
      onClick(id);
    }
  }

  function handleKeyDown(event) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleCardClick();
    }
  }

  return (
    <div
      className={`cursor-pointer rounded-xl border border-gray-300 p-4 shadow-sm transition-all ${
        isCompleted
          ? "bg-gray-100/80 opacity-70"
          : "bg-white/90 hover:shadow-md"
      }`}
      onClick={handleCardClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
    >
      <div className="flex items-center justify-between">
        <div className="flex flex-1 items-start gap-3">
          <div className="flex-shrink-0">
            {todoType === "ROUTINE" ? (
              <div
                className="flex h-12 w-12 flex-col items-center justify-center rounded-lg"
                style={{ backgroundColor: "#FFB347" }}
              >
                <MdOutlineRepeat className="mb-0.5 text-white" size={20} />
                <span
                  className="text-xs leading-none font-medium text-white"
                  style={{ fontSize: "9px" }}
                >
                  루틴
                </span>
              </div>
            ) : (
              <div
                className="flex h-12 w-12 flex-col items-center justify-center rounded-lg"
                style={{ backgroundColor: "#4CAF50" }}
              >
                <BsListCheck className="mb-0.5 text-white" size={20} />
                <span
                  className="text-xs leading-none font-medium text-white"
                  style={{ fontSize: "9px" }}
                >
                  할일
                </span>
              </div>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <h3
              className={`text-lg font-medium ${isCompleted ? "text-gray-400 line-through opacity-60" : "text-black"}`}
            >
              {title}
            </h3>

            <div className="space-y-1">
              {timeDisplay() && (
                <div className="flex items-center gap-1">
                  <BiTime className="text-gray-400" size={14} />
                  <span className="text-sm text-gray-500">{timeDisplay()}</span>
                </div>
              )}

              <div className="flex items-center gap-1">
                {hasOrder ? (
                  <MdFormatListNumbered className="text-gray-400" size={14} />
                ) : (
                  <BiCheckSquare className="text-gray-400" size={14} />
                )}
                <span className="text-sm text-gray-500">
                  {hasOrder ? "순서있음" : "체크리스트"}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-center">
          <BiChevronRight className="text-black" size={28} />
        </div>
      </div>
    </div>
  );
}
