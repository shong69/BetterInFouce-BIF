import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";

export default function DiaryCalendar({
  monthlyData,
  emotions,
  onMonthChange,
  onDiaryClick,
}) {
  return (
    <div className="mx-3 mt-6 mb-[78px] sm:mb-[78px]">
      <div className="overflow-hidden rounded-lg border-1 border-gray-300 bg-white p-4 text-center text-xs shadow-sm sm:text-sm">
        <div
          className={`[&_.fc]:!border-none [&_.fc]:font-sans [&_.fc-button]:!flex [&_.fc-button]:h-8 [&_.fc-button]:w-8 [&_.fc-button]:!items-center [&_.fc-button]:!justify-center [&_.fc-button]:!rounded-full [&_.fc-button]:!border-none [&_.fc-button]:!bg-gray-200 [&_.fc-button]:sm:h-10 [&_.fc-button]:sm:w-10 [&_.fc-col-header-cell]:!border-none [&_.fc-col-header-cell]:p-1 [&_.fc-col-header-cell]:text-xs [&_.fc-col-header-cell]:font-medium [&_.fc-col-header-cell]:text-gray-600 [&_.fc-col-header-cell]:sm:p-2 [&_.fc-col-header-cell]:sm:text-sm [&_.fc-day-other]:bg-white [&_.fc-day-other_.fc-daygrid-day-number]:text-gray-400 [&_.fc-day-sun_.fc-daygrid-day-number]:text-red-500 [&_.fc-daygrid-day]:min-h-[60px] [&_.fc-daygrid-day]:!border-none [&_.fc-daygrid-day]:sm:min-h-[80px] [&_.fc-daygrid-day-bg]:!border-none [&_.fc-daygrid-day-frame]:!border-none [&_.fc-daygrid-day-number]:!p-0 [&_.fc-daygrid-day-number]:text-sm [&_.fc-daygrid-day-number]:font-medium [&_.fc-daygrid-day-number]:text-gray-700 [&_.fc-daygrid-day-number]:sm:text-base [&_.fc-daygrid-day-top]:flex [&_.fc-daygrid-day-top]:items-center [&_.fc-daygrid-day-top]:justify-center [&_.fc-daygrid-day-top]:!border-none [&_.fc-header-toolbar]:rounded-t-lg [&_.fc-header-toolbar]:p-3 [&_.fc-icon-chevron-left]:text-black [&_.fc-icon-chevron-right]:text-black [&_.fc-scrollgrid]:!border-none [&_.fc-scrollgrid_table]:!border-none [&_.fc-scrollgrid_table_tbody]:!border-none [&_.fc-scrollgrid_table_td]:!border-none [&_.fc-scrollgrid_table_th]:!border-none [&_.fc-scrollgrid_table_thead]:!border-none [&_.fc-scrollgrid_table_tr]:!border-none [&_.fc-scrollgrid-section]:!border-none [&_.fc-scrollgrid-section-body]:!border-none [&_.fc-scrollgrid-section-footer]:!border-none [&_.fc-scrollgrid-section-header]:!border-none [&_.fc-table]:!border-collapse [&_.fc-table]:!border-spacing-0 [&_.fc-table]:!border-none [&_.fc-table_td]:!border-none [&_.fc-table_th]:!border-none [&_.fc-toolbar]:!mb-1 [&_.fc-toolbar]:flex [&_.fc-toolbar]:items-center [&_.fc-toolbar]:justify-between [&_.fc-toolbar-chunk]:flex [&_.fc-toolbar-chunk]:items-center [&_.fc-toolbar-chunk]:gap-2 [&_.fc-toolbar-chunk]:rounded-4xl [&_.fc-toolbar-title]:!text-sm [&_.fc-toolbar-title]:!leading-none [&_.fc-toolbar-title]:font-semibold [&_.fc-toolbar-title]:text-gray-800 [&_.fc-toolbar-title]:sm:!text-base [&_.fc-toolbar-title]:md:!text-lg`}
        >
          -감정을 클릭하면 일기를 볼 수 있어요-
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
              onMonthChange(year, month);
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
                        onDiaryClick(dailyInfo);
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
                        className="mt-1.5 h-8 w-8 hover:drop-shadow-[0_3px_6px_rgba(0,0,0,0.3)] sm:h-12 sm:w-12"
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
  );
}
