import { useState, useEffect, useRef, useCallback } from "react";
import { useStatsStore } from "@stores/statsStore";
import { useUserStore } from "@stores/userStore";
import { useToastStore } from "@stores/toastStore";

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
import BaseButton from "@components/ui/BaseButton";
import Modal from "@components/ui/Modal";

import {
  IoPerson,
  IoStatsChart,
  IoPencil,
  IoSparkles,
  IoLogOut,
} from "react-icons/io5";

import turtleImage from "@assets/logo2.png";

// 색상은 감정 이모지 맞춰서 변경하기
const emotionColors = {
  OKAY: "#E0E0E0",
  GOOD: "#FF9800",
  ANGRY: "#F44336",
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

const _MONTH_NAMES = [
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

export default function BifProfile() {
  const { user, logout, changeNickname, withdraw } = useUserStore();
  const { stats, loading, fetchMonthlyStats } = useStatsStore();
  const { addToast } = useToastStore();

  const donutChartRef = useRef(null);
  const keywordChartRef = useRef(null);
  const monthlyChartRef = useRef(null);

  const [showUserInfoModal, setShowUserInfoModal] = useState(false);
  const [activeTab, setActiveTab] = useState("nickname");
  const [newNickname, setNewNickname] = useState("");
  const [withdrawNickname, setWithdrawNickname] = useState("");
  const [nicknameError, setNicknameError] = useState("");
  const [withdrawError, setWithdrawError] = useState("");

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

  function createKeywordChartData(topKeywords) {
    const limitedKeywords = topKeywords.slice(0, 5);
    return {
      labels: limitedKeywords.map((item) => item.keyword),
      datasets: [
        {
          label: "사용 횟수",
          data: limitedKeywords.map((item) => item.count),
          backgroundColor: "#4CAF50",
          borderColor: "#388E3C",
          borderWidth: 1,
          borderRadius: 4,
          borderSkipped: false,
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
          borderColor: "#388E3C",
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

  const keywordOptions = {
    indexAxis: "y",
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          title: function (context) {
            return `키워드: ${context[0].label}`;
          },
          label: function (context) {
            return `사용 횟수: ${context.parsed.x}회`;
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        title: { display: true, text: "사용 횟수" },
        grid: { display: true, color: "#E0E0E0" },
      },
      y: {
        title: { display: true, text: "키워드" },
        grid: { display: false },
      },
    },
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
        title: { display: true, text: "감정" },
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        title: { display: true, text: "횟수" },
        grid: { color: "#E0E0E0" },
      },
    },
  };

  const handleLoadUserStats = useCallback(async () => {
    try {
      if (user?.bifId) {
        const now = new Date();
        await fetchMonthlyStats(
          user.bifId,
          now.getFullYear(),
          now.getMonth() + 1,
        );
      }
    } catch {
      addToast({
        type: "error",
        message: " 통계 데이터를 불러오는데 실패했습니다.",
        duration: 3000,
        position: "top-center",
      });
    }
  }, [fetchMonthlyStats, user?.bifId, addToast]);

  useEffect(() => {
    if (user?.bifId) {
      handleLoadUserStats();
    }
  }, [handleLoadUserStats, user?.bifId]);

  function handleOpenUserInfoModal() {
    setShowUserInfoModal(true);
  }

  function handleCloseUserInfoModal() {
    setShowUserInfoModal(false);
    setActiveTab("nickname");
    setNewNickname("");
    setWithdrawNickname("");
    setNicknameError("");
    setWithdrawError("");
  }

  function handleTabChange(tabType) {
    setActiveTab(tabType);
    setNicknameError("");
    setWithdrawError("");
  }

  async function handleNicknameChange() {
    if (!newNickname.trim()) {
      setNicknameError("닉네임을 입력해주세요.");
      return;
    }

    try {
      const result = await changeNickname(newNickname.trim());
      if (result.success) {
        addToast({
          type: "success",
          message: result.message,
          duration: 3000,
          position: "top-center",
        });
        handleCloseUserInfoModal();

        window.location.reload();
      } else {
        setNicknameError(result.message || "닉네임 변경에 실패했습니다.");
      }
    } catch {
      setNicknameError("닉네임 변경 중 오류가 발생했습니다.");
    }
  }

  function handleAuthConfirm() {
    handleCloseUserInfoModal();
  }

  async function handleWithdraw() {
    if (!withdrawNickname.trim()) {
      setWithdrawError("닉네임을 입력해주세요.");
      return;
    }

    if (withdrawNickname.trim() !== user?.nickname) {
      setWithdrawError("닉네임이 일치하지 않습니다. 다시 입력해주세요.");
      return;
    }

    try {
      const result = await withdraw();

      if (result.success) {
        handleCloseUserInfoModal();
        await logout();
        window.location.href = "/login";
      } else {
        setWithdrawError(result.message || "회원탈퇴에 실패했습니다.");
      }
    } catch {
      setWithdrawError(
        "회원 탈퇴 중 오류가 발생했습니다. 백엔드를 확인해주세요.",
      );
    }
  }

  function handleLogout() {
    logout();
    window.location.href = "/login";
  }

  const currentMonth = new Date().getMonth() + 1;
  const monthNames = [
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
      <Header />

      <div className="min-h-screen bg-gray-50 px-4 pb-20">
        <div className="mb-6 rounded-lg bg-white p-4 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-xl font-bold text-gray-800">마이페이지</h2>

            <button
              onClick={handleOpenUserInfoModal}
              className="flex items-center space-x-2 rounded-lg bg-gray-200 px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-300"
            >
              <IoPencil className="h-4 w-4" />
              <span>회원정보 수정</span>
            </button>
          </div>

          <div className="relative rounded-lg bg-white p-4 shadow-sm">
            <div className="flex items-center space-x-4">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-200">
                <IoPerson className="h-8 w-8 text-gray-600" />
              </div>

              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-800">
                  {stats?.nickname || user?.nickname || "BIF"} 님
                </h3>
                <p className="text-sm text-gray-600">
                  가입일:{" "}
                  {(() => {
                    if (!stats?.joinDate) return "2025년 8월 1일";
                    try {
                      const date = new Date(stats.joinDate);
                      if (isNaN(date.getTime())) return "2025년 08월 01일";
                      return date.toLocaleDateString("ko-KR", {
                        year: "numeric",
                        month: "long",
                        day: "numeric",
                      });
                    } catch {
                      return "2025년 08월 01일";
                    }
                  })()}
                </p>
                <p className="text-sm text-gray-600">
                  작성한 일기: {stats?.totalDiaryCount || 0}개
                </p>
              </div>

              <button
                onClick={handleLogout}
                className="flex items-center space-x-1 rounded bg-gray-100 px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-200"
              >
                <IoLogOut className="h-4 w-4" />
                <span>로그아웃</span>
              </button>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="flex items-center">
            <IoStatsChart className="mr-2 h-6 w-6 text-blue-500" />
            <h2 className="text-xl font-bold text-gray-800">
              {monthNames[currentMonth - 1]}의 감정 통계
            </h2>
          </div>

          {loading && (
            <div className="rounded-lg bg-white p-6 text-center shadow-sm">
              <LoadingSpinner />
              <p className="mt-2 text-gray-600">통계 데이터를 불러오는 중...</p>
            </div>
          )}

          {!loading && (
            <>
              <div className="rounded-lg bg-white p-4 shadow-sm">
                <h3 className="mb-4 text-lg font-semibold text-gray-800">
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

              <div className="rounded-lg bg-white p-4 shadow-sm">
                <h3 className="mb-4 text-lg font-semibold text-gray-800">
                  자주 사용된 키워드 TOP 5
                </h3>
                <div className="h-80 w-full">
                  <Bar
                    ref={keywordChartRef}
                    data={
                      stats?.topKeywords && stats.topKeywords.length > 0
                        ? createKeywordChartData(stats.topKeywords)
                        : createKeywordChartData([
                            { keyword: "가족", count: 0 },
                            { keyword: "직장", count: 0 },
                            { keyword: "친구", count: 0 },
                            { keyword: "휴식", count: 0 },
                            { keyword: "건강", count: 0 },
                          ])
                    }
                    options={keywordOptions}
                  />
                </div>
              </div>

              <div className="rounded-lg bg-white p-4 shadow-sm">
                <h3 className="mb-4 text-lg font-semibold text-gray-800">
                  지난달 대비 감정 변화
                </h3>
                <div className="h-80 w-full">
                  <Bar
                    ref={monthlyChartRef}
                    data={
                      stats?.monthlyChange && stats.monthlyChange.length > 0
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

              <div className="rounded-lg bg-white p-4 shadow-sm">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
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

                  <div className="flex-1 rounded-lg border-l-4 border-blue-300 bg-gradient-to-b from-blue-100 to-blue-200 p-4">
                    <h3 className="mb-2 text-lg font-semibold text-gray-800">
                      현명한 거북이
                    </h3>
                    <p className="leading-relaxed text-gray-700">
                      {stats?.statisticsText ||
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

      <Modal isOpen={showUserInfoModal} onClose={handleCloseUserInfoModal}>
        <div className="mx-auto w-full max-w-md">
          <h2 className="mb-6 text-center text-xl font-bold">회원정보 수정</h2>

          <div className="mb-6 flex border-b border-gray-200">
            <button
              onClick={() => handleTabChange("nickname")}
              className={`flex-1 border-b-2 py-2 text-sm font-medium transition-colors ${
                activeTab === "nickname"
                  ? "border-green-500 text-green-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              닉네임 변경
            </button>
            <button
              onClick={() => handleTabChange("auth")}
              className={`flex-1 border-b-2 py-2 text-sm font-medium transition-colors ${
                activeTab === "auth"
                  ? "border-green-500 text-green-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              인증 번호 확인
            </button>
            <button
              onClick={() => handleTabChange("withdraw")}
              className={`flex-1 border-b-2 py-2 text-sm font-medium transition-colors ${
                activeTab === "withdraw"
                  ? "border-green-500 text-green-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              회원 탈퇴
            </button>
          </div>

          <div className="space-y-4">
            {activeTab === "nickname" && (
              <div>
                <input
                  type="text"
                  value={newNickname}
                  onChange={(e) => setNewNickname(e.target.value)}
                  placeholder="새로운 닉네임을 입력해주세요."
                  className="w-full rounded-lg border border-gray-300 p-3 focus:ring-2 focus:ring-green-500 focus:outline-none"
                />
                {nicknameError && (
                  <p className="mt-2 text-center text-sm text-red-500">
                    {nicknameError}
                  </p>
                )}
                <div className="mt-4 flex space-x-3">
                  <BaseButton
                    onClick={handleCloseUserInfoModal}
                    title="취소"
                    variant="secondary"
                    className="flex-1"
                  />
                  <BaseButton
                    onClick={handleNicknameChange}
                    title="변경"
                    variant="primary"
                    className="flex-1"
                  />
                </div>
              </div>
            )}

            {activeTab === "auth" && (
              <div className="text-center">
                <div className="mb-4 rounded-lg bg-gray-100 p-6">
                  <p className="mb-2 text-sm text-gray-600">
                    보호자 연결용 인증번호
                  </p>
                  <p className="text-2xl font-bold tracking-wider text-gray-800">
                    {stats?.connectionCode || "인증번호를 불러올 수 없습니다"}
                  </p>
                </div>
                <BaseButton
                  onClick={handleAuthConfirm}
                  title="확인"
                  variant="primary"
                  className="w-full"
                />
              </div>
            )}

            {activeTab === "withdraw" && (
              <div>
                <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-4">
                  <p className="text-sm text-red-700">
                    정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.
                  </p>
                </div>
                <input
                  type="text"
                  value={withdrawNickname}
                  onChange={(e) => setWithdrawNickname(e.target.value)}
                  placeholder="닉네임을 입력해주세요."
                  className="w-full rounded-lg border border-gray-300 p-3 focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
                {withdrawError && (
                  <p className="text-center text-sm text-red-500">
                    {withdrawError}
                  </p>
                )}
                <div className="mt-4 flex space-x-3">
                  <BaseButton
                    onClick={handleCloseUserInfoModal}
                    title="취소"
                    variant="secondary"
                    className="flex-1"
                  />
                  <BaseButton
                    onClick={handleWithdraw}
                    title="탈퇴"
                    variant="danger"
                    className="flex-1"
                  />
                </div>
              </div>
            )}
          </div>
        </div>
      </Modal>
    </>
  );
}
