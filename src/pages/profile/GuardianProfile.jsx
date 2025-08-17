import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useUserStore } from "@stores/userStore";
import { useToastStore } from "@stores/toastStore";

import Header from "@components/common/Header";
import BaseButton from "@components/ui/BaseButton";
import GuardianTabBar from "@components/common/GuardianTabBar";

import { IoPerson, IoLogOut } from "react-icons/io5";

export default function GuardianProfile() {
  const navigate = useNavigate();
  const { user, logout, withdraw } = useUserStore();
  const { addToast } = useToastStore();

  const [newNickname, setNewNickname] = useState("");
  const [withdrawNickname, setWithdrawNickname] = useState("");
  const [nicknameError, setNicknameError] = useState("");
  const [withdrawError, setWithdrawError] = useState("");

  async function handleNicknameChange() {
    if (!newNickname.trim()) {
      setNicknameError("닉네임을 입력해주세요.");
      return;
    }

    // TODO: 백엔드 API 연동 - 중복 확인
    if (newNickname.trim() === "test") {
      setNicknameError("이미 존재하는 닉네임입니다.");
      return;
    }

    addToast({
      type: "success",
      message: "닉네임이 성공적으로 변경되었습니다.",
      duration: 3000,
      position: "top-center",
    });

    setNewNickname("");
    setNicknameError("");
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
        addToast({
          type: "success",
          message: result.message,
          duration: 3000,
          position: "top-center",
        });

        navigate("/login");
      } else {
        addToast({
          type: "error",
          message: result.message,
          duration: 3000,
          position: "top-center",
        });
      }
    } catch {
      addToast({
        type: "error",
        message: "회원 탈퇴 중 오류가 발생했습니다.",
        duration: 3000,
        position: "top-center",
      });
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
    navigate("/login");
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
        {/* 마이페이지 섹션 */}
        <div className="space-y-6">
          {/* 마이페이지 헤더 */}
          <div className="mb-6">
            <h2 className="text-xl font-bold text-gray-800">마이페이지</h2>
          </div>

          {/* 사용자 정보 카드 */}
          <div className="rounded-lg bg-white p-4 shadow-sm">
            <div className="flex items-center space-x-4">
              {/* 사용자 프로필 이미지 */}
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-200">
                <IoPerson className="h-8 w-8 text-gray-600" />
              </div>

              {/* 사용자 정보 텍스트 영역 */}
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-800">
                  {user.nickname} 님
                </h3>
                <p className="text-sm text-gray-600">
                  가입일 : {user.joinDate || "2023년 12월 12일"}
                </p>
                <p className="text-sm text-gray-600">
                  인증 관계 : {user.connectedBif || "연결된 BIF 없음"}
                </p>
              </div>

              {/* 로그아웃 버튼 */}
              <button
                onClick={handleLogout}
                className="flex items-center space-x-1 rounded bg-gray-100 px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-200"
              >
                <IoLogOut className="h-4 w-4" />
                <span>로그아웃</span>
              </button>
            </div>
          </div>

          {/* 회원정보 수정 섹션 */}
          <div className="space-y-6">
            <h3 className="text-lg font-semibold text-gray-800">
              회원정보 수정
            </h3>

            {/* 닉네임 변경 섹션 */}
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

            {/* 회원 탈퇴 섹션 */}
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

      <GuardianTabBar />
    </>
  );
}
