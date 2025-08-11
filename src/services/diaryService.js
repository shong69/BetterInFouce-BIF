import api from "./api.js";

export const fetchMonthlyDiaries = async (year, month) => {
  try {
    const response = await api.get(
      `/api/diaries/test/monthly-summary?year=${year}&month=${month}`,
    );
    return response.data;
  } catch (error) {
    console.error("월별 일기 목록 가져오기 실패:", error);
    throw error;
  }
};

export const fetchDiary = async (id) => {
  try {
    const response = await api.get(`/api/diaries/test/${id}`);
    return response.data;
  } catch (error) {
    console.error("일기 가져오기 실패:", error);
    throw error;
  }
};

export const createDiary = async (diaryData) => {
  try {
    const response = await api.post("/api/diaries/test", diaryData);
    return response.data;
  } catch (error) {
    console.error("일기 생성 실패:", error);
    throw error;
  }
};

export const updateDiary = async (id, diaryData) => {
  try {
    const response = await api.patch(`/api/diaries/test/${id}`, diaryData);
    return response.data;
  } catch (error) {
    console.error("일기 수정 실패:", error);
    throw error;
  }
};

export const deleteDiary = async (id) => {
  try {
    await api.delete(`/api/diaries/test/${id}`);
    return true;
  } catch (error) {
    console.error("일기 삭제 실패:", error);
    throw error;
  }
};
