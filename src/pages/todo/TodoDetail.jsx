import { useState, useEffect, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";

import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import DateBox from "@components/ui/DateBox";
import BackButton from "@components/ui/BackButton";
import ProgressBar from "@components/ui/ProgressBar";
import CompletionMessage from "@pages/todo/CompletionMessage";

import { IoCheckmark } from "react-icons/io5";

import {
  getTodoDetail,
  updateSubTodoCompletion,
  completeTodo,
  uncompleteTodo,
  updateTodoStep,
} from "@services/todoService";
import { useToastStore, useLoadingStore } from "@stores";
import { getRandomColorByTitle } from "@utils/colorUtils";

export default function TodoDetail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [todoData, setTodoData] = useState(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [lastClickTime, setLastClickTime] = useState(0);

  const typeFromUrl = new URLSearchParams(window.location.search).get("type");
  const returnTab = new URLSearchParams(window.location.search).get(
    "returnTab",
  );
  const { showError } = useToastStore();
  const { showLoading, hideLoading } = useLoadingStore();

  const colors = useMemo(() => {
    if (!todoData?.title || !todoData?.todoId) {
      return {
        title: "text-gray-800",
        tag: "bg-gray-100 text-gray-600",
        button: "bg-blue-500 text-white hover:bg-blue-600",
      };
    }
    return getRandomColorByTitle(todoData.title, todoData.todoId);
  }, [todoData?.title, todoData?.todoId]);

  const progressData = useMemo(() => {
    if (!todoData?.subTodos) {
      return { completedCount: 0, totalCount: 0, progressPercentage: 0 };
    }

    const completedCount = todoData.subTodos.filter(
      (sub) => sub.isCompleted,
    ).length;
    const totalCount = todoData.subTodos.length;
    const progressPercentage =
      totalCount > 0 ? (completedCount / totalCount) * 100 : 0;

    return { completedCount, totalCount, progressPercentage };
  }, [todoData?.subTodos]);

  const completionStatus = useMemo(() => {
    if (!todoData)
      return { isChecklistCompleted: false, isSequenceCompleted: false };

    const isChecklistCompleted =
      typeFromUrl === "checklist" &&
      progressData.completedCount === progressData.totalCount &&
      progressData.totalCount > 0;

    const isSequenceCompleted =
      typeFromUrl === "sequence" &&
      currentStep === todoData.subTodos.length - 1;

    return { isChecklistCompleted, isSequenceCompleted };
  }, [typeFromUrl, progressData, currentStep, todoData]);

  function isRecentClick() {
    const now = Date.now();
    if (now - lastClickTime < 300) {
      return true;
    }
    setLastClickTime(now);
    return false;
  }

  useEffect(() => {
    showLoading("상세 할 일을 가져오는 중...");

    async function fetchTodoDetail() {
      try {
        const data = await getTodoDetail(id);
        setTodoData(data);

        if (data.hasOrder) {
          setCurrentStep(data.currentStep);
        }
      } catch {
        showError("할 일을 불러오는데 실패했습니다.");
        navigate("/");
      } finally {
        hideLoading();
      }
    }

    fetchTodoDetail();
  }, [id, navigate, showLoading, hideLoading, showError]);

  async function toggleSubTodoComplete(subTodoId) {
    if (isRecentClick()) return;

    const subTodoIndex = todoData.subTodos.findIndex(
      (sub) => sub.subTodoId === subTodoId,
    );

    if (subTodoIndex === -1) {
      showError("세부 할 일이 없습니다!");
      return;
    }

    const originalSubTodo = todoData.subTodos[subTodoIndex];
    const newIsCompleted = !originalSubTodo.isCompleted;

    const updatedSubTodos = [...todoData.subTodos];
    updatedSubTodos[subTodoIndex] = {
      ...originalSubTodo,
      isCompleted: newIsCompleted,
    };

    let shouldBeCompleted;

    if (todoData.hasOrder) {
      const lastSubTodo = todoData.subTodos
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .pop();
      shouldBeCompleted =
        newIsCompleted && originalSubTodo.subTodoId === lastSubTodo.subTodoId;
    } else {
      shouldBeCompleted = updatedSubTodos.every((s) => s.isCompleted);
    }

    const previousTodoData = todoData;
    const previousTodoCompleted = todoData.isCompleted;

    setTodoData((prev) => ({
      ...prev,
      subTodos: updatedSubTodos,
      isCompleted: shouldBeCompleted,
    }));

    try {
      await updateSubTodoCompletion(todoData.todoId, subTodoId, newIsCompleted);

      if (previousTodoCompleted !== shouldBeCompleted) {
        if (shouldBeCompleted) {
          await completeTodo(todoData.todoId);
        } else {
          await uncompleteTodo(todoData.todoId);
        }
      }
    } catch {
      showError("상태 변경에 실패했습니다.");
      setTodoData(previousTodoData);
    }
  }

  async function handlePreviousStep() {
    if (currentStep <= 0 || isRecentClick()) {
      return;
    }

    const wasLastStep = currentStep === todoData.subTodos.length - 1;
    const newStep = currentStep - 1;
    const previousStep = currentStep;
    const previousIsCompleted = todoData.isCompleted;

    setCurrentStep(newStep);

    try {
      await updateTodoStep(id, newStep);
      if (wasLastStep) {
        await uncompleteTodo(id);
        setTodoData((prev) => ({ ...prev, isCompleted: false }));
      }
    } catch {
      showError("단계 저장에 실패했습니다.");
      setCurrentStep(previousStep);
      setTodoData((prev) => ({ ...prev, isCompleted: previousIsCompleted }));
    }
  }

  async function handleNextStep() {
    if (isRecentClick() || currentStep >= todoData.subTodos.length - 1) {
      return;
    }

    const newStep = currentStep + 1;
    const previousStep = currentStep;
    const previousIsCompleted = todoData.isCompleted;

    setCurrentStep(newStep);

    try {
      await updateTodoStep(id, newStep);
      const isNowLastStep = newStep === todoData.subTodos.length - 1;
      if (isNowLastStep) {
        await completeTodo(id);
        setTodoData((prev) => ({ ...prev, isCompleted: true }));
      }
    } catch {
      showError("단계 저장에 실패했습니다.");
      setCurrentStep(previousStep);
      setTodoData((prev) => ({ ...prev, isCompleted: previousIsCompleted }));
    }
  }

  function renderTodoHeader() {
    return (
      <div className="mb-4 flex items-center justify-between">
        <h2 className={`text-lg font-medium ${colors.title}`}>
          {todoData.title}
        </h2>
        <span className={`rounded-full px-3 py-1 text-sm ${colors.tag}`}>
          {todoData.hasOrder ? "순서 있음" : "체크리스트"}
        </span>
      </div>
    );
  }

  if (!todoData) {
    return (
      <div className="min-h-screen pb-20">
        <Header />
        <div className="mx-auto max-w-md px-4 pt-4">
          <DateBox />
          <div className="mt-4 mb-6">
            <BackButton />
          </div>
          <div className="rounded-xl border-2 border-gray-200 bg-white p-8">
            <div className="animate-pulse space-y-4">
              <div className="h-6 rounded bg-gray-200" />
              <div className="space-y-2">
                <div className="h-4 w-3/4 rounded bg-gray-200" />
                <div className="h-4 w-1/2 rounded bg-gray-200" />
              </div>
            </div>
          </div>
        </div>
        <TabBar />
      </div>
    );
  }

  function renderChecklist() {
    return (
      <div className="rounded-xl border-2 border-gray-200 bg-white p-4">
        {renderTodoHeader()}

        <div className="mb-4 space-y-3">
          {todoData.subTodos.map((item) => {
            return (
              <label
                key={item.subTodoId}
                className={`flex cursor-pointer items-center gap-3 rounded-lg p-2 transition-colors hover:bg-gray-50 ${
                  item.isCompleted ? "line-through" : ""
                }`}
              >
                <div className="relative">
                  <input
                    type="checkbox"
                    checked={item.isCompleted}
                    onChange={() => toggleSubTodoComplete(item.subTodoId)}
                    className="sr-only"
                  />
                  <div
                    className={`flex h-5 w-5 items-center justify-center rounded border-2 transition-all duration-200 ${
                      item.isCompleted
                        ? "bg-secondary border-transparent"
                        : "border-gray-300 bg-white hover:border-gray-400"
                    }`}
                  >
                    <IoCheckmark
                      className={`h-5 w-5 text-white transition-all duration-200 ${
                        item.isCompleted
                          ? "scale-100 opacity-100"
                          : "scale-75 opacity-0"
                      }`}
                    />
                  </div>
                </div>
                <span
                  className={`text-gray-700 ${item.isCompleted ? "text-gray-400 line-through" : ""}`}
                >
                  {item.title}
                </span>
              </label>
            );
          })}
        </div>

        <div className="mb-12">
          <ProgressBar
            variant="percentage"
            progress={progressData.progressPercentage}
          />
        </div>
      </div>
    );
  }

  function renderSequence() {
    const currentSubTodo = todoData.subTodos[currentStep];

    return (
      <div className="rounded-xl border-2 border-gray-200 bg-white p-4">
        {renderTodoHeader()}

        <div className="mb-6 text-center">
          <div className="mb-2 text-sm text-gray-500">
            {currentStep + 1}/{todoData.subTodos.length}
          </div>
          <div className={`rounded-2xl ${colors.tag} p-6`}>
            <h3 className={`text-xl font-medium ${colors.title}`}>
              {currentSubTodo?.title}
            </h3>
          </div>
        </div>

        <div className="mb-12">
          <ProgressBar
            variant="step"
            currentStep={currentStep + 1}
            totalSteps={todoData.subTodos.length}
          />
        </div>

        <div className="flex justify-between gap-3">
          <button
            onClick={handlePreviousStep}
            disabled={currentStep === 0}
            className={`flex-1 rounded-xl px-4 py-3 font-medium transition-colors ${
              currentStep === 0
                ? "cursor-not-allowed bg-gray-100 text-gray-400"
                : colors.tag
            }`}
          >
            이전
          </button>
          <button
            onClick={handleNextStep}
            disabled={!todoData || currentStep >= todoData.subTodos.length - 1}
            className={`flex-1 rounded-xl px-4 py-3 font-medium transition-colors ${
              currentStep === todoData.subTodos.length - 1
                ? "cursor-not-allowed bg-gray-100 text-gray-400"
                : colors.button
            }`}
          >
            다음
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen pb-20">
      <Header />

      <div className="mx-auto max-w-md px-4 pt-4">
        <DateBox />

        <div className="mt-4 mb-6">
          <BackButton
            onClick={() => {
              if (returnTab) {
                navigate(`/?tab=${returnTab}`);
              } else {
                navigate(-1);
              }
            }}
          />
        </div>

        {typeFromUrl === "checklist" ? renderChecklist() : renderSequence()}

        {(completionStatus.isChecklistCompleted ||
          completionStatus.isSequenceCompleted) && (
          <CompletionMessage type={typeFromUrl} />
        )}
      </div>

      <TabBar />
    </div>
  );
}
