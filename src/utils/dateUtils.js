export function getCurrentDate() {
  const now = new Date();
  const koreaTime = new Date(
    now.toLocaleString("en-US", { timeZone: "Asia/Seoul" }),
  );
  const month = koreaTime.getMonth() + 1;
  const date = koreaTime.getDate();
  const day = ["일", "월", "화", "수", "목", "금", "토"][koreaTime.getDay()];
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

export function formatDateToYMD(date) {
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const day = date.getDate().toString().padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function formatDateWithDay(date) {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const dayOfWeek = ["일", "월", "화", "수", "목", "금", "토"][date.getDay()];
  return `${year}년 ${month}월 ${day}일 ${dayOfWeek}요일`;
}

export function getDateRange(centerDate, range = 7) {
  const dates = [];
  const center = new Date(centerDate);

  for (let i = -range; i <= range; i++) {
    const date = new Date(center);
    date.setDate(center.getDate() + i);
    dates.push({
      date: formatDateToYMD(date),
      displayDate: formatDateWithDay(date),
      isToday: i === 0,
      isPast: i < 0,
      isFuture: i > 0,
    });
  }

  return dates;
}

export function addDays(date, days) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}
