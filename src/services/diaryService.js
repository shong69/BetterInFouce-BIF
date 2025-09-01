import api from "./api.js";
import * as SpeechSDK from "microsoft-cognitiveservices-speech-sdk";

export const fetchMonthlyDiaries = async function (year, month) {
  const response = await api.get(
    `/api/diaries/monthly-summary?year=${year}&month=${month}`,
  );
  return response.data;
};

export const fetchDiary = async function (id) {
  const response = await api.get(`/api/diaries/${id}`);
  return response.data;
};

export const createDiary = async function (diaryData) {
  const response = await api.post("/api/diaries", diaryData);
  return response.data;
};

export const updateDiary = async function (id, diaryData) {
  const response = await api.patch(`/api/diaries/${id}`, diaryData);
  return response.data;
};

export const deleteDiary = async function (id) {
  await api.delete(`/api/diaries/${id}`);
  return true;
};

export const getSttToken = async function () {
  const response = await api.post("/api/diaries/stt/token");
  return response.data;
};

export const speechRecognitionService = {
  async createRecognizer(token, region) {
    const speechConfig = SpeechSDK.SpeechConfig.fromAuthorizationToken(
      token,
      region,
    );
    speechConfig.speechRecognitionLanguage = "ko-KR";
    speechConfig.setProfanity(SpeechSDK.ProfanityOption.Raw);
    speechConfig.setProperty(
      SpeechSDK.PropertyId.SpeechServiceConnection_EnableAudioLogging,
      "false",
    );

    const audioConfig = SpeechSDK.AudioConfig.fromDefaultMicrophoneInput();
    return new SpeechSDK.SpeechRecognizer(speechConfig, audioConfig);
  },

  setupRecognizer(recognizer, callbacks) {
    const { onRecognizing, onRecognized, onCanceled } = callbacks;

    if (onRecognizing) {
      recognizer.recognizing = (s, e) => {
        onRecognizing(e.result.text);
      };
    }

    if (onRecognized) {
      recognizer.recognized = (s, e) => {
        if (e.result.reason === SpeechSDK.ResultReason.RecognizedSpeech) {
          onRecognized(e.result.text);
        }
      };
    }

    if (onCanceled) {
      recognizer.canceled = (s, e) => {
        onCanceled(e.reason, e.errorDetails);
      };
    }
  },

  startRecognition(recognizer, onSuccess, onError) {
    recognizer.startContinuousRecognitionAsync(onSuccess, onError);
  },

  stopRecognition(recognizer, onSuccess, onError) {
    recognizer.stopContinuousRecognitionAsync(onSuccess, onError);
  },

  closeRecognizer(recognizer) {
    if (recognizer) {
      recognizer.close();
    }
  },
};
