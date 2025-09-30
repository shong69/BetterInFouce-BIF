import { useEffect, useRef, useCallback } from "react";
import { useStatsStore } from "@stores/statsStore";
import { useUserStore } from "@stores/userStore";
import { useToastStore } from "@stores/toastStore";
import { Navigate } from "react-router-dom";

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
import TabBar from "@components/common/TabBar";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import { IoStatsChart } from "react-icons/io5";
import turtleImage from "@assets/logo2.png";

const EMOTION_COLORS = {
  OKAY: "#9CCC65",
  GOOD: "#F06292",
  ANGRY: "#E55A2B",
  DOWN: "#4A90E2",
  GREAT: "#FFD54F",
};

const EMOTION_LABELS = {
  OKAY: "평범",
  GOOD: "좋음",
  ANGRY: "화남",
  DOWN: "우울",
  GREAT: "최고",
};

const MONTH_NAMES = [
  "1월",
  "2월",
  "3월",
  "4월",
  "5월",
  "6월",
  "7월",
  "8월",
  "9월",
  "10월",
  "11월",
  "12월",
];

export default function GuardianStats() {
  const { user } = useUserStore();
  const { stats, loading, fetchGuardianStats } = useStatsStore();
  const { addToast } = useToastStore();

  const donutChartRef = useRef(null);
  const monthlyChartRef = useRef(null);

  const handleLoadUserStats = useCallback(async () => {
    try {
      if (user?.bifId) {
        await fetchGuardianStats(user.bifId);
      }
    } catch {
      addToast({
        type: "error",
        message: "통계 데이터를 불러오는데 실패했습니다.",
        duration: 3000,
        position: "top-center",
      });
    }
  }, [fetchGuardianStats, user?.bifId, addToast]);

  useEffect(() => {
    handleLoadUserStats();
  }, [handleLoadUserStats]);

  if (user?.userRole !== "GUARDIAN") {
    return <Navigate to="/bif-profile" replace />;
  }

  function createDonutChartData(emotionRatio) {
    return {
      labels: emotionRatio.map(
        (item) => EMOTION_LABELS[item.emotion] || item.emotion,
      ),
      datasets: [
        {
          data: emotionRatio.map((item) => item.value),
          backgroundColor: emotionRatio.map(
            (item) => EMOTION_COLORS[item.emotion] || "#CCCCCC",
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
        (item) => EMOTION_LABELS[item.emotion] || item.emotion,
      ),
      datasets: [
        {
          label: "지난달",
          data: monthlyChange.map((item) => item.previousValue || 0),
          backgroundColor: "#D1D5DB",
          borderColor: "#9CA3AF",
          borderWidth: 1,
          borderRadius: 4,
          borderSkipped: false,
        },
        {
          label: "이번달",
          data: monthlyChange.map((item) => item.value),
          backgroundColor: monthlyChange.map(
            (item) => EMOTION_COLORS[item.emotion] || "#4CAF50",
          ),
          borderColor: monthlyChange.map(
            (item) => EMOTION_COLORS[item.emotion] || "#4CAF50",
          ),
          borderWidth: 2,
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
          font: { size: 11 },
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
            return `${context.dataset.label}: ${context.parsed.y}회`;
          },
        },
      },
    },
    scales: {
      x: {
        title: { display: false },
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        title: { display: false },
        grid: { color: "#E0E0E0" },
      },
    },
  };

  const currentMonth = new Date().getMonth() + 1;

  if (!user) {
    return (
      <>
        <Header />
        <div className="flex min-h-screen items-center justify-center bg-gray-50">
          <LoadingSpinner />
        </div>
      </>
    );
  }

  return (
    <>
      <div className="flex min-h-screen flex-col font-['Pretendard']">
        <Header />
        <div className="flex-1">
          <div className="mx-auto max-w-4xl p-2 sm:p-4">
            <div className="rounded-lg p-4">
              <div className="space-y-6">
                <div className="flex items-center">
                  <IoStatsChart className="mr-2 h-6 w-6 text-blue-500" />
                  <h2 className="text-lg font-bold text-gray-800">
                    {MONTH_NAMES[currentMonth - 1]}의 감정 통계
                  </h2>
                </div>

                {loading ? (
                  <div className="rounded-lg bg-white p-6 text-center shadow-sm">
                    <LoadingSpinner />
                    <p className="mt-2 text-gray-600">
                      통계 데이터를 불러오는 중...
                    </p>
                  </div>
                ) : (
                  <>
                    <div className="rounded-lg border-1 border-gray-300 bg-white p-4 shadow-sm">
                      <h3 className="text-md mb-4 font-bold text-gray-800">
                        이번 달 감정 비율
                      </h3>
                      <div className="h-64 w-full">
                        <Doughnut
                          ref={donutChartRef}
                          data={
                            stats?.emotionRatio && stats.emotionRatio.length > 0
                              ? createDonutChartData(stats.emotionRatio)
                              : createDonutChartData([
                                  { emotion: "OKAY", value: 0 },
                                  { emotion: "GOOD", value: 0 },
                                  { emotion: "ANGRY", value: 0 },
                                  { emotion: "DOWN", value: 0 },
                                  { emotion: "GREAT", value: 0 },
                                ])
                          }
                          options={donutOptions}
                        />
                      </div>
                    </div>

                    <div className="rounded-lg border-1 border-gray-300 bg-white p-4 shadow-sm">
                      <h3 className="text-md mb-4 font-bold text-gray-800">
                        지난달 대비 감정 변화
                      </h3>
                      <div className="h-80 w-full">
                        <Bar
                          ref={monthlyChartRef}
                          data={
                            stats?.monthlyChange &&
                            stats.monthlyChange.length > 0
                              ? createMonthlyChartData(stats.monthlyChange)
                              : createMonthlyChartData([
                                  { emotion: "OKAY", value: 0 },
                                  { emotion: "GOOD", value: 0 },
                                  { emotion: "ANGRY", value: 0 },
                                  { emotion: "DOWN", value: 0 },
                                  { emotion: "GREAT", value: 0 },
                                ])
                          }
                          options={monthlyOptions}
                        />
                      </div>
                    </div>

                    <div className="mb-16 rounded-lg p-1">
                      <div className="flex w-full justify-start">
                        <div className="max-w_full flex items-start gap-2">
                          <img
                            src={turtleImage}
                            alt="현명한 거북이"
                            className="h-7 w-7"
                          />
                          <div className="w-full max-w-full rounded-2xl rounded-tl-md bg-linear-to-b from-[#DAEAF8] to-[#F7E6FF] px-4 py-3 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
                            <div className="mb-2 flex items-center gap-2">
                              <span className="text-sm font-semibold text-gray-800">
                                현명한 거북이
                              </span>
                            </div>
                            <span className="block text-sm text-gray-600">
                              {stats?.guardianAdviceText ||
                                stats?.advice ||
                                "지난달에 작성한 일기가 없습니다."}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>

          <TabBar />
        </div>
      </div>
    </>
  );
}
