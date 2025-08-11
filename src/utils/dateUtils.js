export function getCurrentDate() {
  const now = new Date();
  const month = now.getMonth() + 1;
  const date = now.getDate();
  const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
  return `${month}월 ${date}일 ${day}요일`;
}

export function formatDate(diaryDate) {
  if (!diaryDate) return "";

  const date = new Date(diaryDate);
  const year = date.getFullYear().toString().slice(-2);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${year}년 ${month}월 ${day}일`;
}
