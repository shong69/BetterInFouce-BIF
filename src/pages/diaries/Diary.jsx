import { useState } from "react";
import { useNavigate } from "react-router-dom";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import DateBox from "@components/ui/DateBox";
import { useDiaryStore } from "@stores";
import { EMOTIONS } from "@constants/emotions";

export default function Diary() {
  const navigate = useNavigate();
  const { setSelectedEmotion: setStoreEmotion, fetchMonthlyDiaries } =
    useDiaryStore();
  const [monthlyData, setMonthlyData] = useState({});
  const [canWriteToday, setCanWriteToday] = useState(false);
  const emotions = EMOTIONS;

  const fetchMonthlyData = async (year, month) => {
    try {
      const response = await fetchMonthlyDiaries(year, month);

      const monthlyDataMap = response.dailyEmotions;

      if (response.canWriteToday !== undefined) {
        setCanWriteToday(response.canWriteToday);
      }
      setMonthlyData(monthlyDataMap);
    } catch (error) {
      if (error.response && error.response.data) {
        setMonthlyData({});
      }
    }
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
    <>
      <style>
        {`
          .fc-theme-standard .fc-scrollgrid td,
          .fc-theme-standard .fc-scrollgrid th {
            border: none !important;
          }
          
          .fc-daygrid-day-events {
            display: none !important;
            height: 0 !important;
            min-height: 0 !important;
            overflow: hidden !important;
            pointer-events: none !important;
          }
        `}
      </style>
      <Header />
      <div className="mx-auto max-w-4xl bg-white p-2 sm:p-4">
        <div className="mb-1 px-2 sm:px-0">
          <DateBox />
        </div>

        <div
          className={`mx-3 mb-6 rounded-lg border-1 border-gray-300 p-4 shadow-sm sm:mb-8 ${
            !canWriteToday
              ? "border-gray-200 bg-gray-50 opacity-60"
              : "border-gray-200"
          }`}
        >
          <h5
            className={`m-3 text-center text-sm font-semibold sm:m-4 sm:text-xl ${
              !canWriteToday ? "text-gray-500" : "text-gray-800"
            }`}
          >
            {!canWriteToday ? "오늘은 이미 일기를 작성했어요!" : "오늘의 기분"}
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

        <div className="mx-3 mt-6 mb-[78px] sm:mb-[78px]">
          <div className="overflow-hidden rounded-lg border-1 border-gray-300 bg-white p-4 shadow-sm">
            <div
              className={`[&_.fc]:!border-none [&_.fc]:font-sans [&_.fc-button]:!flex [&_.fc-button]:h-8 [&_.fc-button]:w-8 [&_.fc-button]:!items-center [&_.fc-button]:!justify-center [&_.fc-button]:!rounded-full [&_.fc-button]:!border-none [&_.fc-button]:!bg-gray-200 [&_.fc-button]:sm:h-10 [&_.fc-button]:sm:w-10 [&_.fc-col-header-cell]:!border-none [&_.fc-col-header-cell]:p-1 [&_.fc-col-header-cell]:text-xs [&_.fc-col-header-cell]:font-medium [&_.fc-col-header-cell]:text-gray-600 [&_.fc-col-header-cell]:sm:p-2 [&_.fc-col-header-cell]:sm:text-sm [&_.fc-day-other]:bg-white [&_.fc-day-other_.fc-daygrid-day-number]:text-gray-400 [&_.fc-day-sun_.fc-daygrid-day-number]:text-red-500 [&_.fc-daygrid-day]:min-h-[60px] [&_.fc-daygrid-day]:!border-none [&_.fc-daygrid-day]:sm:min-h-[80px] [&_.fc-daygrid-day-bg]:!border-none [&_.fc-daygrid-day-frame]:!border-none [&_.fc-daygrid-day-number]:!p-0 [&_.fc-daygrid-day-number]:text-sm [&_.fc-daygrid-day-number]:font-medium [&_.fc-daygrid-day-number]:text-gray-700 [&_.fc-daygrid-day-number]:sm:text-base [&_.fc-daygrid-day-top]:flex [&_.fc-daygrid-day-top]:items-center [&_.fc-daygrid-day-top]:justify-center [&_.fc-daygrid-day-top]:!border-none [&_.fc-header-toolbar]:rounded-t-lg [&_.fc-header-toolbar]:p-3 [&_.fc-icon-chevron-left]:text-black [&_.fc-icon-chevron-right]:text-black [&_.fc-scrollgrid]:!border-none [&_.fc-scrollgrid_table]:!border-none [&_.fc-scrollgrid_table_tbody]:!border-none [&_.fc-scrollgrid_table_td]:!border-none [&_.fc-scrollgrid_table_th]:!border-none [&_.fc-scrollgrid_table_thead]:!border-none [&_.fc-scrollgrid_table_tr]:!border-none [&_.fc-scrollgrid-section]:!border-none [&_.fc-scrollgrid-section-body]:!border-none [&_.fc-scrollgrid-section-footer]:!border-none [&_.fc-scrollgrid-section-header]:!border-none [&_.fc-table]:!border-collapse [&_.fc-table]:!border-spacing-0 [&_.fc-table]:!border-none [&_.fc-table_td]:!border-none [&_.fc-table_th]:!border-none [&_.fc-toolbar]:!mb-1 [&_.fc-toolbar]:flex [&_.fc-toolbar]:items-center [&_.fc-toolbar]:justify-between [&_.fc-toolbar-chunk]:flex [&_.fc-toolbar-chunk]:items-center [&_.fc-toolbar-chunk]:gap-2 [&_.fc-toolbar-chunk]:rounded-4xl [&_.fc-toolbar-title]:!text-sm [&_.fc-toolbar-title]:!leading-none [&_.fc-toolbar-title]:font-semibold [&_.fc-toolbar-title]:text-gray-800 [&_.fc-toolbar-title]:sm:!text-base [&_.fc-toolbar-title]:md:!text-lg`}
            >
              <FullCalendar
                plugins={[dayGridPlugin, interactionPlugin]}
                initialView="dayGridMonth"
                headerToolbar={{
                  left: "prev",
                  center: "title",
                  right: "next",
                }}
                locale="ko"
                height="auto"
                fixedWeekCount={false}
                datesSet={(dateInfo) => {
                  const centerDate = new Date(
                    dateInfo.start.getTime() +
                      (dateInfo.end.getTime() - dateInfo.start.getTime()) / 2,
                  );
                  const year = centerDate.getFullYear();
                  const month = centerDate.getMonth() + 1;
                  fetchMonthlyData(year, month);
                }}
                dayCellContent={(arg) => {
                  const date = arg.date;
                  const dateStr = date.toLocaleDateString("en-CA");

                  const dailyInfo = monthlyData && monthlyData[dateStr];
                  return (
                    <div className="fc-daygrid-day-number">
                      {dailyInfo ? (
                        <button
                          className="flex h-full w-full cursor-pointer items-center justify-center border-none bg-transparent p-0"
                          onClick={() => {
                            handleDiaryClick(dailyInfo);
                          }}
                          aria-label={`View diary for ${dateStr}`}
                        >
                          <img
                            src={
                              emotions.find((e) => {
                                return e.id === dailyInfo.emotion;
                              })?.icon
                            }
                            alt="emotion"
                            className="mt-1.5 h-9 w-9 hover:drop-shadow-[0_3px_6px_rgba(0,0,0,0.3)] sm:h-12 sm:w-12"
                          />
                        </button>
                      ) : (
                        <div className="mb-10">{date.getDate()}</div>
                      )}
                    </div>
                  );
                }}
              />
            </div>
          </div>
        </div>
      </div>
      <TabBar />
    </>
  );
}
