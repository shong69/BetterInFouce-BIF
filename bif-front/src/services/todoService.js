import api from "@services/api";

export async function createTodoByAi(request) {
  const response = await api.post("/api/todos", request);
  return response.data;
}

export async function getTodos(date) {
  let dateString = "";

  if (date) {
    if (typeof date === "string") {
      dateString = date;
    } else if (date instanceof Date) {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, "0");
      const day = date.getDate().toString().padStart(2, "0");
      dateString = `${year}-${month}-${day}`;
    }
  }

  const params = dateString ? { date: dateString } : {};
  const response = await api.get("/api/todos", { params });
  return response.data;
}

export async function getTodoDetail(todoId, date = null) {
  const params = date ? { date } : {};
  const response = await api.get(`/api/todos/${todoId}`, { params });
  return response.data;
}

export async function updateTodo(todoId, request) {
  const response = await api.put(`/api/todos/${todoId}`, request);
  return response.data;
}

export async function deleteTodo(todoId) {
  const response = await api.delete(`/api/todos/${todoId}`);
  return response.data;
}

export async function updateTodoCompletion(todoId, isCompleted, date = null) {
  const params = date ? { date } : {};
  const response = await api.patch(
    `/api/todos/${todoId}/completion`,
    { isCompleted },
    { params },
  );
  return response.data;
}

export async function updateSubTodoCompletion(
  todoId,
  subTodoId,
  isCompleted,
  date = null,
) {
  const params = date ? { date } : {};
  const response = await api.patch(
    `/api/todos/${todoId}/subtodos/${subTodoId}/complete`,
    { isCompleted },
    { params },
  );
  return response.data;
}

export async function updateTodoStep(todoId, step) {
  const response = await api.patch(`/api/todos/${todoId}/step`, { step });
  return response.data;
}
