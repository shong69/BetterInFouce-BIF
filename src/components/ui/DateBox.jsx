export default function DateBox() {
  function getCurrentDate() {
    const now = new Date();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
    return `${month}월 ${date}일 ${day}요일`;
  }

  return (
    <div className="flex items-center justify-between bg-white px-5 py-5">
      <div className="text-black-600 text-[13px] font-medium tracking-wide">
        {getCurrentDate()}
      </div>
    </div>
  );
}
