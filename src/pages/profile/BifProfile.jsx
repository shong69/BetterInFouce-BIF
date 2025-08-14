import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useStatsStore } from "@stores/statsStore";

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
import PrimaryButton from "@components/ui/PrimaryButton";
import SecondaryButton from "@components/ui/SecondaryButton";
import Modal from "@components/ui/Modal";

import { IoPerson, IoStatsChart, IoPencil, IoSparkles } from "react-icons/io5";

import turtleImage from "@assets/logo2.png";

const DEFAULT_BIF_ID = "1";
const DEFAULT_BIF_USER = {
  nickname: "BIF",
  joinDate: "2023년 12월 12일",
  diaryCount: 45,
};

export default function BifProfile() {
  const navigate = useNavigate();

  const donutChartRef = useRef(null);
  const keywordChartRef = useRef(null);
  const monthlyChartRef = useRef(null);

  const emotionColors = {
    OKAY: "#FFB74D",
    GOOD: "#4CAF50",
    ANGRY: "#F44336",
    DOWN: "#9C27B0",
    GREAT: "#2196F3",
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

  function createKeywordChartData(topKeywords) {
    const limitedKeywords = topKeywords.slice(0, 5);
    return {
      labels: limitedKeywords.map((item) => item.keyword),
      datasets: [
        {
          label: "사용 횟수",
          data: limitedKeywords.map((item) => item.count),
          backgroundColor: [
            "#FF6B6B",
            "#4ECDC4",
            "#45B7D1",
            "#96CEB4",
            "#FFEAA7",
          ],
          borderColor: ["#FF5252", "#26A69A", "#1976D2", "#66BB6A", "#FFC107"],
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
          backgroundColor: monthlyChange.map(
            (item) => emotionColors[item.emotion] || "#CCCCCC",
          ),
          borderColor: monthlyChange.map(
            (item) => emotionColors[item.emotion] || "#CCCCCC",
          ),
          borderWidth: 2,
          borderRadius: 4,
          borderSkipped: false,
        },
        {
          label: "지난 달",
          data: monthlyChange.map((item) => item.previousValue || 0),
          backgroundColor: monthlyChange.map((item) => {
            const color = emotionColors[item.emotion] || "#CCCCCC";
            return color + "80";
          }),
          borderColor: monthlyChange.map(
            (item) => emotionColors[item.emotion] || "#CCCCCC",
          ),
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

  const [user, setUser] = useState(DEFAULT_BIF_USER);

  const [showUserInfoModal, setShowUserInfoModal] = useState(false);
  const [activeTab, setActiveTab] = useState("nickname");
  const [newNickname, setNewNickname] = useState("");
  const [withdrawNickname, setWithdrawNickname] = useState("");
  const [nicknameError, setNicknameError] = useState("");
  const [withdrawError, setWithdrawError] = useState("");
  const authCode = "AbC456";

  const handleLoadUserStats = useCallback(async () => {
    try {
      const bifId = localStorage.getItem("bifId") || DEFAULT_BIF_ID;

      const now = new Date();

      await fetchMonthlyStats(bifId, now.getFullYear(), now.getMonth() + 1);
    } catch (err) {
      throw ("사용자 통계 데이터 로드 실패:", err);
    }
  }, [fetchMonthlyStats]);

  useEffect(() => {
    localStorage.setItem("userType", "BIF");

    setUser(DEFAULT_BIF_USER);

    handleLoadUserStats();
  }, [handleLoadUserStats]);

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

  function handleNicknameChange() {
    if (!newNickname.trim()) {
      setNicknameError("닉네임을 입력해주세요.");
      return;
    }

    if (newNickname.trim() === "test") {
      setNicknameError("이미 존재하는 닉네임입니다.");
      return;
    }

    handleCloseUserInfoModal();
  }

  function handleAuthConfirm() {
    handleCloseUserInfoModal();
  }

  function handleWithdraw() {
    if (!withdrawNickname.trim()) {
      setWithdrawError("닉네임을 입력해주세요.");
      return;
    }

    if (withdrawNickname.trim() !== "BIF") {
      setWithdrawError("닉네임이 일치하지 않습니다. 다시 입력해주세요.");
      return;
    }

    handleCloseUserInfoModal();
  }

  function handleLogout() {
    localStorage.removeItem("bifId");
    localStorage.removeItem("userToken");
    navigate("/login");
  }

  return (
    <>
      <Header />

      <div className="min-h-screen bg-white px-4 pb-20 md:px-6">
        {/* 프로필 정보 섹션 */}
        <div className="mb-6 rounded-lg bg-gray-50 p-4 md:p-6">
          {/* 프로필 섹션 헤더 */}
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="text-lg font-bold md:text-xl">마이페이지</h2>

            <button
              onClick={handleOpenUserInfoModal}
              className="flex w-full items-center justify-center space-x-2 rounded-lg bg-gray-300 px-3 py-2 text-sm text-white transition-colors hover:bg-gray-400 sm:w-auto"
            >
              <IoPencil className="h-4 w-4" />
              <span>회원정보 수정</span>
            </button>
          </div>

          {/* 사용자 정보 카드 */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            {/* 사용자 프로필 이미지 */}
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-300">
              <IoPerson className="h-8 w-8 text-gray-600" />
            </div>

            {/* 사용자 정보 텍스트 영역 */}
            <div className="flex-1 text-center sm:text-left">
              <h3 className="text-lg font-semibold">{user.nickname} 님</h3>
              <p className="text-sm text-gray-600">가입일: {user.joinDate}</p>
              <p className="text-sm text-gray-600">
                작성한 일기: {user.diaryCount}개
              </p>
            </div>

            {/* 로그아웃 버튼 */}
            <button
              onClick={handleLogout}
              className="w-full rounded bg-gray-100 px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-200 sm:w-auto"
            >
              로그아웃
            </button>
          </div>
        </div>

        {/* 통계 섹션 */}
        <div className="space-y-6">
          {/* 통계 섹션 헤더 */}
          <div className="mb-4 flex items-center">
            <IoStatsChart className="mr-2 h-6 w-6 text-blue-500" />
            <h2 className="text-xl font-bold">12월의 감정 통계</h2>
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

              {/* 키워드 Top5 섹션 */}
              {stats.topKeywords && stats.topKeywords.length > 0 && (
                <div className="rounded-lg bg-white p-4 shadow-sm md:p-6">
                  <h3 className="mb-4 text-lg font-semibold">
                    자주 사용된 키워드 TOP 5
                  </h3>
                  <div className="h-64 w-full md:h-80">
                    <Bar
                      ref={keywordChartRef}
                      data={createKeywordChartData(stats.topKeywords)}
                      options={keywordOptions}
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

      <TabBar />

      {/* 회원정보 수정 모달 */}
      <Modal isOpen={showUserInfoModal} onClose={handleCloseUserInfoModal}>
        <div className="mx-auto w-full max-w-md">
          <h2 className="mb-6 text-center text-xl font-bold">회원정보 수정</h2>

          {/* 탭 버튼들 */}
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

          {/* 탭 내용 */}
          <div className="space-y-4">
            {/* 닉네임 변경 탭 */}
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
                  <p className="mt-2 text-sm text-red-500">{nicknameError}</p>
                )}
                <div className="mt-4 flex space-x-3">
                  <SecondaryButton
                    onClick={handleCloseUserInfoModal}
                    className="flex-1"
                  >
                    취소
                  </SecondaryButton>
                  <PrimaryButton
                    onClick={handleNicknameChange}
                    className="flex-1"
                  >
                    변경
                  </PrimaryButton>
                </div>
              </div>
            )}

            {/* 인증 번호 확인 탭 */}
            {activeTab === "auth" && (
              <div className="text-center">
                <div className="mb-4 rounded-lg bg-gray-100 p-6">
                  <p className="text-2xl font-bold tracking-wider text-gray-800">
                    {authCode}
                  </p>
                </div>
                <PrimaryButton onClick={handleAuthConfirm} className="w-full">
                  확인
                </PrimaryButton>
              </div>
            )}

            {/* 회원 탈퇴 탭 */}
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
                  <p className="mt-2 text-sm text-red-500">{withdrawError}</p>
                )}
                <div className="mt-4 flex space-x-3">
                  <SecondaryButton
                    onClick={handleCloseUserInfoModal}
                    className="flex-1"
                  >
                    취소
                  </SecondaryButton>
                  <button
                    onClick={handleWithdraw}
                    className="flex-1 rounded-lg bg-red-500 px-4 py-2 text-white transition-colors hover:bg-red-600"
                  >
                    탈퇴
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* 전체 화면 로딩 스피너 */}
      {loading && <LoadingSpinner />}
    </>
  );
}
