import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { simulationService } from "@services/simulationService";
import { useUserStore } from "@stores/userStore";
import { useToastStore } from "@stores/toastStore";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import SimulationCard from "@pages/simulation/components/SimulationCard";

export default function Simulation() {
  const [simulations, setSimulations] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [linkedBifInfo, setLinkedBifInfo] = useState(null);
  const navigate = useNavigate();

  const { user } = useUserStore();
  const { showSuccess, showError } = useToastStore();

  const userRole = user?.userRole;
  const isBif = userRole === "BIF";
  const isGuardian = userRole === "GUARDIAN";

  useEffect(
    function () {
      async function fetchData() {
        try {
          setLoading(true);
          setError(null);

          const simulationsData = await simulationService.getSimulations();
          setSimulations(simulationsData);

          if (isBif) {
            try {
              const recommendationsData =
                await simulationService.getRecommendations();
              setRecommendations(recommendationsData);
            } catch (recError) {
              throw ("추천 목록 조회 실패:", recError);
            }
          } else if (isGuardian) {
            try {
              const bifInfo = await simulationService.getLinkedBifInfo();
              setLinkedBifInfo(bifInfo);
            } catch (bifError) {
              throw ("연동된 BIF 정보 조회 실패:", bifError);
            }
          }
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
    [isBif, isGuardian],
  );

  function handleStartSimulation(simulationId) {
    navigate(`/simulation/${simulationId}`);
  }

  async function handleRecommendSimulation(simulationId) {
    if (!isGuardian || !linkedBifInfo?.bifId) {
      showError("연동된 BIF 정보가 없습니다.");
      return;
    }

    try {
      await simulationService.recommendSimulation(
        linkedBifInfo.bifId,
        simulationId,
      );
      showSuccess("시뮬레이션을 추천했습니다.");

      const updatedSimulations = simulations.map((sim) =>
        sim.id === simulationId ? { ...sim, isRecommended: true } : sim,
      );
      setSimulations(updatedSimulations);
    } catch {
      showError("추천에 실패했습니다. 다시 시도해주세요.");
    }
  }

  async function handleThumbsUpSimulation(simulationId) {
    const updatedSimulations = simulations.map((sim) =>
      sim.id === simulationId ? { ...sim, isThumbsUp: !sim.isThumbsUp } : sim,
    );
    setSimulations(updatedSimulations);
  }

  function getCurrentDate() {
    const now = new Date();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
    return `${month}월 ${date}일 ${day}요일`;
  }

  const bifRecommendedSimulations = isBif ? recommendations : [];
  const bifOtherSimulations = isBif
    ? simulations.filter((sim) => !sim.isRecommended)
    : [];

  const guardianSimulations = isGuardian ? simulations : [];

  if (loading) {
    return (
      <>
        <LoadingSpinner />
        <Header />
        <TabBar />
      </>
    );
  }

  if (error) {
    return (
      <>
        <Header />
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
      <Header />

      <div className="flex items-center justify-between bg-white px-5 py-5">
        <div className="text-black-600 text-[13px] font-medium">
          {getCurrentDate()}
        </div>
        {isGuardian && (
          <button
            onClick={function () {
              navigate("/guardian/simulation");
            }}
            className="text-xs text-gray-500 transition-colors hover:text-gray-700"
            title="시뮬레이션 관리"
          >
            관리
          </button>
        )}
      </div>

      <main className="w-full max-w-full flex-1 bg-gray-50 px-5 pb-24">
        {isBif && (
          <>
            {bifRecommendedSimulations.length > 0 && (
              <section className="mb-8 w-full">
                <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
                  추천
                </h2>
                <div className="w-full space-y-3">
                  {bifRecommendedSimulations.map((simulation) => (
                    <SimulationCard
                      key={simulation.id}
                      id={simulation.id}
                      title={simulation.title}
                      category={simulation.category}
                      duration={simulation.duration}
                      onClick={handleStartSimulation}
                      showThumbsUpButton={true}
                      onThumbsUp={handleThumbsUpSimulation}
                      isThumbsUp={simulation.isThumbsUp}
                    />
                  ))}
                </div>
              </section>
            )}

            {bifOtherSimulations.length > 0 && (
              <section className="mb-8 w-full">
                <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
                  주제
                </h2>
                <div className="w-full space-y-3">
                  {bifOtherSimulations.map((simulation) => (
                    <SimulationCard
                      key={simulation.id}
                      id={simulation.id}
                      title={simulation.title}
                      category={simulation.category}
                      duration={simulation.duration}
                      onClick={handleStartSimulation}
                      showThumbsUpButton={true}
                      onThumbsUp={handleThumbsUpSimulation}
                      isThumbsUp={simulation.isThumbsUp}
                    />
                  ))}
                </div>
              </section>
            )}
          </>
        )}

        {isGuardian && (
          <>
            {linkedBifInfo && (
              <div className="mb-4 rounded-lg bg-blue-50 p-3">
                <p className="text-sm text-blue-800">
                  연동된 BIF: {linkedBifInfo.nickname || linkedBifInfo.bifId}
                </p>
              </div>
            )}

            {guardianSimulations.length > 0 && (
              <section className="mb-8 w-full">
                <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
                  시뮬레이션 목록
                </h2>
                <div className="w-full space-y-3">
                  {guardianSimulations.map((simulation) => (
                    <SimulationCard
                      key={simulation.id}
                      id={simulation.id}
                      title={simulation.title}
                      category={simulation.category}
                      duration={simulation.duration}
                      onClick={handleStartSimulation}
                      showRecommendButton={true}
                      onRecommend={handleRecommendSimulation}
                      isRecommended={simulation.isRecommended}
                      showThumbsUpButton={true}
                      onThumbsUp={handleThumbsUpSimulation}
                      isThumbsUp={simulation.isThumbsUp}
                    />
                  ))}
                </div>
              </section>
            )}
          </>
        )}

        {!isBif && !isGuardian && (
          <>
            {simulations.length > 0 && (
              <section className="mb-8 w-full">
                <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
                  시뮬레이션
                </h2>
                <div className="w-full space-y-3">
                  {simulations.map((simulation) => (
                    <SimulationCard
                      key={simulation.id}
                      id={simulation.id}
                      title={simulation.title}
                      category={simulation.category}
                      duration={simulation.duration}
                      onClick={handleStartSimulation}
                    />
                  ))}
                </div>
              </section>
            )}
          </>
        )}

        {simulations.length === 0 && (
          <div className="py-12 text-center">
            <p className="text-gray-500">시뮬레이션이 없습니다.</p>
          </div>
        )}
      </main>
      <TabBar />
    </>
  );
}
