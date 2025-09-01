import { getCurrentDate } from "@utils/dateUtils";

export default function DateBox({ customDate }) {
  const getDisplayDate = () => {
    if (customDate) {
      const date = new Date(customDate);
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const dayOfWeek = ["일", "월", "화", "수", "목", "금", "토"][
        date.getDay()
      ];
      return `${month}월 ${day}일 ${dayOfWeek}요일`;
    }
    return getCurrentDate();
  };

  return (
    <div className="flex items-center justify-between px-3 py-3">
      <div className="text-black-600 text-sm font-medium tracking-wide">
        {getDisplayDate()}
      </div>
    </div>
  );
}
