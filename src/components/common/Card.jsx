import { useState, useRef, useEffect } from "react";
import EditButton from "@components/ui/EditButton";
import DeleteButton from "@components/ui/DeleteButton";
import { getRandomColorByTitle } from "@utils/colorUtils";
import { IoIosArrowDown } from "react-icons/io";
import { useUserStore } from "@stores";

export default function Card({
  id,
  title,
  hasOrder,
  subTodos = [],
  type = "todo",
  isCompleted = false,
  onEdit,
  onDelete,
  onClick,
}) {
  const { user } = useUserStore();
  const [isExpanded, setIsExpanded] = useState(false);
  const [isSwipeOpen, setIsSwipeOpen] = useState(false);
  const [startX, setStartX] = useState(0);
  const [currentX, setCurrentX] = useState(0);
  const cardRef = useRef(null);

  const colors = getRandomColorByTitle(title, id);

  useEffect(() => {
    const cardElement = cardRef.current;
    if (!cardElement || type !== "todo") return;

    const handleTouchMovePassive = (e) => {
      if (!startX) return;
      e.preventDefault();

      const newCurrentX = e.touches[0].clientX;
      const diffX = newCurrentX - startX;

      if (diffX > 0 && diffX < 120) {
        setCurrentX(newCurrentX);
        cardElement.style.transform = `translateX(${diffX}px)`;
      }
    };

    cardElement.addEventListener("touchmove", handleTouchMovePassive, {
      passive: false,
    });

    return () => {
      cardElement.removeEventListener("touchmove", handleTouchMovePassive);
    };
  }, [startX, type]);

  function handleTouchStart(e) {
    if (type !== "todo") {
      return;
    }

    setStartX(e.touches[0].clientX);
    setCurrentX(e.touches[0].clientX);
  }

  function handleTouchEnd() {
    if (type !== "todo" || !startX) {
      return;
    }

    const diffX = currentX - startX;

    if (diffX > 40) {
      setIsSwipeOpen(true);
      if (cardRef.current) {
        cardRef.current.style.transform = "translateX(120px)";
      }
    } else {
      setIsSwipeOpen(false);
      if (cardRef.current) {
        cardRef.current.style.transform = "translateX(0px)";
      }
    }

    setStartX(0);
    setCurrentX(0);
  }

  function handleCardClick() {
    if (type === "todo" && isSwipeOpen) {
      setIsSwipeOpen(false);
      if (cardRef.current) {
        cardRef.current.style.transform = "translateX(0px)";
      }
    } else if (onClick) {
      onClick(id);
    }
  }

  function handlePointerStart(e) {
    if (type !== "todo") return;

    const x = e.type.includes("touch") ? e.touches[0].clientX : e.clientX;
    setStartX(x);
    setCurrentX(x);
  }

  function handlePointerMove(e) {
    if (type !== "todo" || !startX) return;

    e.preventDefault();

    const x = e.type.includes("touch") ? e.touches[0].clientX : e.clientX;
    const diffX = x - startX;

    if (diffX > 0 && diffX < 120) {
      setCurrentX(x);
      if (cardRef.current) {
        cardRef.current.style.transform = `translateX(${diffX}px)`;
      }
    }
  }

  function handlePointerEnd() {
    if (type !== "todo" || !startX) return;

    const diffX = currentX - startX;

    if (diffX > 40) {
      setIsSwipeOpen(true);
      if (cardRef.current) {
        cardRef.current.style.transform = "translateX(120px)";
      }
    } else {
      setIsSwipeOpen(false);
      if (cardRef.current) {
        cardRef.current.style.transform = "translateX(0px)";
      }
    }

    setStartX(0);
    setCurrentX(0);
  }

  function handleEdit(e) {
    e.stopPropagation();
    if (onEdit) {
      onEdit(id);
    }
    setIsSwipeOpen(false);
    if (cardRef.current) {
      cardRef.current.style.transform = "translateX(0px)";
    }
  }

  function handleDelete(e) {
    e.stopPropagation();
    if (onDelete) {
      onDelete(id);
    }
    setIsSwipeOpen(false);
    if (cardRef.current) {
      cardRef.current.style.transform = "translateX(0px)";
    }
  }

  function handleToggle(e) {
    e.stopPropagation();
    setIsExpanded(!isExpanded);
  }

  return (
    <div className="rounded-xl shadow-sm">
      <div
        className={`relative ${type === "todo" ? "overflow-hidden" : ""} rounded-xl`}
      >
        {type === "todo" && (
          <div className="absolute top-0 left-0 flex h-full items-center gap-2 pl-4">
            <EditButton onClick={handleEdit} />
            {user?.userRole === "BIF" && (
              <div
                onClick={handleDelete}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    handleDelete(e);
                  }
                }}
                role="button"
                tabIndex={0}
                className="focus:ring-warning-500 rounded focus:ring-2 focus:outline-none"
                aria-label="할 일 삭제"
              >
                <DeleteButton />
              </div>
            )}
          </div>
        )}

        <div
          ref={cardRef}
          style={{ touchAction: "pan-y" }}
          className={`relative rounded-xl p-3 pt-4 text-left transition-transform duration-200 ease-out ${
            type === "todo"
              ? `border-1 border-gray-300 ${isCompleted ? "border-gray-300 bg-gray-100" : "border-gray-200 bg-white"}`
              : "cursor-pointer border-1 border-gray-300 bg-white hover:border-gray-400"
          }`}
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
          onPointerDown={handlePointerStart}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerEnd}
          onPointerLeave={handlePointerEnd}
          onClick={handleCardClick}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              handleCardClick();
            }
          }}
          role="button"
          tabIndex={0}
          aria-label={`${title} 카드`}
        >
          <div>
            <div className="flex items-center justify-between">
              <div className="flex flex-1 items-center">
                <h3 className={`text-md font-medium ${colors.title}`}>
                  {title}
                </h3>
              </div>
              <div className="flex flex-col items-end gap-2">
                <span
                  className={`rounded-xl px-3 py-1 text-sm font-medium ${colors.tag}`}
                >
                  {hasOrder ? "순서 있음" : "체크리스트"}
                </span>
                {subTodos.length > 0 && (
                  <button
                    onClick={handleToggle}
                    className={`flex items-center justify-center rounded-full transition-all duration-200 hover:opacity-80`}
                  >
                    <IoIosArrowDown
                      className={`text-primary font-sm text-lg transition-transform duration-200 ${isExpanded ? "rotate-180" : ""}`}
                    />
                  </button>
                )}
              </div>
            </div>

            {isExpanded && (
              <div className="mt-4">
                {subTodos.map((item) => (
                  <div
                    key={item.subTodoId}
                    className="flex items-center rounded-lg px-3 py-1 text-sm text-gray-700"
                  >
                    <span className="mr-3 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400" />
                    <span>{item.title}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
