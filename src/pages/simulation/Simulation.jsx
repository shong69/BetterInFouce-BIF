import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { simulationService } from "@services/simulationService";
import { useUserStore } from "@stores/userStore";

import TabBar from "@components/common/TabBar";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import SimulationCard from "@pages/simulation/components/SimulationCard";
import Header from "@components/common/Header";

import managerImage from "@assets/manager.png";
import mamaImage from "@assets/mama.png";
import minaImage from "@assets/mina.png";

export default function Simulation() {
  const [simulations, setSimulations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedCategories, setExpandedCategories] = useState(new Set());
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

  function handleStartSimulation(simulationId) {
    navigate(`/simulation/${simulationId}`);
  }

  async function handleRecommendSimulation(simulationId) {
    try {
      await simulationService.recommendSimulation(simulationId);

      const updatedSimulations = simulations.map((sim) =>
        sim.id === simulationId
          ? { ...sim, isRecommended: !sim.isRecommended }
          : sim,
      );
      setSimulations(updatedSimulations);
    } catch {
      setError("추천 상태 업데이트에 실패했습니다.");
    }
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

      <Header showTodoButton={false} />

      <main className="mx-auto w-full max-w-4xl flex-1 px-4 pt-8 pb-24">
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
