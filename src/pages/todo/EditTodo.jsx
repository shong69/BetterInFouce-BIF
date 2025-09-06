import { useState, useEffect, useMemo, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";

import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import PrimaryButton from "@components/ui/PrimaryButton";

import { HiPlus, HiX } from "react-icons/hi";

import { getTodoDetail, updateTodo } from "@services/todoService";
import { useToastStore, useLoadingStore, useTodoStore } from "@stores";

const TODO_TYPES = {
  TASK: "TASK",
  ROUTINE: "ROUTINE",
};

const REPEAT_FREQUENCY = {
  DAILY: "DAILY",
  WEEKLY: "WEEKLY",
};

const NOTIFICATION_OPTIONS = [
  { label: "정각", value: 0 },
  { label: "5분 전", value: 5 },
  { label: "10분 전", value: 10 },
  { label: "15분 전", value: 15 },
  { label: "30분 전", value: 30 },
  { label: "1시간 전", value: 60 },
];

const WEEK_DAYS = [
  { label: "월", value: "MONDAY" },
  { label: "화", value: "TUESDAY" },
  { label: "수", value: "WEDNESDAY" },
  { label: "목", value: "THURSDAY" },
  { label: "금", value: "FRIDAY" },
  { label: "토", value: "SATURDAY" },
  { label: "일", value: "SUNDAY" },
];

const MAX_SUBTODOS = 5;
const MAX_TITLE_LENGTH = 255;

export default function EditTodo() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { showSuccess, showError } = useToastStore();
  const { showLoading, hideLoading } = useLoadingStore();
  const { setNeedsRefresh } = useTodoStore();

  const [isLoading, setIsLoading] = useState(true);
  const [lastSaveTime, setLastSaveTime] = useState(0);
  const [originalData, setOriginalData] = useState(null);
  const timeInputRef = useRef(null);
  const [hasOrder, setHasOrder] = useState(false);
  const [formData, setFormData] = useState({
    title: "",
    type: TODO_TYPES.TASK,
    repeatFrequency: null,
    repeatDays: [],
    dueDate: null,
    dueTime: null,
    notificationEnabled: false,
    notificationTime: 0,
    subTodos: [],
  });
  const [validation, setValidation] = useState({
    title: { isValid: true, message: "" },
    subTodos: { isValid: true, message: "" },
    dueDate: { isValid: true, message: "" },
    dueTime: { isValid: true, message: "" },
  });

  function isRecentSave() {
    const now = Date.now();
    if (now - lastSaveTime < 300) {
      return true;
    }
    setLastSaveTime(now);
    return false;
  }

  function formatDateForInput(dateString) {
    if (!dateString) return "";

    if (Array.isArray(dateString)) {
      const [year, month, day] = dateString;
      return `${year}-${month.toString().padStart(2, "0")}-${day
        .toString()
        .padStart(2, "0")}`;
    }

    if (typeof dateString === "string") {
      return new Date(dateString).toISOString().split("T")[0];
    }

    return "";
  }

  function formatTimeForInput(timeString) {
    if (!timeString) return "";

    if (Array.isArray(timeString)) {
      const [hours, minutes] = timeString;
      return `${hours.toString().padStart(2, "0")}:${minutes
        .toString()
        .padStart(2, "0")}`;
    }

    if (typeof timeString === "string" && timeString.includes(":")) {
      return timeString.length > 5 ? timeString.substring(0, 5) : timeString;
    }

    return "";
  }

  function parseDateFromInput(dateString) {
    return dateString || null;
  }

  function parseTimeFromInput(timeString) {
    return timeString ? `${timeString}:00` : null;
  }

  const isFormValid = useMemo(() => {
    return (
      Object.values(validation).every((field) => field.isValid) &&
      formData.title.trim().length > 0
    );
  }, [validation, formData.title]);

  const isFormChanged = useMemo(() => {
    if (!originalData) return false;
    return JSON.stringify(formData) !== JSON.stringify(originalData);
  }, [formData, originalData]);

  useEffect(() => {
    async function fetchTodoDetail() {
      try {
        const data = await getTodoDetail(id);
        const isOrdered =
          data.subTodos &&
          data.subTodos.length > 0 &&
          data.subTodos.every((sub) => sub.sortOrder > 0);
        setHasOrder(isOrdered);

        const formattedData = {
          title: data.title || "",
          type: data.type || TODO_TYPES.TASK,
          repeatFrequency: data.repeatFrequency || null,
          repeatDays: data.repeatDays || [],
          dueDate: data.dueDate || null,
          dueTime: data.dueTime || null,
          notificationEnabled: data.notificationEnabled || false,
          notificationTime: data.notificationTime || 0,
          subTodos:
            data.subTodos?.map((sub) => ({
              subTodoId: sub.subTodoId || null,
              title: sub.title || "",
              sortOrder: sub.sortOrder || 0,
            })) || [],
        };

        setFormData(formattedData);
        setOriginalData({ ...formattedData });
      } catch {
        showError("할 일을 불러오는데 실패했습니다.");
        navigate("/");
      } finally {
        setIsLoading(false);
      }
    }

    fetchTodoDetail();
  }, [id, navigate, showError]);

  function validateTitle(title) {
    const trimmed = title.trim();
    if (!trimmed) {
      return { isValid: false, message: "제목을 입력해주세요." };
    }
    if (trimmed.length > MAX_TITLE_LENGTH) {
      return {
        isValid: false,
        message: `제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다.`,
      };
    }
    return { isValid: true, message: "" };
  }

  function validateSubTodos(subTodos) {
    if (subTodos.length > MAX_SUBTODOS) {
      return {
        isValid: false,
        message: `세부 할일은 최대 ${MAX_SUBTODOS}개까지 가능합니다.`,
      };
    }

    for (let i = 0; i < subTodos.length; i++) {
      if (!subTodos[i].title.trim()) {
        return {
          isValid: false,
          message: `${i + 1}번째 세부 할일을 입력해주세요.`,
        };
      }
    }

    return { isValid: true, message: "" };
  }

  function validateDueDate(date) {
    if (date) {
      const selectedDate = new Date(date);
      const today = new Date(
        new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" }),
      );

      today.setHours(0, 0, 0, 0);
      selectedDate.setHours(0, 0, 0, 0);

      if (selectedDate < today) {
        return { isValid: false, message: "마감일은 오늘 이후여야 합니다." };
      }
    }
    return { isValid: true, message: "" };
  }

  function validateDueTime(date, time) {
    if (date && time) {
      const selectedDate = new Date(date);
      const today = new Date(
        new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" }),
      );

      today.setHours(0, 0, 0, 0);
      selectedDate.setHours(0, 0, 0, 0);

      if (selectedDate.getTime() === today.getTime()) {
        const currentTime = new Date(
          new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" }),
        );

        let timeString = "";
        if (typeof time === "string") {
          timeString = time;
        } else if (time) {
          timeString = formatTimeForInput(time);
        }

        if (timeString && timeString.includes(":")) {
          const [hours, minutes] = timeString.split(":");
          const selectedDateTime = new Date(
            new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" }),
          );
          selectedDateTime.setHours(
            parseInt(hours, 10),
            parseInt(minutes, 10),
            0,
            0,
          );

          if (selectedDateTime <= currentTime) {
            return {
              isValid: false,
              message: "현재 시간 이후여야 합니다.",
            };
          }
        }
      }
    }
    return { isValid: true, message: "" };
  }

  function handleTitleChange(e) {
    const newTitle = e.target.value;
    setFormData((prev) => ({ ...prev, title: newTitle }));

    const titleValidation = validateTitle(newTitle);
    setValidation((prev) => ({ ...prev, title: titleValidation }));
  }

  function handleTypeChange(newType) {
    const type = newType === "routine" ? TODO_TYPES.ROUTINE : TODO_TYPES.TASK;
    setFormData((prev) => ({
      ...prev,
      type,
      repeatFrequency:
        type === TODO_TYPES.ROUTINE ? REPEAT_FREQUENCY.DAILY : null,
      repeatDays: [],
      dueDate:
        type === TODO_TYPES.TASK
          ? prev.dueDate ||
            new Date(
              new Date().toLocaleString("en-US", { timeZone: "Asia/Seoul" }),
            )
              .toISOString()
              .split("T")[0]
          : null,
    }));
  }

  function handleRepeatFrequencyChange(frequency) {
    const repeatFreq =
      frequency === "daily" ? REPEAT_FREQUENCY.DAILY : REPEAT_FREQUENCY.WEEKLY;
    setFormData((prev) => ({
      ...prev,
      repeatFrequency: repeatFreq,
      repeatDays: repeatFreq === REPEAT_FREQUENCY.DAILY ? [] : prev.repeatDays,
    }));
  }

  function handleDayToggle(dayValue) {
    setFormData((prev) => ({
      ...prev,
      repeatDays: prev.repeatDays.includes(dayValue)
        ? prev.repeatDays.filter((d) => d !== dayValue)
        : [...prev.repeatDays, dayValue],
    }));
  }

  function handleDateChange(e) {
    const newDate = parseDateFromInput(e.target.value);
    setFormData((prev) => ({ ...prev, dueDate: newDate }));

    const dateValidation = validateDueDate(newDate);
    const timeValidation = validateDueTime(newDate, formData.dueTime);

    setValidation((prev) => ({
      ...prev,
      dueDate: dateValidation,
      dueTime: timeValidation,
    }));
  }

  function handleTimeChange(e) {
    const newTime = parseTimeFromInput(e.target.value);
    setFormData((prev) => ({ ...prev, dueTime: newTime }));

    const timeValidation = validateDueTime(formData.dueDate, newTime);

    setValidation((prev) => ({
      ...prev,
      dueTime: timeValidation,
    }));

    if (!timeValidation.isValid) {
      setTimeout(() => {
        timeInputRef.current?.focus();
      }, 100);
    }
  }

  function handleNotificationToggle() {
    setFormData((prev) => ({
      ...prev,
      notificationEnabled: !prev.notificationEnabled,
      notificationTime: !prev.notificationEnabled ? 0 : prev.notificationTime,
    }));
  }

  function handleNotificationTimeChange(e) {
    const time = parseInt(e.target.value, 10);
    setFormData((prev) => ({ ...prev, notificationTime: time }));
  }

  function handleSubTodoChange(index, newTitle) {
    const newSubTodos = [...formData.subTodos];
    newSubTodos[index] = { ...newSubTodos[index], title: newTitle };
    setFormData((prev) => ({ ...prev, subTodos: newSubTodos }));

    const subTodosValidation = validateSubTodos(newSubTodos);
    setValidation((prev) => ({ ...prev, subTodos: subTodosValidation }));
  }

  function handleAddSubTodo() {
    if (formData.subTodos.length >= MAX_SUBTODOS) {
      showError(`세부 할일은 최대 ${MAX_SUBTODOS}개까지 추가할 수 있습니다.`);
      return;
    }

    const newSubTodo = {
      subTodoId: null,
      title: "",
      sortOrder: hasOrder ? formData.subTodos.length + 1 : 0,
    };

    setFormData((prev) => ({
      ...prev,
      subTodos: [...prev.subTodos, newSubTodo],
    }));
  }

  function handleRemoveSubTodo(index) {
    const newSubTodos = formData.subTodos
      .filter((_, i) => i !== index)
      .map((item, i) => ({ ...item, sortOrder: hasOrder ? i + 1 : 0 }));

    setFormData((prev) => ({ ...prev, subTodos: newSubTodos }));

    const subTodosValidation = validateSubTodos(newSubTodos);
    setValidation((prev) => ({ ...prev, subTodos: subTodosValidation }));
  }

  async function handleSave() {
    if (isRecentSave()) return;

    const titleValidation = validateTitle(formData.title);
    const subTodosValidation = validateSubTodos(formData.subTodos);
    const dateValidation = validateDueDate(formData.dueDate);
    const timeValidation = validateDueTime(formData.dueDate, formData.dueTime);

    setValidation({
      title: titleValidation,
      subTodos: subTodosValidation,
      dueDate: dateValidation,
      dueTime: timeValidation,
    });

    if (
      !titleValidation.isValid ||
      !subTodosValidation.isValid ||
      !dateValidation.isValid ||
      !timeValidation.isValid
    ) {
      showError("입력 내용을 확인해주세요.");

      if (!timeValidation.isValid) {
        setTimeout(() => {
          timeInputRef.current?.focus();
        }, 100);
      }
      return;
    }

    showLoading("할 일을 저장하는 중...");

    try {
      const updateRequest = {
        title: formData.title.trim(),
        type: formData.type,
        repeatFrequency: formData.repeatFrequency,
        repeatDays: formData.repeatDays.length > 0 ? formData.repeatDays : null,
        dueDate: formData.dueDate,
        dueTime: formData.dueTime,
        notificationEnabled: formData.notificationEnabled,
        notificationTime: formData.notificationEnabled
          ? formData.notificationTime
          : null,
        subTodos: formData.subTodos
          .filter((sub) => sub.title.trim())
          .map((sub, index) => ({
            subTodoId: sub.subTodoId,
            title: sub.title.trim(),
            sortOrder: hasOrder ? index + 1 : 0,
          })),
      };

      await updateTodo(id, updateRequest);
      setNeedsRefresh(true);
      showSuccess("할 일이 수정되었습니다.");
      navigate(-1);
    } catch (error) {
      if (error?.response?.status === 401) {
        showError("인증 오류가 발생했습니다. 다시 로그인해주세요.");
      } else if (error?.response?.data?.code === "SUBTODO_COUNT_INSUFFICIENT") {
        showError(error.response.data.message);
      } else {
        const errorMessage =
          error?.response?.data?.message || error?.message?.includes("network")
            ? "네트워크 연결을 확인해주세요."
            : "할 일 수정에 실패했습니다. 다시 시도해주세요.";
        showError(errorMessage);
      }
    } finally {
      hideLoading();
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen pb-10">
        <Header />
        <div className="mx-auto max-w-md px-4 pt-4">
          <div className="mt-4 animate-pulse space-y-4">
            <div className="h-4 w-3/4 rounded bg-gray-200" />
            <div className="h-20 rounded bg-gray-200" />
            <div className="h-16 rounded bg-gray-200" />
          </div>
        </div>
        <TabBar />
      </div>
    );
  }

  return (
    <div className="h-screen">
      <Header />

      <div className="mx-auto max-w-4xl p-6 pb-15 md:pb-15">
        <div className="mb-6">
          <h2 className="text-md mb-3 text-black">유형</h2>
          <div className="flex overflow-hidden rounded-xl shadow-sm">
            <button
              onClick={() => handleTypeChange("routine")}
              className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                formData.type === TODO_TYPES.ROUTINE
                  ? "bg-light-orange text-black"
                  : "bg-white text-gray-600 hover:bg-gray-50"
              }`}
            >
              루틴
            </button>
            <button
              onClick={() => handleTypeChange("task")}
              className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                formData.type === TODO_TYPES.TASK
                  ? "bg-light-orange text-black"
                  : "bg-white text-gray-600 hover:bg-gray-50"
              }`}
            >
              할 일
            </button>
          </div>
        </div>

        <div className="mb-6">
          <h2 className="text-md mb-3 text-black">할 일</h2>
          <div className="space-y-2">
            <input
              type="text"
              value={formData.title}
              onChange={handleTitleChange}
              placeholder="할 일을 입력해주세요"
              className={`focus:ring-opacity-50 w-full rounded-lg border p-3 text-base transition-colors focus:ring-2 focus:ring-blue-500 focus:outline-none ${
                validation.title.isValid
                  ? "border-gray-200 focus:border-gray-300"
                  : "border-warning focus:border-red-300"
              }`}
              maxLength={MAX_TITLE_LENGTH}
            />
            <div className="flex justify-between text-xs">
              <div>
                {!validation.title.isValid && (
                  <span className="text-warning">
                    {validation.title.message}
                  </span>
                )}
              </div>
              <span
                className={`${formData.title.length > MAX_TITLE_LENGTH * 0.9 ? "text-orange" : "text-gray-400"}`}
              >
                {formData.title.length}/{MAX_TITLE_LENGTH}
              </span>
            </div>
          </div>
        </div>

        <div className="mb-6">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-md text-black">세부 할 일</h2>
            <button
              onClick={handleAddSubTodo}
              disabled={formData.subTodos.length >= MAX_SUBTODOS}
              className={`flex items-center gap-1 rounded-lg px-3 py-1 text-sm font-medium transition-colors ${
                formData.subTodos.length >= MAX_SUBTODOS
                  ? "cursor-not-allowed bg-gray-100 text-gray-400"
                  : "bg-black text-white hover:bg-blue-200"
              }`}
            >
              <HiPlus size={16} />
              추가 ({formData.subTodos.length}/{MAX_SUBTODOS})
            </button>
          </div>

          <div className="space-y-3 rounded-xl py-2">
            {formData.subTodos.length === 0 ? (
              <p className="py-4 text-center text-sm text-gray-400">
                세부 할일을 추가해보세요
              </p>
            ) : (
              formData.subTodos.map((subTodo, index) => (
                <div
                  key={subTodo.subTodoId}
                  className="flex items-center gap-4"
                >
                  <span className="bg-light-orange flex h-7 w-7 min-w-[1.75rem] items-center justify-center rounded-full text-xs font-medium text-black">
                    {index + 1}
                  </span>
                  <input
                    type="text"
                    value={subTodo.title}
                    onChange={(e) => handleSubTodoChange(index, e.target.value)}
                    placeholder={`세부 할일 ${index + 1}`}
                    className="flex-1 rounded-lg border border-gray-200 p-3 text-base focus:border-gray-200 focus:outline-none"
                  />
                  <button
                    onClick={() => handleRemoveSubTodo(index)}
                    className="hover:text-warning flex h-8 w-8 items-center justify-center rounded-lg text-gray-400 hover:bg-gray-100"
                  >
                    <HiX size={16} />
                  </button>
                </div>
              ))
            )}

            {!validation.subTodos.isValid && (
              <p className="text-warning text-sm">
                {validation.subTodos.message}
              </p>
            )}
          </div>
        </div>

        {formData.type === TODO_TYPES.ROUTINE && (
          <div className="mb-6">
            <h2 className="text-md mb-3 text-black">반복</h2>
            <div className="space-y-4 rounded-xl">
              <div className="flex overflow-hidden rounded-xl shadow-sm">
                <button
                  onClick={() => handleRepeatFrequencyChange("daily")}
                  className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                    formData.repeatFrequency === REPEAT_FREQUENCY.DAILY
                      ? "bg-light-orange text-black"
                      : "bg-white text-black hover:bg-gray-50"
                  }`}
                >
                  매일
                </button>
                <button
                  onClick={() => handleRepeatFrequencyChange("weekly")}
                  className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                    formData.repeatFrequency === REPEAT_FREQUENCY.WEEKLY
                      ? "bg-light-orange text-black"
                      : "bg-white text-black hover:bg-gray-50"
                  }`}
                >
                  매주
                </button>
              </div>

              {formData.repeatFrequency === REPEAT_FREQUENCY.WEEKLY && (
                <div>
                  <h3 className="mb-2 text-sm text-black">반복 요일</h3>
                  <div className="flex flex-wrap justify-around gap-2">
                    {WEEK_DAYS.map((day) => (
                      <button
                        key={day.value}
                        onClick={() => handleDayToggle(day.value)}
                        className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                          formData.repeatDays.includes(day.value)
                            ? "bg-light-orange text-black"
                            : "bg-white text-black hover:bg-gray-200"
                        }`}
                      >
                        {day.label}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {formData.type === TODO_TYPES.TASK && (
          <div className="mb-6">
            <h2 className="text-md mb-3 text-black">날짜</h2>
            <div className="space-y-2">
              <input
                type="date"
                value={formatDateForInput(formData.dueDate)}
                onChange={handleDateChange}
                className={`focus:ring-opacity-50 w-full rounded-lg border p-3 text-base focus:ring-2 focus:ring-blue-500 focus:outline-none ${
                  validation.dueDate.isValid
                    ? "border-gray-200 focus:border-gray-300"
                    : "border-warning focus:border-red-300"
                }`}
              />
              {!validation.dueDate.isValid && (
                <p className="text-warning text-sm">
                  {validation.dueDate.message}
                </p>
              )}
            </div>
          </div>
        )}

        <div className="mb-6">
          <h2 className="text-md mb-3 text-black">시간</h2>
          <div className="space-y-2">
            <input
              ref={timeInputRef}
              type="time"
              value={formatTimeForInput(formData.dueTime)}
              onChange={handleTimeChange}
              className={`focus:ring-opacity-50 w-full rounded-lg border p-3 text-base focus:border-gray-300 focus:ring-2 focus:ring-blue-500 focus:outline-none ${
                validation.dueTime.isValid
                  ? "border-gray-200"
                  : "border-warning focus:border-red-300"
              }`}
            />
            {!validation.dueTime.isValid && (
              <p className="text-warning text-sm">
                {validation.dueTime.message}
              </p>
            )}
          </div>
        </div>

        <div className="mb-6">
          <label className="mb-3 flex cursor-pointer items-center gap-2">
            <h2 className="text-md text-black">알림 설정</h2>
            <div className="flex items-center justify-center">
              <input
                type="checkbox"
                checked={formData.notificationEnabled}
                onChange={handleNotificationToggle}
                className="focus:ring-opacity-50 mx-2 h-5 w-5 rounded border-gray-300 text-blue-500 transition-colors focus:ring-blue-500"
              />
            </div>
          </label>

          {formData.notificationEnabled && (
            <div className="rounded-xl">
              <label className="block">
                <select
                  value={formData.notificationTime}
                  onChange={handleNotificationTimeChange}
                  className="focus:ring-opacity-50 w-full appearance-none rounded-lg border border-gray-200 bg-white p-3 text-base transition-colors focus:border-gray-300 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  {NOTIFICATION_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          )}
        </div>
      </div>

      <div className="fixed right-0 bottom-4 left-0 px-4 pt-6">
        <div className="mx-auto max-w-4xl px-2">
          <PrimaryButton
            title="할 일 수정하기"
            onClick={handleSave}
            disabled={!isFormValid || !isFormChanged}
          />
        </div>
      </div>
    </div>
  );
}
