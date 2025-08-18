import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { simulationService } from "@services/simulationService";
import { useUserStore } from "@stores/userStore";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import SimulationCard from "@pages/simulation/components/SimulationCard";

export default function Simulation() {
  const [simulations, setSimulations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const { user } = useUserStore();

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
          const processedSimulations = simulationsData.map((sim) => ({
            ...sim,
            isActive: sim.isActive === null ? false : sim.isActive,
          }));
          setSimulations(processedSimulations);
        } catch (err) {
          if (
            err.message?.includes("Network Error") ||
            err.code === "ERR_NETWORK"
          ) {
            setError(
              "서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.",
            );
          } else if (err.response?.status === 403) {
            setError("접근 권한이 없습니다.");
          } else if (err.response?.status === 401) {
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
    const responseData =
      await simulationService.recommendSimulation(simulationId);

    const newIsActive = responseData?.data?.isActive;

    if (newIsActive === undefined) {
      return;
    }

    setSimulations((prevSimulations) =>
      prevSimulations.map((sim) =>
        sim.id === simulationId ? { ...sim, isActive: newIsActive } : sim,
      ),
    );
  }

  function getCurrentDate() {
    const now = new Date();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
    return `${month}월 ${date}일 ${day}요일`;
  }

  const bifRecommendedSimulations = isBif
    ? simulations.filter((sim) => sim.isActive)
    : [];
  const bifOtherSimulations = isBif
    ? simulations.filter((sim) => !sim.isActive)
    : [];

  const guardianRecommendedSimulations = isGuardian
    ? simulations.filter((sim) => sim.isActive)
    : [];
  const guardianOtherSimulations = isGuardian
    ? simulations.filter((sim) => !sim.isActive)
    : [];

  if (loading) {
    return (
      <>
        <Header />
        <LoadingSpinner />
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
      <Header />

      <div className="flex items-center justify-between bg-white px-5 py-5">
        <div className="text-black-600 text-[13px] font-medium">
          {getCurrentDate()}
        </div>
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
                      showThumbsUpButton={false}
                      onThumbsUp={handleRecommendSimulation}
                      isThumbsUp={simulation.isActive}
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
                      showThumbsUpButton={false}
                      onThumbsUp={handleRecommendSimulation}
                      isThumbsUp={simulation.isActive}
                    />
                  ))}
                </div>
              </section>
            )}
          </>
        )}

        {isGuardian && (
          <>
            {guardianRecommendedSimulations.length > 0 && (
              <section className="mb-8 w-full">
                <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
                  추천
                </h2>
                <div className="w-full space-y-3">
                  {guardianRecommendedSimulations.map((simulation) => (
                    <SimulationCard
                      key={simulation.id}
                      id={simulation.id}
                      title={simulation.title}
                      category={simulation.category}
                      duration={simulation.duration}
                      onClick={handleStartSimulation}
                      showThumbsUpButton={true}
                      onThumbsUp={handleRecommendSimulation}
                      isThumbsUp={simulation.isActive}
                    />
                  ))}
                </div>
              </section>
            )}
            {guardianOtherSimulations.length > 0 && (
              <section className="mb-8 w-full">
                <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
                  주제
                </h2>
                <div className="w-full space-y-3">
                  {guardianOtherSimulations.map((simulation) => (
                    <SimulationCard
                      key={simulation.id}
                      id={simulation.id}
                      title={simulation.title}
                      category={simulation.category}
                      duration={simulation.duration}
                      onClick={handleStartSimulation}
                      showThumbsUpButton={true}
                      onThumbsUp={handleRecommendSimulation}
                      isThumbsUp={simulation.isActive}
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
