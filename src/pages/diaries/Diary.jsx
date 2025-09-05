import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import DiaryCalendar from "./components/DiaryCalendar";
import { useDiaryStore } from "@stores";
import { EMOTIONS } from "@constants/emotions";
import "@styles/diary.css";

export default function Diary() {
  const navigate = useNavigate();
  const { setSelectedEmotion: setStoreEmotion, fetchMonthlyDiaries } =
    useDiaryStore();
  const [monthlyData, setMonthlyData] = useState({});
  const [canWriteToday, setCanWriteToday] = useState(false);
  const [consecutiveDays, setConsecutiveDays] = useState(0);
  const emotions = EMOTIONS;

  const fetchMonthlyData = async (year, month) => {
    try {
      const response = await fetchMonthlyDiaries(year, month);

      const monthlyDataMap = response.dailyEmotions;

      if (response.canWriteToday !== undefined) {
        setCanWriteToday(response.canWriteToday);
      }
      setMonthlyData(monthlyDataMap);

      const currentDate = new Date();
      const currentYear = currentDate.getFullYear();
      const currentMonth = currentDate.getMonth() + 1;

      if (year === currentYear && month === currentMonth) {
        setConsecutiveDays(calculateConsecutiveDays(monthlyDataMap));
      }
    } catch (error) {
      if (error.response && error.response.data) {
        setMonthlyData({});
      }
    }
  };

  const calculateConsecutiveDays = (monthlyData) => {
    if (!monthlyData || Object.keys(monthlyData).length === 0) {
      return 0;
    }

    let consecutiveDays = 0;
    const currentDate = new Date();
    let checkedToday = false;

    while (true) {
      const year = currentDate.getFullYear();
      const month = String(currentDate.getMonth() + 1).padStart(2, "0");
      const day = String(currentDate.getDate()).padStart(2, "0");
      const dateString = `${year}-${month}-${day}`;

      if (monthlyData[dateString]) {
        consecutiveDays++;
      } else {
        if (!checkedToday) {
          checkedToday = true;
        } else {
          break;
        }
      }

      currentDate.setDate(currentDate.getDate() - 1);
    }

    return consecutiveDays;
  };

  const handleEmotionSelect = (emotionId) => {
    if (!canWriteToday) {
      return;
    }
    setStoreEmotion(emotionId);
    navigate(`/diaries/create`);
  };

  const handleDiaryClick = (dailyInfo) => {
    const diaryId = dailyInfo.diaryId;
    navigate(`/diaries/${diaryId}`);
  };

  return (
    <div className="min-h-screen">
      <Header showTodoButton={false} />
      <div className="mx-auto max-w-4xl p-2 sm:p-4">
        <div className="mx-3 mb-6 rounded-lg border-1 border-gray-300 shadow-sm sm:mb-8">
          <section className="starry-bg relative overflow-hidden rounded-lg bg-gray-800 px-6 py-4 text-center">
            <div className="relative z-10">
              <h3 className="mb-2 text-sm font-medium text-white">
                연속 일기 작성
              </h3>
              <div className="mb-2 flex items-center justify-center gap-2">
                <span className="text-2xl">🔥</span>
                <p className="bg-gradient-to-r from-[#FFE500] to-[#61FF59] bg-clip-text text-4xl font-bold text-transparent">
                  {consecutiveDays}일
                </p>
                <span className="text-2xl">🔥</span>
              </div>
              <p className="text-xs text-white">
                {consecutiveDays === 0
                  ? "오늘의 감정일기를 작성해보세요!"
                  : "꾸준히 기록하고 있어요"}
              </p>
            </div>
          </section>
        </div>
        <div
          className={`mx-3 mb-6 rounded-lg border-1 border-gray-300 p-4 shadow-sm sm:mb-8 ${
            !canWriteToday
              ? "border-gray-200 bg-gray-50"
              : "border-gray-200 bg-white"
          }`}
        >
          <h5
            className={`m-3 text-center text-sm font-semibold sm:m-4 sm:text-xl ${
              !canWriteToday ? "text-gray-500" : "text-gray-800"
            }`}
          >
            {!canWriteToday
              ? "오늘은 이미 일기를 작성했어요!"
              : "오늘은 어떤 하루였나요?"}
          </h5>
          <div className="grid grid-cols-5">
            {emotions.map((emotion) => {
              const isDisabled = !canWriteToday;

              return (
                <button
                  key={emotion.id}
                  onClick={() => {
                    handleEmotionSelect(emotion.id);
                  }}
                  disabled={isDisabled}
                  className={`group rounded-lg p-2 text-center transition-all duration-200 sm:p-6 ${
                    isDisabled
                      ? "cursor-not-allowed opacity-50"
                      : "cursor-pointer touch-manipulation hover:border-blue-300 hover:bg-blue-50"
                  }`}
                >
                  <div
                    className={`mb-1 transition-transform sm:mb-2 ${
                      isDisabled ? "" : "group-hover:scale-110"
                    }`}
                  >
                    <img
                      src={emotion.icon}
                      alt={emotion.name}
                      className="mx-auto h-10 w-10 drop-shadow-lg sm:h-12 sm:w-12"
                    />
                  </div>
                  <div
                    className={`text-xs leading-tight font-medium sm:text-sm ${
                      isDisabled ? "text-gray-400" : "text-gray-700"
                    }`}
                  >
                    {emotion.name}
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        <DiaryCalendar
          monthlyData={monthlyData}
          emotions={emotions}
          onMonthChange={fetchMonthlyData}
          onDiaryClick={handleDiaryClick}
        />
      </div>
      <TabBar />
    </div>
  );
}
