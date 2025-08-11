export function getCurrentDate() {
  const now = new Date();
  const month = now.getMonth() + 1;
  const date = now.getDate();
  const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
  return `${month}월 ${date}일 ${day}요일`;
}

export function formatDate(diaryDate) {
  if (!diaryDate) return ""; //오늘 날짜 리턴

  const date = new Date(diaryDate);
  const year = date.getFullYear().toString().slice(-2);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${year}년 ${month}월 ${day}일`;
}

// LocalDateTime 형식을 위한 더 상세한 포맷팅
export function formatDateTime(dateTimeString) {
  if (!dateTimeString) return "";

  const date = new Date(dateTimeString);
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hours = date.getHours().toString().padStart(2, "0");
  const minutes = date.getMinutes().toString().padStart(2, "0");

  return `${year}년 ${month}월 ${day}일 ${hours}:${minutes}`;
}

// 요일을 포함한 날짜 포맷팅
export function formatDateWithDay(diaryDate) {
  if (!diaryDate) return "";

  const date = new Date(diaryDate);
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const dayNames = ["일", "월", "화", "수", "목", "금", "토"];
  const dayName = dayNames[date.getDay()];

  return `${year}년 ${month}월 ${day}일 (${dayName})`;
}
