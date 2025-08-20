import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import Header from "@components/common/Header";
import Card from "@components/common/Card";
import TabBar from "@components/common/TabBar";
import Modal from "@components/ui/Modal";
import DateBox from "@components/ui/DateBox";
import TabButton from "@components/ui/TabButton";
import DatePickerModal from "@components/ui/DatePickerModal";

import { BiPlus, BiCalendar } from "react-icons/bi";
import { HiOutlineClipboardList } from "react-icons/hi";

import {
  useLoadingStore,
  useToastStore,
  useTodoStore,
  useUserStore,
} from "@stores";
import { getTodos, deleteTodo } from "@services/todoService";

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
  const [routines, setRoutines] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    todoId: null,
    todoTitle: "",
  });

  const [datePickerModal, setDatePickerModal] = useState({
    isOpen: false,
  });

  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState(() => {
    const tabParam = searchParams.get("tab");
    return tabParam === "task" ? "TASK" : "ROUTINE";
  });
  const { showLoading, hideLoading } = useLoadingStore();
  const { showSuccess, showError, showWarning } = useToastStore();
  const { selectedDate, setSelectedDate } = useTodoStore();
  const { user } = useUserStore();

  useEffect(() => {
    async function fetchTodoList() {
      setIsLoading(true);
      try {
        const data = await getTodos(selectedDate);
        setTasks(data.filter((todo) => todo.type === "TASK") || []);
        setRoutines(data.filter((todo) => todo.type === "ROUTINE") || []);
      } catch {
        showError("할 일을 불러오는데 실패했습니다.");
      } finally {
        setIsLoading(false);
      }
    }

    fetchTodoList();
  }, [selectedDate, showError]);

  const currentItems = activeTab === "ROUTINE" ? routines : tasks;
  const incompletedItems = currentItems.filter((item) => !item.isCompleted);
  const completedItems = currentItems.filter((item) => item.isCompleted);

  function handleCardClick(id) {
    if (user?.userRole === "GUARDIAN") {
      showWarning("Guardian은 할 일 상세보기에 접근할 수 없습니다.");
      return;
    }

    const allItems = [...tasks, ...routines];
    const clickedItem = allItems.find((item) => item.todoId === id);

    const returnTab = activeTab === "ROUTINE" ? "routine" : "task";

    if (clickedItem?.hasOrder) {
      navigate(`/todo/${id}?type=sequence&returnTab=${returnTab}`);
    } else {
      navigate(`/todo/${id}?type=checklist&returnTab=${returnTab}`);
    }
  }

  function handleEdit(id) {
    const returnTab = activeTab === "ROUTINE" ? "routine" : "task";
    navigate(`/todo/${id}/edit?returnTab=${returnTab}`);
  }

  async function handleDelete(id) {
    const allItems = [...tasks, ...routines];
    const todoToDelete = allItems.find((item) => item.todoId === id);

    setDeleteModal({
      isOpen: true,
      todoId: id,
      todoTitle: todoToDelete?.title || "할 일",
    });
  }

  function closeDeleteModal() {
    setDeleteModal({ isOpen: false, todoId: null, todoTitle: "" });
  }

  async function confirmDelete() {
    if (!deleteModal.todoId) {
      showError("삭제할 할 일을 찾을 수 없습니다.");
      return;
    }

    try {
      showLoading("삭제 중...");
      await deleteTodo(deleteModal.todoId);

      const deletedId = deleteModal.todoId;
      setTasks((prev) => prev.filter((todo) => todo.todoId !== deletedId));
      setRoutines((prev) =>
        prev.filter((routine) => routine.todoId !== deletedId),
      );

      closeDeleteModal();
      showSuccess("할 일이 삭제되었습니다.");
    } catch (error) {
      const errorMessage =
        error?.response?.status === 404
          ? "이미 삭제된 할 일입니다."
          : error?.response?.status === 403
            ? "삭제 권한이 없습니다."
            : "삭제에 실패했습니다. 다시 시도해주세요.";

      showError(errorMessage);
    } finally {
      hideLoading();
    }
  }

  function handleAddTodo() {
    const returnTab = activeTab === "ROUTINE" ? "routine" : "task";
    navigate(`/todo/new?returnTab=${returnTab}`);
  }

  function openDatePicker() {
    setDatePickerModal({ isOpen: true });
  }

  function closeDatePicker() {
    setDatePickerModal({ isOpen: false });
  }

  function handleDateSelect(selectedDateValue) {
    setSelectedDate(selectedDateValue);
    closeDatePicker();
  }

  return (
    <div className="h-screen">
      <Header />

      <div className="mx-auto max-w-4xl p-2 sm:p-4">
        <div className="mb-1 px-2 sm:px-0">
          <div className="flex items-center gap-1">
            <DateBox />
            <button
              onClick={openDatePicker}
              className="text-black-600 flex h-10 w-10 items-center justify-center rounded-full bg-gray-100 transition-colors hover:bg-gray-200"
            >
              <BiCalendar size={20} />
            </button>
          </div>
        </div>

        <div className="mt-2 px-4">
          <TabButton
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            leftTitle="루틴"
            rightTitle="할 일"
          />
        </div>
        <div className="mt-6 flex-1 overflow-y-auto p-4">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-green-600" />
            </div>
          ) : (
            <>
              {incompletedItems.length > 0 && (
                <div className="mb-6">
                  <h3 className="mb-3 text-sm font-bold text-gray-800">
                    할 일 ({incompletedItems.length}개)
                  </h3>
                  <div className="space-y-3">
                    {incompletedItems.map((item) => (
                      <Card
                        key={item.todoId}
                        id={item.todoId}
                        type="todo"
                        title={item.title}
                        hasOrder={item.hasOrder}
                        subTodos={item.subTodos || []}
                        isCompleted={false}
                        onEdit={() => handleEdit(item.todoId)}
                        onDelete={() => handleDelete(item.todoId)}
                        onClick={handleCardClick}
                      />
                    ))}
                  </div>
                </div>
              )}

              {completedItems.length > 0 && (
                <div className="mb-6">
                  <h3 className="mb-3 text-sm font-bold text-gray-800">
                    ✅ 완료한 일 ({completedItems.length}개)
                  </h3>
                  <div className="space-y-4">
                    {completedItems.map((item) => (
                      <Card
                        key={item.todoId}
                        id={item.todoId}
                        type="todo"
                        title={item.title}
                        hasOrder={item.hasOrder}
                        subTodos={item.subTodos || []}
                        isCompleted={true}
                        onEdit={() => handleEdit(item.todoId)}
                        onDelete={() => handleDelete(item.todoId)}
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

        {currentItems.length > 0 && (
          <div className="mt-8 mb-8 p-4 text-center">
            <p className="text-xs text-gray-400">
              카드를 오른쪽으로 스와이프하면 수정/삭제 버튼이 나타납니다.
            </p>
          </div>
        )}
      </div>

      {user?.userRole === "BIF" && (
        <button
          onClick={handleAddTodo}
          className="fixed right-6 bottom-28 flex h-16 w-16 touch-manipulation items-center justify-center rounded-full bg-green-600 shadow-lg transition-colors active:bg-green-700"
        >
          <BiPlus className="text-white" size={28} />
        </button>
      )}

      <Modal
        isOpen={deleteModal.isOpen}
        onClose={closeDeleteModal}
        primaryButtonText="삭제"
        secondaryButtonText="취소"
        primaryButtonColor="bg-warning"
        onPrimaryClick={confirmDelete}
      >
        <div className="text-center">
          <p className="mb-2 text-lg font-medium">할 일 삭제</p>
          <p className="text-gray-600">
            "<span className="font-medium">{deleteModal.todoTitle}</span>"을(를)
            삭제하시겠습니까?
          </p>
        </div>
      </Modal>

      <DatePickerModal
        isOpen={datePickerModal.isOpen}
        onClose={closeDatePicker}
        onDateSelect={handleDateSelect}
        currentDate={selectedDate}
      />

      <TabBar />
    </div>
  );
}
