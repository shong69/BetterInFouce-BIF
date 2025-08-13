import api from "./api.js";

export const fetchMonthlyDiaries = async function (year, month) {
  const response = await api.get(
    `/api/diaries/monthly-summary?year=${year}&month=${month}`,
  );
  return response.data;
};

export const fetchDiary = async function (id) {
  const response = await api.get(`/api/diaries/${id}`);
  return response.data;
};

export const createDiary = async function (diaryData) {
  const response = await api.post("/api/diaries", diaryData);
  return response.data;
};

export const updateDiary = async function (id, diaryData) {
  const response = await api.patch(`/api/diaries/${id}`, diaryData);
  return response.data;
};

export const deleteDiary = async function (id) {
  await api.delete(`/api/diaries/${id}`);
  return true;
};
