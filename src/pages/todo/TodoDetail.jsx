import { useState, useEffect, useMemo } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";

import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import Modal from "@components/ui/Modal";
import EditButton from "@components/ui/EditButton";
import DeleteButton from "@components/ui/DeleteButton";

import { IoCheckmark } from "react-icons/io5";
import { FaCircle, FaCheckCircle } from "react-icons/fa";
import { HiOutlineTrash } from "react-icons/hi";

import {
  getTodoDetail,
  updateSubTodoCompletion,
  updateTodoStep,
  deleteTodo,
} from "@services/todoService";
import { useToastStore, useUserStore, useTodoStore } from "@stores";

export default function TodoDetail() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();
  const [todoData, setTodoData] = useState(null);
  const { selectedDate, updateTodoCompletion } = useTodoStore();
  const [currentStep, setCurrentStep] = useState(0);
  const [lastClickTime, setLastClickTime] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    todoTitle: "",
  });

  const { showError } = useToastStore();
  const { user } = useUserStore();

  const colors = useMemo(() => {
    if (!todoData?.type) {
      return {
        title: "text-black",
        tag: "bg-gray-100 text-black",
        button: "bg-black text-white hover:bg-gray-800",
        completeButton: "bg-[#FFB347] text-black hover:bg-[#F0B937]",
        checkbox: "#FFB347",
        background: "bg-orange-100",
      };
    }

    const isRoutine = todoData.type === "ROUTINE";
    return {
      title: "text-black",
      tag: `${isRoutine ? "bg-orange" : "bg-primary"} text-black`,
      button: "bg-black text-white hover:bg-gray-800",
      completeButton: isRoutine
        ? "bg-orange text-black hover:bg-[#F0B937]"
        : "bg-primary text-white hover:bg-[#45A049]",
      checkbox: isRoutine ? "#FFB347" : "#4CAF50",
      background: isRoutine ? "bg-orange-100" : "bg-green-100",
    };
  }, [todoData?.type]);

  function isRecentClick() {
    const now = Date.now();
    if (now - lastClickTime < 300) {
      return true;
    }
    setLastClickTime(now);
    return false;
  }

  useEffect(() => {
    async function fetchInitialData() {
      setIsLoading(true);
      try {
        const data = await getTodoDetail(id, selectedDate);
        setTodoData(data);
        if (data.hasOrder) {
          setCurrentStep(data.currentStep || 0);
        }
      } catch {
        showError("할 일을 불러오는데 실패했습니다.");
        navigate("/");
      } finally {
        setIsLoading(false);
      }
    }
    fetchInitialData();
  }, [id, selectedDate, navigate, showError]);

  useEffect(() => {
    const handleVisibilityChange = async () => {
      if (!document.hidden) {
        const data = await getTodoDetail(id, selectedDate);
        setTodoData(data);
        if (data.hasOrder) {
          setCurrentStep(data.currentStep || 0);
        }
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [id, selectedDate]);

  async function toggleSubTodoComplete(subTodoId) {
    if (user?.userRole === "GUARDIAN") {
      showError("Guardian은 할 일을 완료/미완료할 수 없습니다.");
      return;
    }

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

    const shouldBeCompleted = updatedSubTodos.every((s) => s.isCompleted);

    const previousTodoData = todoData;

    setTodoData((prev) => ({
      ...prev,
      subTodos: updatedSubTodos,
      isCompleted: shouldBeCompleted,
    }));

    try {
      await updateSubTodoCompletion(
        todoData.todoId,
        subTodoId,
        newIsCompleted,
        selectedDate,
      );

      if (previousTodoData.isCompleted !== shouldBeCompleted) {
        await updateTodoCompletion(
          todoData.todoId,
          shouldBeCompleted,
          selectedDate,
        );
      }

      const updatedData = await getTodoDetail(id, selectedDate);
      setTodoData(updatedData);
    } catch {
      showError("상태 변경에 실패했습니다.");
      setTodoData(previousTodoData);
    }
  }

  async function handlePreviousStep() {
    if (user?.userRole === "GUARDIAN") {
      showError("Guardian은 할 일을 완료/미완료할 수 없습니다.");
      return;
    }

    if (isRecentClick()) {
      return;
    }

    let newStep;
    if (currentStep === 0) {
      if (todoData.isCompleted) {
        newStep = todoData.subTodos.length - 1;
      } else {
        newStep = 0;
      }
    } else {
      newStep = currentStep - 1;
    }

    if (newStep === currentStep) {
      return;
    }

    const previousStep = currentStep;
    const previousIsCompleted = todoData.isCompleted;

    setCurrentStep(newStep);

    try {
      await updateTodoStep(id, newStep);
      const updatedData = await getTodoDetail(id, selectedDate);
      setTodoData(updatedData);
      setCurrentStep(newStep);
    } catch {
      showError("단계 저장에 실패했습니다.");
      setCurrentStep(previousStep);
      if (todoData.type !== "ROUTINE") {
        setTodoData((prev) => ({ ...prev, isCompleted: previousIsCompleted }));
      }
    }
  }

  async function handleNextStep() {
    if (user?.userRole === "GUARDIAN") {
      showError("Guardian은 할 일을 완료/미완료할 수 없습니다.");
      return;
    }

    if (isRecentClick()) {
      return;
    }

    let newStep;
    if (currentStep === todoData.subTodos.length - 1) {
      if (todoData.isCompleted) {
        newStep = 0;
      } else {
        newStep = todoData.subTodos.length - 1;
      }
    } else {
      newStep = currentStep + 1;
    }

    if (newStep === currentStep) {
      return;
    }

    const previousStep = currentStep;

    setCurrentStep(newStep);

    if (!todoData.isCompleted) {
      try {
        await updateTodoStep(id, newStep);
        const updatedData = await getTodoDetail(id, selectedDate);
        setTodoData(updatedData);
        setCurrentStep(newStep);
      } catch {
        showError("단계 저장에 실패했습니다.");
        setCurrentStep(previousStep);
      }
    }
  }

  async function handleCompleteTodo() {
    if (user?.userRole === "GUARDIAN") {
      showError("Guardian은 할 일을 완료/미완료할 수 없습니다.");
      return;
    }

    if (isRecentClick()) {
      return;
    }

    if (todoData.hasOrder && currentStep !== todoData.subTodos.length - 1) {
      showError("모든 단계를 완료해야 할일을 완료할 수 있습니다.");
      return;
    }

    try {
      if (todoData.hasOrder && todoData.currentStep !== currentStep) {
        await updateTodoStep(id, currentStep);
      }

      await updateTodoCompletion(id, true, selectedDate);
      const updatedData = await getTodoDetail(id, selectedDate);
      setTodoData(updatedData);
      if (updatedData.hasOrder) {
        setCurrentStep(updatedData.currentStep || 0);
      }
    } catch {
      showError("할 일 완료에 실패했습니다.");
    }
  }

  async function handleEditTodo() {
    navigate(`/todo/${id}/edit`, {
      state: { returnPath: location.pathname + location.search },
    });
  }

  async function handleDeleteTodo() {
    if (user?.userRole === "GUARDIAN") {
      showError("Guardian은 할 일을 삭제할 수 없습니다.");
      return;
    }

    setDeleteModal({
      isOpen: true,
      todoTitle: todoData?.title || "할 일",
    });
  }

  function closeDeleteModal() {
    setDeleteModal({ isOpen: false, todoTitle: "" });
  }

  async function confirmDelete() {
    try {
      await deleteTodo(id);
      closeDeleteModal();
      navigate("/", { replace: true });
    } catch {
      showError("할 일 삭제에 실패했습니다.");
    }
  }

  function renderTodoHeader() {
    return (
      <div className="mb-4">
        <h2 className={`text-lg font-medium ${colors.title}`}>
          {todoData.title}
        </h2>
      </div>
    );
  }

  if (!todoData || isLoading) {
    return (
      <div className="min-h-screen pb-20">
        <Header />
        <div className="mx-auto max-w-md px-4 pt-4">
          <div className="rounded-xl border-1 border-gray-300 bg-white p-8 shadow-sm">
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
      <div className="rounded-xl border-1 border-gray-300 bg-white p-4 shadow-sm">
        {renderTodoHeader()}

        <div className="mb-4 space-y-3">
          {todoData.subTodos.map((item) => {
            return (
              <label
                key={item.subTodoId}
                className={`flex cursor-pointer items-center gap-3 rounded-lg p-2 transition-colors ${
                  item.isCompleted
                    ? "bg-gray-50 opacity-70"
                    : "hover:bg-gray-50"
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
                        ? "border-transparent"
                        : "border-gray-300 bg-white hover:border-gray-400"
                    }`}
                    style={{
                      backgroundColor: item.isCompleted
                        ? colors.checkbox
                        : "white",
                    }}
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
                  className={`text-md ${item.isCompleted ? "text-gray-400 line-through opacity-60" : "text-gray-700"}`}
                >
                  {item.title}
                </span>
              </label>
            );
          })}
        </div>
      </div>
    );
  }

  function renderSequence() {
    const currentSubTodo = todoData.subTodos[currentStep];

    return (
      <div className="rounded-xl border-1 border-gray-300 bg-white p-4 shadow-sm">
        {renderTodoHeader()}

        <div className="mb-6 text-center">
          <div className={`rounded-2xl ${colors.background} px-6 py-12`}>
            <h3 className={`text-xl font-medium ${colors.title}`}>
              {currentSubTodo?.title}
            </h3>
          </div>
        </div>

        <div className="flex justify-between gap-3">
          <button
            onClick={handlePreviousStep}
            disabled={currentStep === 0}
            className={`text-md flex-1 rounded-xl px-4 py-3 font-medium transition-colors ${
              currentStep === 0
                ? "cursor-not-allowed bg-gray-100 text-gray-400"
                : colors.button
            }`}
          >
            이전
          </button>
          <button
            onClick={
              currentStep === todoData.subTodos.length - 1
                ? handleCompleteTodo
                : handleNextStep
            }
            disabled={
              user?.userRole === "GUARDIAN" ||
              (todoData.isCompleted &&
                currentStep === todoData.subTodos.length - 1)
            }
            className={`text-md flex-1 rounded-xl px-4 py-3 font-medium transition-colors ${
              user?.userRole === "GUARDIAN" ||
              (todoData.isCompleted &&
                currentStep === todoData.subTodos.length - 1)
                ? "cursor-not-allowed bg-gray-100 text-gray-400"
                : currentStep === todoData.subTodos.length - 1
                  ? colors.completeButton
                  : colors.button
            }`}
          >
            {currentStep === todoData.subTodos.length - 1
              ? todoData.isCompleted
                ? "완료됨"
                : "완료하기"
              : "다음"}
          </button>
        </div>
      </div>
    );
  }

  const rightActions = (
    <>
      <EditButton onClick={handleEditTodo} />
      <DeleteButton onClick={handleDeleteTodo} />
    </>
  );

  return (
    <div className="min-h-screen pb-20">
      <Header rightActions={rightActions} />

      <div className="mx-auto max-w-4xl p-2 sm:p-4">
        <div className="px-2">
          {todoData?.hasOrder ? renderSequence() : renderChecklist()}

          <div className="mt-4">
            <div className="rounded-xl border border-gray-200/50 bg-white/90 p-4 shadow-sm backdrop-blur-sm">
              <span className="mb-3 block text-sm text-black">진행도</span>

              <div
                className="mb-3 flex items-center justify-center px-4"
                key={currentStep}
              >
                {todoData.subTodos.map((subTodo, index) => {
                  let isCompleted = false;
                  let isActive = false;

                  if (
                    todoData?.type === "SEQUENCE" ||
                    (todoData?.type === "ROUTINE" && todoData?.hasOrder)
                  ) {
                    if (todoData.isCompleted) {
                      isCompleted = true;
                      isActive = false;
                    } else {
                      isCompleted = index < currentStep;
                      isActive = index === currentStep;
                    }
                  } else {
                    isCompleted = subTodo.isCompleted;
                    isActive = !isCompleted;
                  }

                  const lineActive = isCompleted;

                  return (
                    <div
                      key={`progress-${subTodo.subTodoId || index}`}
                      className="flex items-center"
                    >
                      <div
                        className={`text-2xl transition-all duration-500 ${
                          isCompleted
                            ? "scale-110 animate-bounce"
                            : "scale-100 opacity-60"
                        }`}
                        style={{
                          transform: isCompleted ? "scale(1.2)" : "scale(1)",
                          filter: isCompleted
                            ? `drop-shadow(0 0 10px ${todoData.type === "ROUTINE" ? "rgba(255, 179, 71, 0.5)" : "rgba(76, 175, 80, 0.5)"})`
                            : "none",
                        }}
                      >
                        {isCompleted ? (
                          <FaCheckCircle
                            style={{
                              color:
                                todoData.type === "ROUTINE"
                                  ? "#FFB347"
                                  : "#4CAF50",
                            }}
                          />
                        ) : isActive ? (
                          <FaCircle
                            style={{
                              color:
                                todoData.type === "ROUTINE"
                                  ? "#FFB347"
                                  : "#4CAF50",
                              opacity: 1,
                            }}
                          />
                        ) : (
                          <FaCircle
                            style={{ color: "#D1D5DB", opacity: 0.5 }}
                          />
                        )}
                      </div>

                      {index < todoData.subTodos.length - 1 && (
                        <div className="mx-2 flex items-center">
                          <div
                            className="h-0.5 w-8 transition-all duration-700"
                            style={{
                              backgroundImage: lineActive
                                ? todoData.type === "ROUTINE"
                                  ? "linear-gradient(90deg, #fb923c, #fdba74)"
                                  : "linear-gradient(90deg, #4caf50, #81c784)"
                                : "repeating-linear-gradient(90deg, #d1d5db 0px, #d1d5db 4px, transparent 4px, transparent 8px)",
                              backgroundSize: lineActive
                                ? "100% 100%"
                                : "8px 100%",
                            }}
                          />
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

              {todoData.isCompleted && (
                <div className="py-2 text-center">
                  <p className="text-lg font-medium text-black">
                    🎉 완료했어요! 🌟
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <Modal
        isOpen={deleteModal.isOpen}
        onClose={closeDeleteModal}
        primaryButtonText="삭제"
        secondaryButtonText="취소"
        primaryButtonColor="bg-warning hover:bg-red-600"
        onPrimaryClick={confirmDelete}
      >
        <div className="text-center">
          <div className="mb-4 flex justify-center">
            <div className="bg-warning flex h-16 w-16 items-center justify-center rounded-full">
              <HiOutlineTrash className="text-white" size={32} />
            </div>
          </div>
          <h3 className="mb-2 text-xl font-bold text-black">
            작업을 삭제할까요?
          </h3>
          <p className="mb-1 text-sm text-black">
            삭제된 작업은 복구할 수 없어요.
          </p>
          <p className="text-sm text-black">정말로 진행하시겠어요?</p>
        </div>
      </Modal>

      <TabBar />
    </div>
  );
}
