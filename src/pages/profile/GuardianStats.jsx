import { useEffect, useRef, useCallback } from "react";
import { useStatsStore } from "@stores/statsStore";
import { useNavigate } from "react-router-dom";

import {
  Chart,
  ArcElement,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
  Legend,
} from "chart.js";
import { Doughnut, Bar } from "react-chartjs-2";
Chart.register(
  ArcElement,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
  Legend,
);

import Header from "@components/common/Header";
import LoadingSpinner from "@components/ui/LoadingSpinner";

import {
  IoStatsChart,
  IoSparkles,
  IoBook,
  IoCalendar,
  IoBarChart,
} from "react-icons/io5";

import turtleImage from "@assets/logo2.png";

const DEFAULT_BIF_ID = "1";

export default function GuardianStats() {
  const navigate = useNavigate();

  const donutChartRef = useRef(null);
  const monthlyChartRef = useRef(null);

  const emotionColors = {
    OKAY: "#87CEEB",
    ANGRY: "#F44336",
    GOOD: "#FF9800",
    DOWN: "#9C27B0",
    GREAT: "#FFEB3B",
  };

  const emotionLabels = {
    OKAY: "평범",
    GOOD: "좋음",
    ANGRY: "화남",
    DOWN: "우울",
    GREAT: "최고",
  };

  function createDonutChartData(emotionRatio) {
    return {
      labels: emotionRatio.map(
        (item) => emotionLabels[item.emotion] || item.emotion,
      ),
      datasets: [
        {
          data: emotionRatio.map((item) => item.value),
          backgroundColor: emotionRatio.map(
            (item) => emotionColors[item.emotion] || "#CCCCCC",
          ),
          borderWidth: 2,
          borderColor: "#FFFFFF",
          hoverBorderWidth: 3,
          hoverBorderColor: "#FFFFFF",
        },
      ],
    };
  }

  function createMonthlyChartData(monthlyChange) {
    return {
      labels: monthlyChange.map(
        (item) => emotionLabels[item.emotion] || item.emotion,
      ),
      datasets: [
        {
          label: "이번 달",
          data: monthlyChange.map((item) => item.value),
          backgroundColor: "#4CAF50",
          borderColor: "#4CAF50",
          borderWidth: 2,
          borderRadius: 4,
          borderSkipped: false,
        },
        {
          label: "지난 달",
          data: monthlyChange.map((item) => item.previousValue || 0),
          backgroundColor: "#E0E0E0",
          borderColor: "#E0E0E0",
          borderWidth: 1,
          borderRadius: 4,
          borderSkipped: false,
        },
      ],
    };
  }

  const donutOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: "bottom",
        labels: {
          padding: 20,
          usePointStyle: true,
          font: { size: 12 },
        },
      },
      tooltip: {
        callbacks: {
          label: function (context) {
            const total = context.dataset.data.reduce((a, b) => a + b, 0);
            const percentage = ((context.parsed / total) * 100).toFixed(1);
            return `${context.label}: ${context.parsed}회 (${percentage}%)`;
          },
        },
      },
    },
    cutout: "60%",
  };

  const monthlyOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: function (context) {
            const changePercentage =
              stats?.monthlyChange[context.dataIndex]?.changePercentage;

            let label = `${context.dataset.label}: ${context.parsed.y}%`;

            if (
              context.dataset.label === "이번 달" &&
              changePercentage !== undefined
            ) {
              const changeText =
                changePercentage > 0
                  ? `(+${changePercentage.toFixed(1)}%)`
                  : `(${changePercentage.toFixed(1)}%)`;
              label += ` ${changeText}`;
            }

            return label;
          },
        },
      },
    },
    scales: {
      x: {
        title: { display: true, text: "감정" },
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        max: 100,
        title: { display: true, text: "비율 (%)" },
        grid: { color: "#E0E0E0" },
      },
    },
  };

  const { stats, loading, error, fetchMonthlyStats } = useStatsStore();

  const handleLoadUserStats = useCallback(async () => {
    try {
      const bifId = localStorage.getItem("bifId") || DEFAULT_BIF_ID;

      const now = new Date();

      await fetchMonthlyStats(bifId, now.getFullYear(), now.getMonth() + 1);
    } catch (err) {
      console.error("사용자 통계 데이터 로드 실패:", err);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem("userType", "GUARDIAN");

    handleLoadUserStats();
  }, [handleLoadUserStats]);

  return (
    <>
      <Header />

      <div className="min-h-screen bg-white px-4 pb-20 md:px-6">
        {/* 통계 섹션 */}
        <div className="space-y-6">
          {/* 통계 섹션 헤더 */}
          <div className="mb-6 flex items-center">
            <IoStatsChart className="mr-2 h-6 w-6 text-blue-500" />
            <h2 className="text-xl font-bold">BIF님의 12월 감정 통계</h2>
          </div>

          {/* 로딩 중일 때 표시할 내용 */}
          {loading && (
            <div className="rounded-lg bg-white p-6 text-center">
              <LoadingSpinner />
              <p className="mt-2 text-gray-600">통계 데이터를 불러오는 중...</p>
            </div>
          )}

          {/* 에러가 있을 때 표시할 내용 */}
          {error && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="text-red-600">{error}</p>
            </div>
          )}

          {/* 통계 데이터가 있고 로딩이 끝났을 때 표시할 내용 */}
          {stats && !loading && (
            <>
              {/* 도넛 차트 섹션 */}
              {stats.emotionRatio && stats.emotionRatio.length > 0 && (
                <div className="rounded-lg bg-white p-4 shadow-sm md:p-6">
                  <h3 className="mb-4 text-lg font-semibold">
                    이번 달 감정 비율
                  </h3>
                  <div className="h-48 w-full md:h-64">
                    <Doughnut
                      ref={donutChartRef}
                      data={createDonutChartData(stats.emotionRatio)}
                      options={donutOptions}
                    />
                  </div>
                </div>
              )}

              {/* 월별 변화 섹션 */}
              {stats.monthlyChange && stats.monthlyChange.length > 0 && (
                <div className="rounded-lg bg-white p-4 shadow-sm md:p-6">
                  <h3 className="mb-4 text-lg font-semibold">
                    지난달 대비 감정 변화
                  </h3>
                  <div className="h-64 w-full md:h-80">
                    <Bar
                      ref={monthlyChartRef}
                      data={createMonthlyChartData(stats.monthlyChange)}
                      options={monthlyOptions}
                    />
                  </div>
                </div>
              )}

              {/* 현명한 거북이 메시지 섹션 */}
              {stats.statisticsText && (
                <div className="rounded-lg bg-white p-4 shadow-sm md:p-6">
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                    {/* 크리스탈 볼 + 거북이 이미지 */}
                    <div className="relative flex justify-center sm:justify-start">
                      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-blue-200 to-purple-200">
                        <img
                          src={turtleImage}
                          alt="현명한 거북이"
                          className="h-8 w-8 object-contain"
                        />
                      </div>
                      <IoSparkles className="absolute -top-1 -right-1 h-4 w-4 text-yellow-400" />
                      <IoSparkles className="absolute -bottom-1 -left-1 h-3 w-3 text-yellow-400" />
                      <IoSparkles className="absolute top-0 -right-2 h-2 w-2 text-yellow-400" />
                      <IoSparkles className="absolute right-0 -bottom-2 h-2 w-2 text-yellow-400" />
                    </div>

                    {/* 메시지 말풍선 */}
                    <div className="flex-1 rounded-lg border-l-4 border-blue-300 bg-gradient-to-b from-blue-100 to-blue-200 p-4 shadow-sm">
                      <h3 className="mb-2 text-lg font-semibold text-gray-800">
                        현명한 거북이
                      </h3>
                      <p className="leading-relaxed text-gray-700">
                        {stats.statisticsText}
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* 커스텀 탭바 */}
      <div className="fixed right-0 bottom-0 left-0 z-50 border-t border-gray-200 bg-white shadow-lg">
        <div className="flex justify-around py-2">
          {/* 통계보기 (활성화) */}
          <button className="flex flex-col items-center text-green-600">
            <div className="relative">
              <IoBook className="h-6 w-6" />
              <div className="absolute -top-1 -left-1 h-2 w-2 rounded-sm bg-green-600" />
            </div>
            <span className="mt-1 text-xs font-medium">통계 보기</span>
          </button>

          {/* 할 일 */}
          <button
            onClick={() => navigate("/")}
            className="flex flex-col items-center text-gray-400 transition-colors hover:text-gray-600"
          >
            <IoCalendar className="h-6 w-6" />
            <span className="mt-1 text-xs">할 일</span>
          </button>

          {/* 시뮬레이션 */}
          <button
            onClick={() => navigate("/simulations")}
            className="flex flex-col items-center text-gray-400 transition-colors hover:text-gray-600"
          >
            <IoBarChart className="h-6 w-6" />
            <span className="mt-1 text-xs">시뮬레이션</span>
          </button>
        </div>
      </div>

      {/* 전체 화면 로딩 스피너 */}
      {loading && <LoadingSpinner />}
    </>
  );
}
