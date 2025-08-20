import axios from "axios";

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api"}/api`;

const ttsState = {
  isPlaying: false,
  currentAudio: null,
};
const ttsListeners = new Set();

const ttsManager = {
  isPlaying() {
    return ttsState.isPlaying;
  },

  addListener(callback) {
    ttsListeners.add(callback);
  },

  removeListener(callback) {
    ttsListeners.delete(callback);
  },

  notifyListeners() {
    ttsListeners.forEach((callback) => callback(ttsState.isPlaying));
  },

  stopCurrent() {
    if (ttsState.currentAudio) {
      ttsState.currentAudio.pause();
      ttsState.currentAudio.currentTime = 0;
      ttsState.currentAudio = null;
    }
  },

  async playTTS(audioContent) {
    if (ttsState.isPlaying) {
      return false;
    }

    this.stopCurrent();

    ttsState.isPlaying = true;
    this.notifyListeners();

    try {
      await this.playAudio(audioContent);
      return true;
    } finally {
      ttsState.isPlaying = false;
      ttsState.currentAudio = null;
      this.notifyListeners();
    }
  },

  playAudio(base64Audio) {
    return new Promise((resolve, reject) => {
      try {
        const binaryString = atob(base64Audio);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
          bytes[i] = binaryString.charCodeAt(i);
        }

        const blob = new Blob([bytes], { type: "audio/mp3" });
        const audioUrl = URL.createObjectURL(blob);
        const audio = new Audio(audioUrl);

        ttsState.currentAudio = audio;

        audio.onended = () => {
          URL.revokeObjectURL(audioUrl);
          resolve();
        };

        audio.onerror = () => {
          URL.revokeObjectURL(audioUrl);
          reject(new Error("오디오 재생 실패"));
        };

        audio.play().catch(reject);
      } catch (error) {
        reject(error);
      }
    });
  },
};

export default function mapBackendToFrontend(backendData) {
  if (!Array.isArray(backendData)) {
    return [];
  }

  return backendData.map(function (simulation) {
    const title = simulation?.title || "제목 없음";
    const description = simulation?.description || "설명 없음";
    const id = simulation?.simulation_id || simulation?.id || 0;
    const category = simulation?.category || getCategoryFromTitle(title);
    const isActive = simulation?.isActive || false;

    return {
      id: id,
      title: title,
      description: description,
      category: category,
      isActive: isActive,
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

function removeEmojis(text) {
  const emojiRegex =
    /(\u00a9|\u00ae|[\u2000-\u3300]|\ud83c[\ud000-\udfff]|\ud83d[\ud000-\udfff]|\ud83e[\ud000-\udfff])/g;
  return text.replace(emojiRegex, "");
}

export const simulationService = {
  getSimulations: async function () {
    try {
      const accessToken = sessionStorage.getItem("accessToken");

      const headers = {};
      if (accessToken) {
        headers["Authorization"] = `Bearer ${accessToken}`;
      }

      const response = await axios.get(`${API_BASE_URL}/simulations`, {
        headers,
      });

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
    const response = await axios.get(
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
      throw Error("시뮬레이션 상세 정보를 가져올 수 없습니다.");
    }
  },

  startSimulation: async function (simulationId) {
    const response = await axios.post(
      `${API_BASE_URL}/simulations/${simulationId}/start`,
    );

    return response.data;
  },

  getFeedback: async function (simulationId, score) {
    const response = await axios.get(
      `${API_BASE_URL}/simulations/${simulationId}/feedback`,
      { params: { score } },
    );
    return response.data;
  },

  completeSimulation: async function (sessionId, totalScore) {
    try {
      const requestBody = { totalScore: totalScore };
      if (sessionId) {
        requestBody.sessionId = sessionId;
      }

      const response = await axios.post(
        `${API_BASE_URL}/simulations/complete`,
        requestBody,
      );
      return response.data;
    } catch {
      return { success: false, message: "시뮬레이션 완료 처리 실패" };
    }
  },

  recommendSimulation: async function (simulationId) {
    const accessToken = sessionStorage.getItem("accessToken");
    if (!accessToken) {
      throw Error("인증 토큰이 없습니다. 로그인이 필요합니다.");
    }

    const payload = JSON.parse(atob(accessToken.split(".")[1]));
    const guardianId = payload.sub || payload.guardianId || payload.userId;
    const bifId = payload.bifId || payload.connectedBifId;

    if (!guardianId || !bifId) {
      throw Error("토큰에서 사용자 정보를 찾을 수 없습니다.");
    }

    const requestBody = {
      guardianId: guardianId,
      bifId: Number(bifId),
      simulationId: Number(simulationId),
    };

    const response = await axios.post(
      `${API_BASE_URL}/simulations/recommendations`,
      requestBody,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
      },
    );

    return response.data;
  },

  textToSpeech: async function (text, voiceName = null) {
    if (!text || text.trim() === "") {
      throw Error("텍스트가 필요합니다.");
    }
    const textForTTS = removeEmojis(text);
    const requestBody = { text: textForTTS };

    if (voiceName) {
      requestBody.voiceName = voiceName;
    }

    const response = await axios.post(
      `${API_BASE_URL}/simulations/tts`,
      requestBody,
    );

    if (response.data && response.data.success && response.data.data) {
      return response.data.data.audioContent;
    } else {
      throw Error("TTS 응답 형식 오류");
    }
  },

  playTTS: async function (text, voiceName = null) {
    if (!text || text.trim() === "" || ttsManager.isPlaying()) {
      return false;
    }

    try {
      const audioContent = await this.textToSpeech(text, voiceName);
      return await ttsManager.playTTS(audioContent);
    } catch {
      return false;
    }
  },

  tts: ttsManager,

  getAvailableVoices: function () {
    return [
      {
        id: "ko-KR-Chirp3-HD-Alnilam",
        name: "Alnilam (HD)",
        gender: "female",
        type: "Chirp3-HD",
      },
      {
        id: "ko-KR-Chirp3-HD-Aoede",
        name: "Aoede (HD)",
        gender: "female",
        type: "Chirp3-HD",
      },
    ];
  },
};
