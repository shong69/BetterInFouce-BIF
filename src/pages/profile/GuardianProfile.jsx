import { useState, useEffect } from "react";
import { useUserStore } from "@stores/userStore";
import { useStatsStore } from "@stores/statsStore";
import { useToastStore } from "@stores/toastStore";
import { Navigate } from "react-router-dom";

import Header from "@components/common/Header";
import BaseButton from "@components/ui/BaseButton";
import TabBar from "@components/common/TabBar";

import { IoPerson, IoLogOut, IoPencil } from "react-icons/io5";

export default function GuardianProfile() {
  const { user, logout, changeNickname, withdraw } = useUserStore();
  const { stats, fetchGuardianStats } = useStatsStore();
  const { addToast } = useToastStore();

  const [newNickname, setNewNickname] = useState("");
  const [withdrawNickname, setWithdrawNickname] = useState("");
  const [nicknameError, setNicknameError] = useState("");
  const [withdrawError, setWithdrawError] = useState("");

  useEffect(() => {
    if (user?.bifId) {
      fetchGuardianStats(user.bifId);
    }
  }, [user?.bifId, fetchGuardianStats]);

  if (user?.userRole !== "GUARDIAN") {
    return <Navigate to="/guardian-profile" replace />;
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
          message: "닉네임이 성공적으로 변경되었습니다!",
          duration: 3000,
          position: "top-center",
        });

        setNewNickname("");
        setNicknameError("");

        setTimeout(() => {
          window.location.reload();
        }, 1000);
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
        <div className="flex min-h-screen items-center justify-center bg-white">
          <div className="text-gray-600">사용자 정보를 불러오는 중...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <div className="flex min-h-screen flex-col font-['Pretendard']">
        <Header />
        <div className="flex-1 bg-white">
          <div className="mx-auto max-w-4xl p-2 sm:p-4">
            <div className="mb-6 rounded-lg bg-white p-4">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-bold text-gray-800">마이페이지</h2>
              </div>

              <div className="rounded-lg border-1 border-gray-300 bg-white p-4 shadow-sm">
                <div className="flex flex-col space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-200">
                      <IoPerson className="h-5 w-5 text-gray-600" />
                    </div>

                    <button
                      onClick={handleLogout}
                      className="flex items-center space-x-1 rounded bg-gray-200 px-3 py-2 text-sm text-gray-800 transition-colors hover:bg-gray-400"
                    >
                      <IoLogOut className="h-4 w-4" />
                      <span>로그아웃</span>
                    </button>
                  </div>

                  <div className="min-w-0">
                    <h3 className="truncate text-lg font-semibold text-gray-800">
                      {user?.nickname &&
                      user.nickname.length > 0 &&
                      !user.nickname.includes("Wx")
                        ? `${user.nickname} 님`
                        : "보호자 님"}
                    </h3>
                    <div className="flex flex-col space-y-1 sm:flex-row sm:space-y-0 sm:space-x-4">
                      <p className="text-sm whitespace-nowrap text-gray-600">
                        가입일:{" "}
                        {(() => {
                          const dateStr = stats?.guardianJoinDate;
                          if (!dateStr) return "가입일을 불러올 수 없습니다";
                          const d = new Date(dateStr);
                          if (isNaN(d.getTime()))
                            return "가입일을 불러올 수 없습니다";
                          return d.toLocaleDateString("ko-KR", {
                            year: "numeric",
                            month: "long",
                            day: "numeric",
                          });
                        })()}
                      </p>
                      <p className="text-sm whitespace-nowrap text-gray-600" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="mb-20">
              <div className="mb-6 rounded-lg bg-white p-4">
                <div className="ml-4 flex items-center">
                  <IoPencil className="mr-2 h-6 w-6 text-blue-500" />
                  <h2 className="text-lg font-bold text-gray-800">
                    회원정보 수정
                  </h2>
                </div>
                <div className="mt-4 space-y-6">
                  <div className="rounded-lg border-1 border-gray-300 bg-white p-4 shadow-sm">
                    <h4 className="text-md mb-4 font-bold text-gray-800">
                      닉네임 변경
                    </h4>
                    <div className="space-y-3">
                      <input
                        type="text"
                        value={newNickname}
                        onChange={(e) => setNewNickname(e.target.value)}
                        placeholder="새로운 닉네임을 입력해주세요."
                        className="w-full rounded-lg border-1 border-gray-300 p-3 text-sm shadow-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
                      />
                      {nicknameError && (
                        <p className="mt-2 text-center text-sm text-red-500">
                          {nicknameError}
                        </p>
                      )}
                      <div className="flex justify-end">
                        <BaseButton
                          onClick={handleNicknameChange}
                          title="변경"
                          variant="primary"
                          className="text-md w-24"
                        />
                      </div>
                    </div>
                  </div>

                  <div className="rounded-lg border-1 border-gray-300 bg-white p-4">
                    <h4 className="text-md mb-4 font-bold text-gray-800">
                      회원 탈퇴
                    </h4>
                    <div className="space-y-3">
                      <div className="rounded-lg border-1 border-red-300 bg-red-50 p-4 shadow-sm">
                        <p className="text-sm text-red-700">
                          정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.
                        </p>
                      </div>
                      <input
                        type="text"
                        value={withdrawNickname}
                        onChange={(e) => setWithdrawNickname(e.target.value)}
                        placeholder="닉네임을 입력해주세요."
                        className="w-full rounded-lg border-1 border-gray-300 p-3 text-sm shadow-sm focus:ring-2 focus:ring-red-500 focus:outline-none"
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
                          className="text-md w-24"
                        />
                      </div>
                    </div>
                  </div>
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
