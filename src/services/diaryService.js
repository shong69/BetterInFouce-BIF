import api from "./api.js";

// 월별 일기 목록 가져오기
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

// 특정 일기 가져오기
export const fetchDiary = async (id) => {
  try {
    const response = await api.get(`/api/diaries/test/${id}`);
    return response.data;
  } catch (error) {
    console.error("일기 가져오기 실패:", error);
    throw error;
  }
};

// 일기 생성
export const createDiary = async (diaryData) => {
  try {
    const response = await api.post("/api/diaries/test", diaryData);
    return response.data;
  } catch (error) {
    console.error("일기 생성 실패:", error);
    throw error;
  }
};

// 일기 수정
export const updateDiary = async (id, diaryData) => {
  try {
    // TODO: 실제 API 호출로 변경
    const response = await api.patch(`/api/diaries/test/${id}`, diaryData);
    return response.data;
  } catch (error) {
    console.error("일기 수정 실패:", error);
    throw error;
  }
};

// 일기 삭제
export const deleteDiary = async (id) => {
  try {
    await api.delete(`/api/diaries/test/${id}`);
    return true;
  } catch (error) {
    console.error("일기 삭제 실패:", error);
    throw error;
  }
};
