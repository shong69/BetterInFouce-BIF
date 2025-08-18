import { useEffect, useRef, useCallback } from "react";
import { useStatsStore } from "@stores/statsStore";
import { useUserStore } from "@stores/userStore";

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
import TabBar from "@components/common/TabBar";

import { IoStatsChart, IoSparkles } from "react-icons/io5";

import turtleImage from "@assets/logo2.png";

const DEFAULT_BIF_ID = "1";

export default function GuardianStats() {
  const { user } = useUserStore();

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
      const bifId =
        user?.bifId || localStorage.getItem("bifId") || DEFAULT_BIF_ID;

      const now = new Date();

      await fetchMonthlyStats(bifId, now.getFullYear(), now.getMonth() + 1);
    } catch (err) {
      throw ("사용자 통계 데이터 로드 실패:", err);
    }
  }, [fetchMonthlyStats, user?.bifId]);

  useEffect(() => {
    localStorage.setItem("userType", "GUARDIAN");

    if (user?.bifId) {
      handleLoadUserStats();
    }
  }, [handleLoadUserStats, user?.bifId]);

  return (
    <>
      <Header />

      <div className="min-h-screen bg-white px-4 pb-20 md:px-6">
        <div className="space-y-6">
          <div className="mb-6 flex items-center">
            <IoStatsChart className="mr-2 h-6 w-6 text-blue-500" />
            <h2 className="text-xl font-bold text-gray-800">
              {stats?.nickname || "BIF"}님의 {new Date().getMonth() + 1}월 감정
              통계
            </h2>
          </div>

          {loading && (
            <div className="rounded-lg bg-white p-6 text-center">
              <LoadingSpinner />
              <p className="mt-2 text-gray-600">통계 데이터를 불러오는 중...</p>
            </div>
          )}

          {error && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="text-red-600">{error}</p>
            </div>
          )}

          {stats && !loading && (
            <>
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
              <div className="rounded-lg bg-white p-4 shadow-sm">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                  {/* 크리스탈 볼 + 거북이 이미지 */}
                  <div className="relative flex justify-center sm:flex-row sm:justify-start">
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
                  <div className="flex-1 rounded-lg border-l-4 border-blue-300 bg-gradient-to-b from-blue-100 to-blue-200 p-4">
                    <h3 className="mb-2 text-lg font-semibold text-gray-800">
                      현명한 거북이
                    </h3>
                    <p className="leading-relaxed text-gray-700">
                      {stats?.guardianAdviceText ||
                        "아직 작성된 일기가 없습니다. 첫 번째 일기를 작성해보세요!"}
                    </p>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      <TabBar />
    </>
  );
}
