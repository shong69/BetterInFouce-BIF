import { useState, useRef, useEffect } from "react";
import { useToastStore } from "@stores/toastStore";
import { getSttToken, speechRecognitionService } from "@services/diaryService";

export default function useSpeechRecorder(onTextRecognized) {
  const [isRecording, setIsRecording] = useState(false);
  const [interimText, setInterimText] = useState("");
  const recognizerRef = useRef(null);
  const { showSuccess, showError } = useToastStore();

  function getFriendlyErrorMessage(error) {
    if (!error) return "알 수 없는 오류가 발생했습니다. 다시 시도해주세요.";
    const name = error.name || error.code || "";
    if (name === "NotAllowedError" || name === "PermissionDeniedError") {
      return "마이크 권한이 필요합니다. 브라우저 설정에서 권한을 허용해주세요.";
    }
    if (name === "NotFoundError" || name === "DevicesNotFoundError") {
      return "마이크 장치를 찾을 수 없습니다. 연결 상태를 확인해주세요.";
    }
    if (name === "NotReadableError") {
      return "마이크를 사용할 수 없습니다. 다른 앱에서 사용 중인지 확인해주세요.";
    }
    if (name === "OverconstrainedError") {
      return "호환되지 않는 마이크 설정입니다. 기본 입력 장치를 사용해주세요.";
    }
    if (name === "AbortError") {
      return "마이크 접근이 중단되었습니다. 다시 시도해주세요.";
    }
    if (typeof error === "string" && error.includes("authorization")) {
      return "인증이 만료되었습니다. 잠시 후 다시 시도해주세요.";
    }
    return error.message || "오류가 발생했습니다. 다시 시도해주세요.";
  }

  const handleStartRecording = async () => {
    try {
      await navigator.mediaDevices.getUserMedia({ audio: true });

      const { token, region } = await getSttToken();

      const recognizer = await speechRecognitionService.createRecognizer(
        token,
        region,
      );
      recognizerRef.current = recognizer;

      const callbacks = {
        onRecognizing: (text) => {
          setInterimText(text);
        },
        onRecognized: (text) => {
          setInterimText("");
          if (onTextRecognized) {
            onTextRecognized(text);
          }
        },
        onError: (error) => {
          showError(getFriendlyErrorMessage(error));
          setIsRecording(false);
        },
        onTokenExpired: () => {
          handleTokenExpiredAndRestart();
        },
      };

      await speechRecognitionService.startRecognition(
        recognizer,
        callbacks,
        () => {
          setIsRecording(true);
          showSuccess("음성 인식이 시작되었습니다.");
        },
      );
    } catch (error) {
      showError(getFriendlyErrorMessage(error));
    }
  };

  const handleStopRecording = () => {
    if (recognizerRef.current) {
      speechRecognitionService.stopRecognition(
        recognizerRef.current,
        () => {
          speechRecognitionService.closeRecognizer(recognizerRef.current);
          recognizerRef.current = null;
          setIsRecording(false);
          showSuccess("음성 인식이 중단되었습니다.");
        },
        (error) => {
          showError(getFriendlyErrorMessage(error));
        },
      );
    }
  };

  const handleTokenExpiredAndRestart = async () => {
    if (recognizerRef.current) {
      speechRecognitionService.closeRecognizer(recognizerRef.current);
      recognizerRef.current = null;
    }

    await handleStartRecording();
  };

  const toggleRecording = async () => {
    if (isRecording) {
      handleStopRecording();
    } else {
      await handleStartRecording();
    }
  };

  useEffect(() => {
    return () => {
      if (recognizerRef.current) {
        speechRecognitionService.closeRecognizer(recognizerRef.current);
      }
    };
  }, []);

  return {
    isRecording,
    interimText,
    toggleRecording,
  };
}
