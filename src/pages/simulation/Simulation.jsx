import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { simulationService } from "@services/simulationService";
import { useUserStore } from "@stores/userStore";

import TabBar from "@components/common/TabBar";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import SimulationCard from "@pages/simulation/components/SimulationCard";

import managerImage from "@assets/manager.png";
import mamaImage from "@assets/mama.png";
import minaImage from "@assets/mina.png";

export default function Simulation() {
  const [simulations, setSimulations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedCategories, setExpandedCategories] = useState(new Set());
  const [currentTime, setCurrentTime] = useState("");
  const navigate = useNavigate();
  const { user } = useUserStore();

  const userRole = user?.userRole;

  const isGuardian = userRole === "GUARDIAN";

  useEffect(
    function () {
      async function fetchData() {
        try {
          setLoading(true);
          setError(null);

          const simulationsData = await simulationService.getSimulations();
          setSimulations(simulationsData);
        } catch (error) {
          if (
            error.message?.includes("Network Error") ||
            error.code === "ERR_NETWORK"
          ) {
            setError(
              "서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.",
            );
          } else if (error.response?.status === 403) {
            setError("접근 권한이 없습니다.");
          } else if (error.response?.status === 401) {
            setError("로그인이 필요합니다.");
          } else {
            setError("시뮬레이션을 불러오는데 실패했습니다.");
          }
        } finally {
          setLoading(false);
        }
      }

      fetchData();
    },
    [user],
  );

  useEffect(() => {
    function updateTime() {
      const now = new Date();
      const hours = String(now.getHours()).padStart(2, "0");
      const minutes = String(now.getMinutes()).padStart(2, "0");
      setCurrentTime(`${hours}:${minutes}`);
    }

    updateTime();
    const intervalId = setInterval(updateTime, 60 * 1000);

    return () => clearInterval(intervalId);
  }, []);

  function handleStartSimulation(simulationId) {
    navigate(`/simulation/${simulationId}`);
  }

  async function handleRecommendSimulation(simulationId) {
    const updatedSimulations = simulations.map((sim) =>
      sim.id === simulationId
        ? { ...sim, isRecommended: !sim.isRecommended }
        : sim,
    );
    setSimulations(updatedSimulations);
  }

  function toggleCategory(category) {
    setExpandedCategories((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(category)) {
        newSet.delete(category);
      } else {
        newSet.add(category);
      }
      return newSet;
    });
  }

  function getCurrentDate() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
    return { year, month, date, day };
  }

  function calculateOverallProgress() {
    const totalSimulations = simulations.length;
    if (totalSimulations === 0) return 0;

    const completedCount = simulations.filter((sim) => {
      const completedKey = `sim_${sim.id}_completed`;
      return localStorage.getItem(completedKey) === "true";
    }).length;

    return Math.round((completedCount / totalSimulations) * 100);
  }

  const categoryGroups = {
    업무: {
      simulations: simulations.filter((sim) => sim.category === "업무"),
      description: "업무 환경에서 필요한 대화를 연습합니다.",
      color: "text-[#EF4444]",
      bgColor: "bg-[#FEE2E2]",
      characterImage: managerImage,
    },
    사회: {
      simulations: simulations.filter((sim) => sim.category === "사회"),
      description: "사회생활에서 필요한 대화를 나눕니다.",
      color: "text-[#0B70F5]",
      bgColor: "bg-[#C2DCFF]",
      characterImage: mamaImage,
    },
    일상: {
      simulations: simulations.filter((sim) => sim.category === "일상"),
      description: "일상 속 다양한 상황들을 연습합니다.",
      color: "text-[#F59E0B]",
      bgColor: "bg-[#FEF3C7]",
      characterImage: minaImage,
    },
  };

  if (loading) {
    return (
      <>
        <LoadingSpinner />
        <TabBar />
      </>
    );
  }

  if (error) {
    return (
      <>
        <div className="w-full max-w-full flex-1 bg-gray-50 px-5 pb-24">
          <div className="py-8 text-center">
            <div className="mb-2 text-lg text-red-500">오류가 발생했습니다</div>
            <div className="text-gray-600">{error}</div>
          </div>
        </div>
        <TabBar />
      </>
    );
  }

  return (
    <>
      <LoadingSpinner />

      <div className="flex items-center justify-between px-5 py-3">
        <div className="text-lg font-bold text-gray-800">{currentTime}</div>
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-300">
            <svg
              className="h-5 w-5 text-gray-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 17h5l-1-1v-3.6a3 3 0 0 0-3-3H6a3 3 0 0 0-3 3v3.6l-1 1H2a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1h18a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1h-1z"
              />
            </svg>
          </div>
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-300">
            <svg
              className="h-5 w-5 text-gray-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
              />
            </svg>
          </div>
        </div>
      </div>

      <main className="w-full max-w-full flex-1 bg-[radial-gradient(ellipse_at_top,rgba(234,252,95,0.6),rgba(251,255,218,0.7),rgba(247,248,242,0.8))] px-5 pt-8 pb-24">
        <div className="mb-8 flex items-center justify-between">
          <div className="flex flex-col">
            <div className="mb-1 text-[16px] font-bold text-black">
              시뮬레이션
            </div>
            <div className="text-[13px] font-medium text-black">
              {getCurrentDate().year}년 {getCurrentDate().month}월{" "}
              {getCurrentDate().date}일
            </div>
            <div className="text-[13px] font-medium text-black">
              {getCurrentDate().day}요일
            </div>
          </div>
          <div className="flex flex-col items-end">
            <div className="mb-1 text-sm text-gray-600">전체 진행도</div>
            <div className="text-primary text-lg font-bold">
              {calculateOverallProgress()}%
            </div>
          </div>
        </div>

        <div className="w-full space-y-6">
          {Object.entries(categoryGroups).map(([category, group]) => {
            if (group.simulations.length === 0) return null;

            const isExpanded = expandedCategories.has(category);

            return (
              <div
                key={category}
                className="w-full rounded-xl border border-gray-300 bg-white/80 p-4 shadow-sm backdrop-blur-sm"
              >
                <button
                  className="flex w-full cursor-pointer items-center justify-between"
                  onClick={() => toggleCategory(category)}
                  type="button"
                >
                  <div className="flex items-center gap-5">
                    <div className="flex-shrink-0">
                      <img
                        src={group.characterImage}
                        alt={`${category} 캐릭터`}
                        className="h-10 w-10 object-cover object-top"
                      />
                    </div>
                    <div className="text-left">
                      <h3 className={`text-xl font-bold ${group.color}`}>
                        {category}
                      </h3>
                      <p className="mt-2 text-sm text-black">
                        {group.description}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center">
                    <svg
                      className={`h-6 w-6 text-gray-400 transition-transform ${
                        isExpanded ? "rotate-180" : ""
                      }`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 9l-7 7-7-7"
                      />
                    </svg>
                  </div>
                </button>

                {isExpanded && (
                  <div className="mt-6 space-y-3 border-t border-gray-100 pt-6">
                    {group.simulations
                      .sort((a, b) => {
                        if (a.isRecommended && !b.isRecommended) return -1;
                        if (!a.isRecommended && b.isRecommended) return 1;
                        return 0;
                      })
                      .map((simulation) => (
                        <SimulationCard
                          key={simulation.id}
                          id={simulation.id}
                          title={simulation.title}
                          category={simulation.category}
                          duration={simulation.duration}
                          onClick={handleStartSimulation}
                          showThumbsUpButton={isGuardian}
                          onThumbsUp={handleRecommendSimulation}
                          isThumbsUp={simulation.isRecommended}
                        />
                      ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {simulations.length === 0 && (
          <div className="py-12 text-center">
            <p className="text-gray-500">
              시뮬레이션을 불러오는데 실패했습니다.
            </p>
          </div>
        )}
      </main>
      <TabBar />
    </>
  );
}
