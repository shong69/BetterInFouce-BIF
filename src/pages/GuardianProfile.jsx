import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

import Header from "@components/common/Header";
import PrimaryButton from "@components/ui/PrimaryButton";

import { IoPerson, IoBook, IoCalendar, IoBarChart } from "react-icons/io5";

const DEFAULT_GUARDIAN_USER = {
  nickname: "보호자",
  joinDate: "2023년 12월 12일",
  connectedBif: "BIF",
};

export default function GuardianProfile() {
  const navigate = useNavigate();

  const [user, setUser] = useState(DEFAULT_GUARDIAN_USER);

  const [newNickname, setNewNickname] = useState("");
  const [withdrawNickname, setWithdrawNickname] = useState("");
  const [nicknameError, setNicknameError] =
    useState("이미 존재하는 닉네임입니다.");
  const [withdrawError, setWithdrawError] = useState(
    "닉네임이 일치하지 않습니다. 다시 입력해주세요.",
  );

  useEffect(() => {
    localStorage.setItem("userType", "GUARDIAN");

    setUser(DEFAULT_GUARDIAN_USER);
  }, []);

  function handleNicknameChange() {
    if (!newNickname.trim()) {
      setNicknameError("닉네임을 입력해주세요.");
      return;
    }

    if (newNickname.trim() === "test") {
      setNicknameError("이미 존재하는 닉네임입니다.");
      return;
    }

    setNewNickname("");
    setNicknameError("");
  }

  function handleWithdraw() {
    if (!withdrawNickname.trim()) {
      setWithdrawError("닉네임을 입력해주세요.");
      return;
    }

    if (withdrawNickname.trim() !== "보호자") {
      setWithdrawError("닉네임이 일치하지 않습니다. 다시 입력해주세요.");
      return;
    }

    setWithdrawNickname("");
    setWithdrawError("");
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
        {/* 마이페이지 섹션 */}
        <div className="space-y-6">
          {/* 마이페이지 헤더 */}
          <div className="mb-6 flex items-center">
            <h2 className="text-xl font-bold">마이페이지</h2>
          </div>

          {/* 사용자 정보 카드 */}
          <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm md:p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
              {/* 사용자 프로필 이미지 */}
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-300">
                <IoPerson className="h-8 w-8 text-gray-600" />
              </div>

              {/* 사용자 정보 텍스트 영역 */}
              <div className="flex-1 text-center sm:text-left">
                <h3 className="text-lg font-semibold">{user.nickname} 님</h3>
                <p className="text-sm text-gray-600">
                  가입일 : {user.joinDate}
                </p>
                <p className="text-sm text-gray-600">
                  인증 관계 : {user.connectedBif}
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

          {/* 회원정보 수정 섹션 */}
          <div className="space-y-6">
            <h3 className="text-lg font-semibold">회원정보 수정</h3>

            {/* 닉네임 변경 섹션 */}
            <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm md:p-6">
              <h4 className="text-md mb-4 font-semibold">닉네임 변경</h4>
              <div className="space-y-3">
                <input
                  type="text"
                  value={newNickname}
                  onChange={(e) => setNewNickname(e.target.value)}
                  placeholder="새로운 닉네임을 입력해주세요."
                  className="w-full rounded-lg border border-gray-300 p-3 focus:ring-2 focus:ring-green-500 focus:outline-none"
                />
                {nicknameError && (
                  <p className="text-sm text-red-500">{nicknameError}</p>
                )}
                <div className="flex justify-end">
                  <PrimaryButton onClick={handleNicknameChange}>
                    변경
                  </PrimaryButton>
                </div>
              </div>
            </div>

            {/* 회원 탈퇴 섹션 */}
            <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm md:p-6">
              <h4 className="text-md mb-4 font-semibold">회원 탈퇴</h4>
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
                  <p className="text-sm text-red-500">{withdrawError}</p>
                )}
                <div className="flex justify-end">
                  <button
                    onClick={handleWithdraw}
                    className="rounded-lg bg-red-500 px-4 py-2 text-white transition-colors hover:bg-red-600"
                  >
                    탈퇴
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 커스텀 탭바 */}
      <div className="fixed right-0 bottom-0 left-0 z-50 border-t border-gray-200 bg-white shadow-lg">
        <div className="flex justify-around py-2">
          {/* 통계보기 (활성화) */}
          <button
            onClick={() => navigate("/guardian-stats")}
            className="flex flex-col items-center text-green-600"
          >
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
    </>
  );
}
