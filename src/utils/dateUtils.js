export function getCurrentDate() {
  const now = new Date();
  const month = now.getMonth() + 1;
  const date = now.getDate();
  const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
  return `${month}월 ${date}일 ${day}요일`;
}
