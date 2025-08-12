import api from "./api.js";

export const fetchMonthlyDiaries = async (year, month) => {
  const response = await api.get(
    `/api/diaries/monthly-summary?year=${year}&month=${month}`,
  );
  return response.data;
};

export const fetchDiary = async (id) => {
  const response = await api.get(`/api/diaries/${id}`);
  return response.data;
};

export const createDiary = async (diaryData) => {
  const response = await api.post("/api/diaries", diaryData);
  return response.data;
};

export const updateDiary = async (id, diaryData) => {
  const response = await api.patch(`/api/diaries/${id}`, diaryData);
  return response.data;
};

export const deleteDiary = async (id) => {
  await api.delete(`/api/diaries/${id}`);
  return true;
};
