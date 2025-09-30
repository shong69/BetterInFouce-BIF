import api from "./api";

const API_BASE_URL = "/api";
export default function mapBackendToFrontend(backendData) {
  if (!Array.isArray(backendData)) {
    return [];
  }

  return backendData.map(function (simulation) {
    const title = simulation?.title || "제목 없음";
    const description = simulation?.description || "설명 없음";
    const id = simulation?.simulation_id || simulation?.id || 0;
    const category = simulation?.category || getCategoryFromTitle(title);
    const isRecommended =
      simulation?.isRecommended || simulation?.isActive || false;

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

export const simulationService = {
  getSimulations: async function () {
    try {
      const response = await api.get(`${API_BASE_URL}/simulations`);
      if (response.data && response.data.success && response.data.data) {
        const backendData = response.data.data;
        return mapBackendToFrontend(backendData);
      } else if (Array.isArray(response.data)) {
        return mapBackendToFrontend(response.data);
      } else {
        return [];
      }
    } catch {
      return [];
    }
  },

  getSimulationDetails: async function (simulationId) {
    const response = await api.get(
      `${API_BASE_URL}/simulations/${simulationId}/details`,
    );
    if (response.data && response.data.success) {
      const simulationData = response.data.data;
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
        sessionId: response.data.sessionId,
      };

      return simulation;
    } else {
      throw new Error("시뮬레이션 상세 정보를 가져올 수 없습니다.");
    }
  },

  startSimulation: async function (simulationId) {
    const response = await api.post(
      `${API_BASE_URL}/simulations/${simulationId}/start`,
    );

    return response.data;
  },

  getFeedback: async function (simulationId, score) {
    const response = await api.get(
      `${API_BASE_URL}/simulations/${simulationId}/feedback`,
      { params: { score } },
    );
    return response.data;
  },
  submitChoice: async function (sessionId, choice, choiceId = null) {
    const requestBody = { sessionId: sessionId, choice: choice };
    if (choiceId) {
      requestBody.choiceId = choiceId;
    }

    const response = await api.post(
      `${API_BASE_URL}/simulations/choice`,
      requestBody,
    );
    return response.data;
  },

  completeSimulation: async function (sessionId, totalScore) {
    try {
      const requestBody = { totalScore: totalScore };
      if (sessionId) {
        requestBody.sessionId = sessionId;
      }

      const response = await api.post(
        `${API_BASE_URL}/simulations/complete`,
        requestBody,
      );
      return response.data;
    } catch {
      return { success: false, message: "시뮬레이션 완료 처리 실패" };
    }
  },

  recommendSimulation: async function (simulationId) {
    const response = await api.post(
      `${API_BASE_URL}/simulations/recommendations`,
      {
        simulationId: simulationId,
      },
    );

    if (response.data && response.data.success !== false) {
      return response.data;
    } else {
      throw new Error("추천 업데이트 응답이 올바르지 않습니다.");
    }
  },

  tts: {
    _isPlaying: false,
    currentAudio: null,

    isPlaying: function () {
      return this._isPlaying;
    },

    playTTS: async function (message, voice = "ko-KR-Chirp3-HD-Aoede") {
      try {
        if (this.currentAudio) {
          this.currentAudio.pause();
          this.currentAudio = null;
        }

        this._isPlaying = true;

        const response = await api.post(`${API_BASE_URL}/simulations/tts`, {
          text: message,
          voiceName: voice,
        });

        if (response.data && response.data.success && response.data.data) {
          const audioContent =
            response.data.data.audioContent || response.data.data.audio;

          if (audioContent) {
            const audioBlob = this.base64ToBlob(audioContent, "audio/mp3");
            const audioUrl = URL.createObjectURL(audioBlob);

            this.currentAudio = new Audio(audioUrl);

            this.currentAudio.onloadstart = () => {
              this._isPlaying = true;
            };

            this.currentAudio.onended = () => {
              this._isPlaying = false;
              URL.revokeObjectURL(audioUrl);
            };

            this.currentAudio.onerror = () => {
              this._isPlaying = false;
              URL.revokeObjectURL(audioUrl);
            };

            await this.currentAudio.play();

            this._isPlaying = true;

            return true;
          } else {
            throw new Error("TTS API 응답에 오디오 데이터가 없습니다.");
          }
        } else {
          throw new Error("TTS API 응답 형식이 올바르지 않습니다.");
        }
      } catch {
        this._isPlaying = false;
        return false;
      }
    },

    stopTTS: function () {
      if (this.currentAudio) {
        this.currentAudio.pause();
        this.currentAudio = null;
        this._isPlaying = false;
      }
    },

    base64ToBlob: function (base64, mimeType) {
      const byteCharacters = atob(base64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      return new Blob([byteArray], { type: mimeType });
    },
  },
};
