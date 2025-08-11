import { useState, useRef } from "react";
import EditButton from "@components/ui/EditButton";
import DeleteButton from "@components/ui/DeleteButton";
import { getRandomColor } from "@utils/colorUtils";
import { HiChevronRight, HiChevronDown } from "react-icons/hi";

export default function Card({
  id,
  title,
  category,
  duration,
  items = [],
  type = "todo",
  isExpandable = false,
  onEdit,
  onDelete,
  onClick,
}) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isSwipeOpen, setIsSwipeOpen] = useState(false);
  const [startX, setStartX] = useState(0);
  const [currentX, setCurrentX] = useState(0);
  const cardRef = useRef(null);

  const colors = getRandomColor(title);

  function handleTouchStart(e) {
    if (type !== "todo") {
      return;
    }

    setStartX(e.touches[0].clientX);
    setCurrentX(e.touches[0].clientX);
  }

  function handleTouchMove(e) {
    if (type !== "todo" || !startX) {
      return;
    }

    const newCurrentX = e.touches[0].clientX;
    const diffX = newCurrentX - startX;

    if (diffX > 0 && diffX < 120) {
      setCurrentX(newCurrentX);
      if (cardRef.current) {
        cardRef.current.style.transform = `translateX(${diffX}px)`;
      }
    }
  }

  function handleTouchEnd() {
    if (type !== "todo" || !startX) {
      return;
    }

    const diffX = currentX - startX;

    if (diffX > 60) {
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

  function renderContent() {
    if (type === "simulation") {
      return (
        <div>
          <div className="mb-2 flex items-center justify-between">
            <h3 className={`text-lg font-medium ${colors.title} flex-1`}>
              {title}
            </h3>
            <span
              className={`rounded-full px-3 py-1 text-sm ${colors.tag} ml-3`}
            >
              {category}
            </span>
          </div>

          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-500">약 {duration}분</p>
            <HiChevronRight className="h-5 w-5 text-green-600" />
          </div>
        </div>
      );
    }

    return (
      <div>
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <h3 className={`text-lg font-medium ${colors.title}`}>{title}</h3>
          </div>
          <div className="flex items-center gap-3">
            <span className={`rounded-full px-3 py-1 text-sm ${colors.tag}`}>
              {category}
            </span>
            {isExpandable && (
              <button
                onClick={handleToggle}
                className={`flex h-8 w-8 items-center justify-center rounded-full transition-all duration-200 ${colors.button} hover:opacity-80`}
              >
                <HiChevronDown
                  className={`h-4 w-4 transition-transform duration-200 ${isExpanded ? "rotate-180" : ""}`}
                />
              </button>
            )}
          </div>
        </div>

        {isExpanded && (
          <div className="mt-4">
            {items.map((item) => (
              <div
                key={item.id}
                className="flex items-center rounded-lg px-3 py-1 text-sm text-gray-700"
              >
                <span className="mr-3 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400" />
                <span>{typeof item === "string" ? item : item.text}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div
      className={`relative ${type === "todo" ? "overflow-hidden" : ""} rounded-xl`}
    >
      {type === "todo" && (
        <div className="absolute top-0 left-0 flex h-full items-center gap-2 pl-4">
          <div
            onClick={handleEdit}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                handleEdit(e);
              }
            }}
            role="button"
            tabIndex={0}
            className="focus:ring-primary-500 rounded focus:ring-2 focus:outline-none"
            aria-label="할 일 수정"
          >
            <EditButton />
          </div>
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
        </div>
      )}

      <div
        ref={cardRef}
        className={`relative rounded-xl bg-white p-4 transition-transform duration-200 ease-out ${
          type === "todo"
            ? "border-2 border-gray-200"
            : "cursor-pointer border border-gray-300 hover:border-gray-400"
        }`}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
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
        {renderContent()}
      </div>
    </div>
  );
}
