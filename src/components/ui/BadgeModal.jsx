import { useState, useEffect } from "react";
import Modal from "./Modal";
import { IoClose, IoCheckmarkCircle, IoLockClosed } from "react-icons/io5";

const BADGE_DATA = [
  {
    id: 1,
    name: "첫 걸음",
    description: "첫 번째 일기 작성",
    icon: "🌱",
    requirement: 1,
    color: "from-green-400 to-green-600",
  },
  {
    id: 2,
    name: "일기 초보",
    description: "5개의 일기 작성",
    icon: "📝",
    requirement: 5,
    color: "from-blue-400 to-blue-600",
  },
  {
    id: 3,
    name: "꾸준한 기록자",
    description: "20개의 일기 작성",
    icon: "📚",
    requirement: 10,
    color: "from-purple-400 to-purple-600",
  },
  {
    id: 4,
    name: "감정 탐험가",
    description: "50개의 일기 작성",
    icon: "🎭",
    requirement: 20,
    color: "from-pink-400 to-pink-600",
  },
  {
    id: 5,
    name: "마음의 기록가",
    description: "100개의 일기 작성",
    icon: "💝",
    requirement: 50,
    color: "from-red-400 to-red-600",
  },
  {
    id: 6,
    name: "감정 마스터",
    description: "200개의 일기 작성",
    icon: "👑",
    requirement: 100,
    color: "from-yellow-400 to-yellow-600",
  },
];

export default function BadgeModal({ isOpen, onClose, totalDiaryCount }) {
  const [_earnedBadges, setEarnedBadges] = useState([]);

  useEffect(() => {
    if (totalDiaryCount) {
      const earned = BADGE_DATA.filter(
        (badge) => totalDiaryCount >= badge.requirement,
      );
      setEarnedBadges(earned);
    }
  }, [totalDiaryCount]);

  const getCurrentBadge = () => {
    if (!totalDiaryCount) return null;

    const currentBadge = BADGE_DATA.filter(
      (badge) => totalDiaryCount >= badge.requirement,
    ).sort((a, b) => b.requirement - a.requirement)[0];

    return currentBadge;
  };

  const getNextBadge = () => {
    if (!totalDiaryCount) return BADGE_DATA[0];

    const nextBadge = BADGE_DATA.find(
      (badge) => totalDiaryCount < badge.requirement,
    );
    return nextBadge || null;
  };

  const currentBadge = getCurrentBadge();
  const nextBadge = getNextBadge();

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="mx-auto w-full max-w-xs sm:max-w-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-bold text-gray-800 sm:text-xl">
            나의 뱃지
          </h2>
          <button
            onClick={onClose}
            className="rounded-full p-2 text-gray-500 hover:bg-gray-100"
          >
            <IoClose className="h-5 w-5" />
          </button>
        </div>

        {currentBadge && (
          <div className="mb-4 rounded-lg bg-gradient-to-r from-green-50 to-blue-50 p-3 sm:mb-6 sm:p-4">
            <div className="flex items-center space-x-3">
              <div className="text-xl sm:text-3xl">{currentBadge.icon}</div>
              <div className="min-w-0 flex-1">
                <h3 className="text-sm font-semibold text-gray-800 sm:text-lg">
                  현재 뱃지: {currentBadge.name}
                </h3>
                <p className="text-xs text-gray-600 sm:text-sm">
                  {currentBadge.description}
                </p>
                <p className="text-xs text-gray-500">
                  총 {totalDiaryCount}개의 일기 작성
                </p>
              </div>
            </div>
          </div>
        )}

        {nextBadge && (
          <div className="mb-4 rounded-lg bg-gray-50 p-3 sm:mb-6 sm:p-4">
            <div className="flex items-center space-x-3">
              <div className="text-xl opacity-50 sm:text-3xl">
                {nextBadge.icon}
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-sm font-semibold text-gray-800 sm:text-lg">
                  다음 목표: {nextBadge.name}
                </h3>
                <p className="text-xs text-gray-600 sm:text-sm">
                  {nextBadge.description}
                </p>
                <div className="mt-2">
                  <div className="flex justify-between text-xs text-gray-500">
                    <span>{totalDiaryCount}개</span>
                    <span>{nextBadge.requirement}개</span>
                  </div>
                  <div className="mt-1 h-2 w-full rounded-full bg-gray-200">
                    <div
                      className="h-2 rounded-full bg-gradient-to-r from-blue-400 to-blue-600"
                      style={{
                        width: `${Math.min((totalDiaryCount / nextBadge.requirement) * 100, 100)}%`,
                      }}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 sm:gap-4">
          {BADGE_DATA.map((badge) => {
            const isEarned = totalDiaryCount >= badge.requirement;
            return (
              <div
                key={badge.id}
                className={`relative rounded-lg border-2 p-2 transition-all sm:p-4 ${
                  isEarned
                    ? "border-green-200 bg-gradient-to-br from-green-50 to-green-100"
                    : "border-gray-200 bg-gray-50"
                }`}
              >
                {isEarned && (
                  <div className="absolute -top-1 -right-1 rounded-full bg-green-500 p-1 sm:-top-2 sm:-right-2">
                    <IoCheckmarkCircle className="h-3 w-3 text-white sm:h-4 sm:w-4" />
                  </div>
                )}

                <div className="text-center">
                  <div
                    className={`text-2xl sm:text-4xl ${!isEarned ? "opacity-50 grayscale" : ""}`}
                  >
                    {badge.icon}
                  </div>
                  <h3
                    className={`mt-1 text-xs font-semibold sm:mt-2 sm:text-sm ${
                      isEarned ? "text-gray-800" : "text-gray-500"
                    }`}
                  >
                    {badge.name}
                  </h3>
                  <p
                    className={`text-xs ${
                      isEarned ? "text-gray-600" : "text-gray-400"
                    }`}
                  >
                    {badge.description}
                  </p>
                  {!isEarned && (
                    <div className="mt-1 flex items-center justify-center space-x-1 text-xs text-gray-400 sm:mt-2">
                      <IoLockClosed className="h-3 w-3" />
                      <span>{badge.requirement}개 필요</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </Modal>
  );
}
