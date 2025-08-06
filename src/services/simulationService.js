import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";
export default function mapBackendToFrontend(backendData) {
  if (!Array.isArray(backendData)) {
    console.error("백엔드 데이터가 배열이 아닙니다:", backendData);
    return [];
  }

  return backendData.map(function (simulation) {
    const title = simulation?.title || "제목 없음";
    const description = simulation?.description || "설명 없음";
    const id = simulation?.simulation_id || simulation?.id || 0;
    const category = simulation?.category || getCategoryFromTitle(title);
    const isRecommended =
      simulation?.is_recommended || simulation?.isRecommended || false;

    return {
      id: id,
      title: title,
      description: description,
      category: category,
      duration: getDurationByCategory(category),
      difficulty: getDifficultyByCategory(category),
      isRecommended: isRecommended,
      createdAt: simulation?.created_at || new Date().toISOString(),
      updatedAt: simulation?.updated_at || new Date().toISOString(),
    };
  });
}

function getCategoryFromTitle(title) {
  if (!title || typeof title !== "string" || title.trim() === "") {
    return "일상";
  }

  const lowerTitle = title.toLowerCase();

  if (
    lowerTitle.includes("직장") ||
    lowerTitle.includes("업무") ||
    lowerTitle.includes("동료")
  ) {
    return "업무";
  } else if (
    lowerTitle.includes("가게") ||
    lowerTitle.includes("계산") ||
    lowerTitle.includes("음식") ||
    lowerTitle.includes("병원")
  ) {
    return "사회";
  } else {
    return "일상";
  }
}

function getDurationByCategory(category) {
  const durationMap = {
    업무: 12,
    사회: 10,
    일상: 8,
    의료: 15,
  };
  return durationMap[category] || 10;
}

function getDifficultyByCategory(category) {
  const difficultyMap = {
    업무: "고급",
    사회: "중급",
    일상: "초급",
    의료: "중급",
  };
  return difficultyMap[category] || "중급";
}

async function getSimulationDetails(simulationId) {
  try {
    const response = await axios.get(
      `${API_BASE_URL}/simulations/${simulationId}/details`,
    );
    if (response.data && response.data.success) {
      const simulationData = response.data.data;
      console.log("백엔드에서 받은 시뮬레이션 데이터:", simulationData);
      console.log("백엔드 카테고리 필드:", simulationData.category);
      const simulation = {
        id: simulationData.simulationId || simulationData.simulation_id || 0,
        title:
          simulationData.simulationTitle ||
          simulationData.title ||
          "시뮬레이션",
        description: simulationData.description || "설명이 없습니다.",
        category:
          simulationData.category ||
          getCategoryFromTitle(
            simulationData.simulationTitle || simulationData.title || "",
          ),
        duration: getDurationByCategory(
          simulationData.category ||
            getCategoryFromTitle(
              simulationData.simulationTitle || simulationData.title || "",
            ),
        ),
        difficulty: getDifficultyByCategory(
          simulationData.category ||
            getCategoryFromTitle(
              simulationData.simulationTitle || simulationData.title || "",
            ),
        ),
        steps: simulationData.steps || [],
        completion: simulationData.completion || {
          excellent: "완벽한 대화였습니다!",
          good: "잘하셨어요!",
          poor: "조금 더 연습해보세요!",
        },
      };

      return simulation;
    } else {
      console.error("시뮬레이션 상세 정보 조회 실패:", response.data);
      throw new Error("시뮬레이션 상세 정보를 가져올 수 없습니다.");
    }
  } catch (error) {
    console.log("백엔드 API 연결 실패:", error.message);
    throw error;
  }
}

export const simulationService = {
  getSimulations: async function () {
    try {
      const response = await axios.get(`${API_BASE_URL}/simulations`);
      console.log("백엔드 응답 데이터:", response.data);
      if (response.data && response.data.success && response.data.data) {
        const backendData = response.data.data;
        console.log("백엔드 데이터:", backendData);
        return mapBackendToFrontend(backendData);
      } else if (Array.isArray(response.data)) {
        console.log("직접 배열 응답:", response.data);
        return mapBackendToFrontend(response.data);
      } else {
        console.error("예상치 못한 백엔드 응답 구조:", response.data);
        return [];
      }
    } catch (error) {
      console.log("백엔드 API 연결 실패:", error.message);
      return [];
    }
  },

  getSimulationDetails: getSimulationDetails,

  startSimulation: async function (simulationId) {
    try {
      const response = await axios.post(
        `${API_BASE_URL}/simulations/${simulationId}/start`,
      );
      // 백엔드에서 생성한 세션 ID 반환
      return response.data;
    } catch (error) {
      console.log("백엔드 API 연결 실패:", error.message);
      throw error;
    }
  },

  // 점수 업데이트
  updateScore: async function (sessionId, stepScore) {
    try {
      const response = await axios.post(
        `${API_BASE_URL}/sessions/${sessionId}/score`,
        { score: stepScore },
      );
      return response.data;
    } catch (error) {
      console.log("점수 업데이트 실패:", error.message);
      throw error;
    }
  },

  // 시뮬레이션 완료
  completeSimulation: async function (sessionId, totalScore) {
    try {
      const response = await axios.post(
        `${API_BASE_URL}/sessions/${sessionId}/complete`,
        { totalScore: totalScore },
      );
      return response.data;
    } catch (error) {
      console.log("시뮬레이션 완료 처리 실패:", error.message);
      throw error;
    }
  },
};
