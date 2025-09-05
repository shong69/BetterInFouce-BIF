import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import Header from "@components/common/Header";
import Card from "@components/common/Card";
import TabBar from "@components/common/TabBar";

import { formatDateToYMD, addDays } from "@utils/dateUtils";
import { HiOutlineClipboardList } from "react-icons/hi";
import { BiChevronLeft, BiChevronRight, BiHome } from "react-icons/bi";

import { useToastStore, useTodoStore, useUserStore } from "@stores";
import { getTodos } from "@services/todoService";

function EmptyTodoState() {
  return (
    <div className="px-4 py-12 text-center text-gray-500">
      <HiOutlineClipboardList className="mx-auto mb-4 h-16 w-16 opacity-50" />
      <p className="mb-2 text-lg font-medium">할 일이 없어요</p>
      <p className="text-sm text-gray-400">새로운 할 일을 추가해보세요!</p>
    </div>
  );
}

export default function Todo() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);

  const { selectedDate, setSelectedDate, routines, tasks, setTodos } =
    useTodoStore();

  const [searchParams] = useSearchParams();
  const [activeTab] = useState(() => {
    const tabParam = searchParams.get("tab");
    return tabParam === "task" ? "TASK" : "ROUTINE";
  });
  const [filterTab, setFilterTab] = useState("ALL");
  const [touchStart, setTouchStart] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState(0);
  const { showError } = useToastStore();
  const { user } = useUserStore();

  function getWeekStart(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day;
    return new Date(d.setDate(diff));
  }

  const [currentWeekStart, setCurrentWeekStart] = useState(() => {
    return getWeekStart(new Date(selectedDate));
  });

  function goToPrevWeek() {
    const prevWeek = addDays(currentWeekStart, -7);
    setCurrentWeekStart(prevWeek);
    const currentDay = new Date(selectedDate).getDay();
    const newSelectedDate = addDays(prevWeek, currentDay);
    setSelectedDate(formatDateToYMD(newSelectedDate));
  }

  function goToNextWeek() {
    const nextWeek = addDays(currentWeekStart, 7);
    setCurrentWeekStart(nextWeek);
    const currentDay = new Date(selectedDate).getDay();
    const newSelectedDate = addDays(nextWeek, currentDay);
    setSelectedDate(formatDateToYMD(newSelectedDate));
  }

  function goToToday() {
    const today = new Date();
    const todayString = formatDateToYMD(today);
    setSelectedDate(todayString);
    setCurrentWeekStart(getWeekStart(today));
  }

  function getWeekInfo() {
    const weekEnd = addDays(currentWeekStart, 6);
    const startMonth = currentWeekStart.getMonth() + 1;
    const endMonth = weekEnd.getMonth() + 1;

    if (startMonth === endMonth) {
      return `${startMonth}월`;
    } else {
      return `${startMonth}월 - ${endMonth}월`;
    }
  }

  useEffect(() => {
    async function fetchTodoList() {
      setIsLoading(true);
      try {
        const data = await getTodos(selectedDate);
        setTodos(data);
      } catch {
        showError("할 일을 불러오는데 실패했습니다.");
      } finally {
        setIsLoading(false);
      }
    }

    fetchTodoList();
  }, [selectedDate, setTodos, showError]);

  useEffect(() => {
    const selectedWeekStart = getWeekStart(new Date(selectedDate));
    if (
      formatDateToYMD(selectedWeekStart) !== formatDateToYMD(currentWeekStart)
    ) {
      setCurrentWeekStart(selectedWeekStart);
    }
  }, [selectedDate, currentWeekStart]);

  function getFilteredItems() {
    let items = [];

    switch (filterTab) {
      case "ROUTINE":
        items = routines;
        break;
      case "TASK":
        items = tasks;
        break;
      case "COMPLETED":
        items = [...routines, ...tasks].filter((item) => item.isCompleted);
        break;
      case "ALL":
      default:
        items = [...routines, ...tasks];
        break;
    }

    return items;
  }

  const currentItems = getFilteredItems();
  const incompletedItems = currentItems.filter((item) => !item.isCompleted);
  const completedItems = currentItems.filter((item) => item.isCompleted);

  function handleCardClick(id) {
    const returnTab = activeTab === "ROUTINE" ? "routine" : "task";

    if (user?.userRole === "GUARDIAN") {
      navigate(`/todo/${id}/edit?returnTab=${returnTab}`);
      return;
    }

    const allItems = [...tasks, ...routines];
    const clickedItem = allItems.find((item) => item.todoId === id);

    if (clickedItem?.hasOrder) {
      navigate(`/todo/${id}?type=sequence&returnTab=${returnTab}`);
    } else {
      navigate(`/todo/${id}?type=checklist&returnTab=${returnTab}`);
    }
  }

  function onTouchStart(e) {
    setTouchStart(e.touches[0].clientX);
    setIsDragging(false);
    setDragOffset(0);
  }

  function onTouchMove(e) {
    if (!touchStart) return;

    const currentTouch = e.touches[0].clientX;
    const diff = currentTouch - touchStart;

    if (Math.abs(diff) > 10 && !isDragging) {
      setIsDragging(true);
    }

    if (isDragging) {
      setDragOffset(diff * 0.6);
    }
  }

  function onTouchEnd() {
    if (!touchStart) return;

    const swipeThreshold = 80;

    if (isDragging && Math.abs(dragOffset) > swipeThreshold) {
      if (dragOffset > 0) {
        goToPrevWeek();
      } else {
        goToNextWeek();
      }
    }

    setDragOffset(0);
    setTouchStart(null);
    setIsDragging(false);
  }

  return (
    <div className="h-screen">
      <Header showTodoButton={true} />

      <div className="mb-4 flex w-full justify-center">
        <div className="w-full max-w-4xl px-4">
          <div className="rounded-xl border border-gray-300 bg-white/90 p-4 shadow-sm">
            <div className="mb-3 flex items-center justify-between">
              <button
                onClick={goToPrevWeek}
                className="flex h-8 w-8 items-center justify-center rounded-full bg-black transition-colors hover:bg-gray-600"
                aria-label="이전 주"
              >
                <BiChevronLeft size={20} className="text-white" />
              </button>

              <div className="text-center">
                <span className="mb-2 block text-sm font-medium text-black">
                  {getWeekInfo()}
                </span>
                <button
                  onClick={goToToday}
                  className="flex items-center gap-1 rounded-full bg-[#F3FDA3] px-3 py-1.5 text-xs font-medium text-black transition-colors hover:bg-[#F0F5A0]"
                  aria-label="오늘로 이동"
                  title="오늘 날짜로 이동"
                >
                  <BiHome size={14} />
                  오늘로 가기
                </button>
              </div>

              <button
                onClick={goToNextWeek}
                className="flex h-8 w-8 items-center justify-center rounded-full bg-black transition-colors hover:bg-gray-600"
                aria-label="다음 주"
              >
                <BiChevronRight size={20} className="text-white" />
              </button>
            </div>

            <div
              className={`flex w-full items-center justify-between transition-transform select-none ${
                isDragging ? "duration-75" : "duration-300 ease-out"
              }`}
              style={{
                transform: `translateX(${dragOffset}px)`,
              }}
              onTouchStart={onTouchStart}
              onTouchMove={onTouchMove}
              onTouchEnd={onTouchEnd}
            >
              {Array.from({ length: 7 }, (_, i) => {
                const date = addDays(currentWeekStart, i);
                const isSelected = formatDateToYMD(date) === selectedDate;
                const isToday =
                  formatDateToYMD(date) === formatDateToYMD(new Date());
                const dayNames = ["일", "월", "화", "수", "목", "금", "토"];

                return (
                  <button
                    key={`${formatDateToYMD(date)}-${i}`}
                    onClick={(e) => {
                      if (isDragging) {
                        e.preventDefault();
                        return;
                      }
                      setSelectedDate(formatDateToYMD(date));
                    }}
                    className="flex flex-1 flex-col items-center transition-all duration-300"
                  >
                    <div
                      className={`flex flex-col items-center rounded-xl px-3 py-2 transition-all duration-300 ${
                        isSelected
                          ? "scale-105 transform bg-[#F3FDA3] shadow-lg"
                          : "hover:bg-gray-50"
                      }`}
                    >
                      <span
                        className={`mb-2 text-xs font-medium ${
                          isSelected
                            ? "text-black"
                            : isToday
                              ? "text-orange-600"
                              : "text-gray-500"
                        }`}
                      >
                        {dayNames[date.getDay()]}
                      </span>
                      <div className="relative">
                        {isSelected && (
                          <div className="absolute -inset-x-2 -inset-y-1 transform rounded-xl bg-white opacity-90" />
                        )}
                        <span
                          className={`relative text-sm font-bold ${
                            isSelected
                              ? "text-black"
                              : isToday
                                ? "text-orange-600"
                                : "text-black"
                          }`}
                        >
                          {date.getDate()}
                        </span>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      <div className="mb-4 flex w-full justify-center">
        <div className="w-full max-w-4xl px-4">
          <div className="flex justify-center">
            <div className="flex w-full rounded-xl border border-gray-300 bg-white/80 p-1 shadow-sm">
              {[
                { key: "ALL", label: "모두" },
                { key: "ROUTINE", label: "루틴" },
                { key: "TASK", label: "할일" },
                { key: "COMPLETED", label: "완료" },
              ].map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => setFilterTab(tab.key)}
                  className={`flex-1 rounded-xl px-3 py-2 text-sm font-medium transition-colors ${
                    filterTab === tab.key
                      ? "bg-black text-white"
                      : "text-gray-600 hover:text-black"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-4xl px-4 pb-16">
        <div className="mb-4 flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <div className="border-primary h-8 w-8 animate-spin rounded-full border-b-2" />
            </div>
          ) : (
            <>
              {incompletedItems.length > 0 && (
                <div className="mb-4">
                  <div className="space-y-3">
                    {incompletedItems.map((item) => (
                      <Card
                        key={item.todoId}
                        id={item.todoId}
                        title={item.title}
                        hasOrder={item.hasOrder}
                        isCompleted={item.isCompleted}
                        todoType={item.type}
                        startTime={item.startTime}
                        endTime={item.endTime}
                        onClick={handleCardClick}
                      />
                    ))}
                  </div>
                </div>
              )}

              {completedItems.length > 0 && (
                <div className="mb-4">
                  <div className="space-y-4">
                    {completedItems.map((item) => (
                      <Card
                        key={item.todoId}
                        id={item.todoId}
                        title={item.title}
                        hasOrder={item.hasOrder}
                        isCompleted={item.isCompleted}
                        todoType={item.type}
                        startTime={item.startTime}
                        endTime={item.endTime}
                        onClick={handleCardClick}
                      />
                    ))}
                  </div>
                </div>
              )}

              {currentItems.length === 0 && <EmptyTodoState />}
            </>
          )}
        </div>
      </div>

      <TabBar />
    </div>
  );
}
