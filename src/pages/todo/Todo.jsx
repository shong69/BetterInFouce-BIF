import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import Header from "@components/common/Header";
import Card from "@components/common/Card";
import TabBar from "@components/common/TabBar";
import Modal from "@components/ui/Modal";
import DateBox from "@components/ui/DateBox";
import TabButton from "@components/ui/TabButton";

import { BiPlus } from "react-icons/bi";
import { HiOutlineClipboardList } from "react-icons/hi";

import { useLoadingStore, useToastStore, useTodoStore } from "@stores";
import { getTodos, getTodoDetail, deleteTodo } from "@services/todoService";

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
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    todoId: null,
    todoTitle: "",
  });

  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState(() => {
    const tabParam = searchParams.get("tab");
    return tabParam === "task" ? "TASK" : "ROUTINE";
  });
  const { showLoading, hideLoading } = useLoadingStore();
  const { showSuccess, showError, showWarning } = useToastStore();
  const { selectedDate } = useTodoStore();

  useEffect(() => {
    async function fetchTodoList() {
      showLoading("할 일을 가져오는 중...");
      try {
        const data = await getTodos(selectedDate);
        setTasks(data.filter((todo) => todo.type === "TASK") || []);
        setRoutines(data.filter((todo) => todo.type === "ROUTINE") || []);
      } catch {
        showError("할 일을 불러오는데 실패했습니다.");
      } finally {
        hideLoading();
      }
    }

    fetchTodoList();
  }, [selectedDate, showLoading, hideLoading, showError]);

  const currentItems = activeTab === "ROUTINE" ? routines : tasks;
  const incompletedItems = currentItems.filter((item) => !item.isCompleted);
  const completedItems = currentItems.filter((item) => item.isCompleted);

  function handleCardClick(id) {
    const allItems = [...tasks, ...routines];
    const clickedItem = allItems.find((item) => item.todoId === id);

    const returnTab = activeTab === "ROUTINE" ? "routine" : "task";

    if (clickedItem?.hasOrder) {
      navigate(`/todo/${id}?type=sequence&returnTab=${returnTab}`);
    } else {
      navigate(`/todo/${id}?type=checklist&returnTab=${returnTab}`);
    }
  }

  async function handleEdit(id) {
    try {
      showLoading("할 일 정보를 불러오는 중...");
      await getTodoDetail(id);
      hideLoading();

      const returnTab = activeTab === "ROUTINE" ? "routine" : "task";
      navigate(`/todo/${id}/edit?returnTab=${returnTab}`);
    } catch (error) {
      hideLoading();

      if (error.response?.status === 404) {
        showError("존재하지 않는 할 일입니다.");
      } else if (error.response?.status === 403) {
        showWarning("수정 권한이 없습니다.");
      } else if (error.response?.status === 401) {
        showError("로그인이 필요합니다.");
        setTimeout(() => navigate("/login"), 1000);
      } else {
        showError("네트워크 오류가 발생했습니다. 다시 시도해주세요.");
      }
    }
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

  return (
    <div className="min-h-screen pb-20">
      <Header />

      <div className="mx-auto max-w-md px-4 pt-4">
        <DateBox />

        <div className="mt-4">
          <TabButton
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            leftTitle="루틴"
            rightTitle="할 일"
          />
        </div>
        <div className="mt-6">
          {incompletedItems.length > 0 && (
            <div className="mb-6">
              <h3 className="mb-3 text-sm font-medium text-gray-600">
                할 일 ({incompletedItems.length}개)
              </h3>
              <div className="space-y-4">
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
              <h3 className="text-primary mb-3 text-sm font-medium">
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
        </div>

        {currentItems.length > 0 && (
          <div className="mt-8 mb-8 text-center">
            <p className="text-sm text-gray-400">
              카드를 오른쪽으로 스와이프하면 수정/삭제 버튼이 나타납니다.
            </p>
          </div>
        )}
      </div>

      <button
        onClick={handleAddTodo}
        className="fixed right-6 bottom-24 flex h-14 w-14 touch-manipulation items-center justify-center rounded-full bg-green-600 shadow-lg transition-colors active:bg-green-700"
      >
        <BiPlus className="text-white" size={28} />
      </button>

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

      <TabBar />
    </div>
  );
}
