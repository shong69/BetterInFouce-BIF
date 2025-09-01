import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import PageHeader from "@components/common/PageHeader";
import TabBar from "@components/common/TabBar";
import Modal from "@components/ui/Modal";
import SecondaryButton from "@components/ui/SecondaryButton";
import RecordButton from "@components/ui/RecordButton";
import BackButton from "@components/ui/BackButton";
import { useDiaryStore } from "@stores/diaryStore";
import { useToastStore } from "@stores/toastStore";
import { getEmotionContent } from "@utils/emotionUtils";
import { getSttToken, speechRecognitionService } from "@services/diaryService";

export default function DiaryCreate() {
  const navigate = useNavigate();
  const [content, setContent] = useState("");
  const [isRecording, setIsRecording] = useState(false);
  const [interimText, setInterimText] = useState("");
  const { createDiary, selectedEmotion, clearSelectedEmotion } =
    useDiaryStore();
  const { showSuccess, showError } = useToastStore();
  const [showExitModal, setShowExitModal] = useState(false);
  const recognizerRef = useRef(null);

  const handleBack = () => {
    if (content.trim()) {
      setShowExitModal(true);
    } else {
      navigate("/diaries");
    }
  };

  const handleExitConfirm = () => {
    setShowExitModal(false);
    clearSelectedEmotion();
    navigate("/diaries");
  };

  const handleExitCancel = () => {
    setShowExitModal(false);
  };

  const toggleRecording = async () => {
    if (isRecording) {
      handleStopRecording();
    } else {
      await handleStartRecording();
    }
  };

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
          const cleanedText = text.replace(/\.$/, "").trim();
          setContent((prev) => {
            const newText = prev ? `${prev} ${cleanedText}` : cleanedText;
            return newText;
          });
          setInterimText("");
        },
        onCanceled: async (reason, errorDetails) => {
          if (errorDetails && errorDetails.includes("authorization")) {
            try {
              showSuccess("토큰을 갱신하고 있습니다...");
              await handleTokenExpiredAndRestart();
            } catch {
              showError("토큰 갱신에 실패했습니다.");
              setIsRecording(false);
            }
          } else if (errorDetails) {
            showError(`음성 인식 오류: ${errorDetails}`);
            setIsRecording(false);
          } else {
            setIsRecording(false);
          }
        },
      };

      speechRecognitionService.setupRecognizer(recognizer, callbacks);

      speechRecognitionService.startRecognition(
        recognizer,
        () => {
          setIsRecording(true);
          showSuccess("음성 인식을 시작합니다.");
        },
        () => {
          showError("음성 인식 시작에 실패했습니다.");
          setIsRecording(false);
        },
      );
    } catch (error) {
      if (error.name === "NotAllowedError") {
        showError("브라우저 설정에서 마이크 권한을 허용해주세요.");
      } else if (error.name === "NotFoundError") {
        showError(
          "마이크를 찾을 수 없습니다. 마이크가 연결되어 있는지 확인해주세요.",
        );
      } else {
        showError("음성 인식을 시작할 수 없습니다. 다시 시도해주세요.");
      }
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
        () => {
          showError("음성 인식 중단에 실패했습니다.");
          setIsRecording(false);
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

  useEffect(() => {
    return () => {
      if (recognizerRef.current) {
        speechRecognitionService.closeRecognizer(recognizerRef.current);
      }
    };
  }, []);

  const handleSave = async () => {
    if (!content.trim()) {
      showError("일기 내용을 입력해주세요.");
      return;
    }

    try {
      const response = await createDiary({
        emotion: selectedEmotion,
        content,
      });
      showSuccess("일기가 성공적으로 저장되었습니다!");
      clearSelectedEmotion();
      const id = response.id;
      navigate(`/diaries/${id}`);
    } catch (error) {
      if (error.response && error.response.data) {
        showError("일기를 불러오는데 실패했습니다.");
      }
    }
  };
  return (
    <div className="bg-radial-gradient min-h-screen">
      <PageHeader title="감정일기" />
      <div className="mx-auto mb-24 max-w-2xl p-4 sm:p-4">
        <div className="mb-2">
          <BackButton onClick={handleBack} />
        </div>
        <div className="mt-3 mb-1 flex items-center justify-between">
          <div
            className="ml-3 text-sm font-extralight"
            style={{ whiteSpace: "pre-line" }}
          >
            {getEmotionContent(selectedEmotion).text}
          </div>
          <div className="mr-4 flex items-center">
            <div
              className={`mr-2 rounded-full px-3 py-1 text-xs text-[#333333] ${getEmotionContent(selectedEmotion).bgColor}`}
            >
              {getEmotionContent(selectedEmotion).name}
            </div>
            <img
              src={getEmotionContent(selectedEmotion).icon}
              alt="emotion"
              className="h-8 w-8 drop-shadow-md sm:h-10 sm:w-10"
            />
          </div>
        </div>

        <div className="mb-10 px-2 sm:mb-10 sm:px-0">
          <textarea
            id="diary-content"
            value={content + (interimText ? ` ${interimText}` : "")}
            onChange={(e) => {
              setContent(e.target.value);
            }}
            placeholder="오늘 하루는 어땠나요?"
            className="text-medium h-[50vh] w-full resize-none rounded-xl border border-gray-300 p-3 focus:ring-2 focus:ring-blue-500 focus:outline-none sm:h-[60vh] sm:p-4 sm:text-base"
          />
        </div>

        <div className="px-2 sm:px-0">
          <RecordButton isRecording={isRecording} onClick={toggleRecording} />
          <SecondaryButton onClick={handleSave} title={"일기 저장하기"} />
        </div>
      </div>
      <TabBar />

      <Modal
        isOpen={showExitModal}
        onClose={handleExitCancel}
        primaryButtonText="확인"
        secondaryButtonText="취소"
        onPrimaryClick={handleExitConfirm}
        onSecondaryClick={handleExitCancel}
        children={
          <div className="text-center">
            <h2 className="mb-2 text-lg font-semibold">정말 나가시겠습니까?</h2>
            <div className="text-sm">저장하지 않은 일기가 있습니다.</div>
          </div>
        }
      />
    </div>
  );
}
