import { useState, useEffect, useCallback } from "react";
import { useUserStore } from "@stores/userStore";
import { useToastStore } from "@stores/toastStore";
import { useStatsStore } from "@stores/statsStore";

import Header from "@components/common/Header";
import BaseButton from "@components/ui/BaseButton";
import TabBar from "@components/common/TabBar";

import { IoPerson, IoLogOut } from "react-icons/io5";

export default function GuardianProfile() {
  const { user, logout, changeNickname, withdraw } = useUserStore();
  const { addToast } = useToastStore();
  const { stats, fetchMonthlyStats } = useStatsStore();

  const [newNickname, setNewNickname] = useState("");
  const [withdrawNickname, setWithdrawNickname] = useState("");
  const [nicknameError, setNicknameError] = useState("");
  const [withdrawError, setWithdrawError] = useState("");

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

        window.location.reload();
      } else {
        setNicknameError(result.message || "닉네임 변경에 실패했습니다.");
      }
    } catch {
      setNicknameError("닉네임 변경 중 오류가 발생했습니다.");
    }
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
    addToast({
      type: "success",
      message: "로그아웃되었습니다.",
      duration: 3000,
      position: "top-center",
    });
    window.location.href = "/login";
  }

  if (!user) {
    return (
      <>
        <Header />
        <div className="flex min-h-screen items-center justify-center bg-gray-50">
          <div className="text-gray-600">사용자 정보를 불러오는 중...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Header />

      <div className="min-h-screen bg-gray-50 px-4 pb-20">
        <div className="space-y-6">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-gray-800">마이페이지</h2>
          </div>

          <div className="rounded-lg bg-white p-4 shadow-sm">
            <div className="flex items-center space-x-4">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-200">
                <IoPerson className="h-8 w-8 text-gray-600" />
              </div>

              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-800">
                  {stats?.nickname || user?.nickname || "보호자"} 님
                </h3>
                <p className="text-sm text-gray-600">
                  가입일:{" "}
                  {(() => {
                    if (!stats?.joinDate) return "가입일을 불러올 수 없습니다";
                    try {
                      const date = new Date(stats.joinDate);
                      if (isNaN(date.getTime()))
                        return "가입일을 불러올 수 없습니다";
                      return date.toLocaleDateString("ko-KR", {
                        year: "numeric",
                        month: "long",
                        day: "numeric",
                      });
                    } catch {
                      return "가입일을 불러올 수 없습니다";
                    }
                  })()}
                </p>
                <p className="text-sm text-gray-600">
                  인증 관계: {stats?.connectionCode || "연결된 BIF 없음"}
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

          <div className="space-y-6">
            <h3 className="text-lg font-semibold text-gray-800">
              회원정보 수정
            </h3>

            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h4 className="text-md mb-4 font-semibold text-gray-800">
                닉네임 변경
              </h4>
              <div className="space-y-3">
                <input
                  type="text"
                  value={newNickname}
                  onChange={(e) => setNewNickname(e.target.value)}
                  placeholder="새로운 닉네임을 입력해주세요."
                  className="w-full rounded-lg border border-gray-300 p-3 focus:ring-2 focus:ring-green-500 focus:outline-none"
                />
                {nicknameError && (
                  <p className="text-center text-sm text-red-500">
                    {nicknameError}
                  </p>
                )}
                <div className="flex justify-end">
                  <BaseButton
                    onClick={handleNicknameChange}
                    title="변경"
                    variant="primary"
                    className="w-24"
                  />
                </div>
              </div>
            </div>

            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h4 className="text-md mb-4 font-semibold text-gray-800">
                회원 탈퇴
              </h4>
              <div className="space-y-3">
                <p className="text-sm text-gray-600">
                  정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.
                </p>
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
                <div className="flex justify-end">
                  <BaseButton
                    onClick={handleWithdraw}
                    title="탈퇴"
                    variant="danger"
                    className="w-24"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <TabBar />
    </>
  );
}
