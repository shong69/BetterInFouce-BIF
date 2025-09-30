import { useState, useEffect } from "react";
import { BiCalendar, BiX } from "react-icons/bi";
import { formatDateToYMD } from "@utils/dateUtils";

export default function DatePickerModal({
  isOpen,
  onClose,
  onDateSelect,
  currentDate,
}) {
  const [selectedDate, setSelectedDate] = useState(currentDate || new Date());

  useEffect(() => {
    if (isOpen) {
      setSelectedDate(currentDate || new Date());
    }
  }, [isOpen, currentDate]);

  const handleDateSelect = (date) => {
    setSelectedDate(date);
    onDateSelect(date);
    onClose();
  };

  const handleMonthChange = (increment) => {
    const newDate = new Date(selectedDate);
    newDate.setMonth(newDate.getMonth() + increment);
    setSelectedDate(newDate);
  };

  const renderCalendar = () => {
    const year = selectedDate.getFullYear();
    const month = selectedDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());

    const days = [];
    const today = new Date();
    const todayStr = formatDateToYMD(today);

    for (let i = 0; i < 42; i++) {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + i);

      const dateStr = formatDateToYMD(date);
      const isCurrentMonth = date.getMonth() === month;
      const isToday = dateStr === todayStr;
      const isSelected = formatDateToYMD(selectedDate) === dateStr;

      days.push({
        date: date,
        day: date.getDate(),
        isCurrentMonth,
        isToday,
        isSelected,
      });
    }

    return (
      <div className="mt-4">
        <div className="mb-4 flex items-center justify-between">
          <button
            onClick={() => handleMonthChange(-1)}
            className="rounded-lg p-2 hover:bg-gray-100"
          >
            ←
          </button>
          <h3 className="text-lg font-medium">
            {year}년 {month + 1}월
          </h3>
          <button
            onClick={() => handleMonthChange(1)}
            className="rounded-lg p-2 hover:bg-gray-100"
          >
            →
          </button>
        </div>

        <div className="mb-2 grid grid-cols-7 gap-1">
          {["일", "월", "화", "수", "목", "금", "토"].map((day) => (
            <div key={day} className="py-2 text-center text-sm text-gray-500">
              {day}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-7 gap-1">
          {days.map((dayInfo) => (
            <button
              key={formatDateToYMD(dayInfo.date)}
              onClick={() => handleDateSelect(dayInfo.date)}
              className={`rounded-lg p-2 text-sm transition-colors ${!dayInfo.isCurrentMonth ? "text-gray-300" : "text-gray-900"} ${dayInfo.isToday ? "bg-blue-100 font-medium text-blue-600" : ""} ${dayInfo.isSelected ? "bg-secondary text-white" : "hover:bg-gray-100"} `}
            >
              {dayInfo.day}
            </button>
          ))}
        </div>
      </div>
    );
  };

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
      onClick={onClose}
      onKeyDown={(e) => e.key === "Escape" && onClose()}
      role="button"
      tabIndex={0}
    >
      <div
        className="max-h-[90vh] w-full max-w-sm overflow-y-auto rounded-lg bg-white shadow-lg"
        onClick={(e) => e.stopPropagation()}
        role="presentation"
      >
        <div className="flex items-center justify-between border-b p-4">
          <div className="flex items-center space-x-2">
            <BiCalendar size={20} />
            <h2 className="text-md font-medium">날짜 선택</h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 hover:bg-gray-100"
          >
            <BiX size={20} />
          </button>
        </div>

        <div className="p-4">{renderCalendar()}</div>
      </div>
    </div>
  );
}
