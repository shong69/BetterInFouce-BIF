import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { simulationService } from "../services/simulationService";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import Card from "@components/common/Card";

function Simulation() {
  const [simulations, setSimulations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(function () {
    async function fetchSimulations() {
      try {
        setLoading(true);
        const data = await simulationService.getSimulations();
        setSimulations(data);
      } catch (error) {
        setError("시뮬레이션을 불러오는데 실패했습니다.", error);
      } finally {
        setLoading(false);
      }
    }

    fetchSimulations();
  }, []);

  function handleStartSimulation(simulationId) {
    navigate(`/simulation/${simulationId}`);
  }

  function getCurrentDate() {
    const now = new Date();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
    return `${month}월 ${date}일 ${day}요일`;
  }

  const recommendedSimulations = simulations.filter((sim) => sim.isRecommended);
  const otherSimulations = simulations.filter((sim) => !sim.isRecommended);

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
      </div>

      <main className="w-full max-w-full flex-1 bg-gray-50 px-5 pb-24">
        {recommendedSimulations.length > 0 && (
          <section className="mb-8 w-full">
            <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
              추천
            </h2>
            <div className="w-full space-y-3">
              {recommendedSimulations.map((simulation) => (
                <Card
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

        {otherSimulations.length > 0 && (
          <section className="mb-8 w-full">
            <h2 className="mb-4 text-[15px] font-extrabold text-gray-800">
              주제
            </h2>
            <div className="w-full space-y-3">
              {otherSimulations.map((simulation) => (
                <Card
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

export default Simulation;
