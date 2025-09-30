import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import Modal from "@components/ui/Modal";
import SecondaryButton from "@components/ui/SecondaryButton";
import RecordButton from "@components/ui/RecordButton";
import { useDiaryStore } from "@stores/diaryStore";
import { useToastStore } from "@stores/toastStore";
import { getEmotionContent } from "@utils/emotionUtils";
import { getSttToken, speechRecognitionService } from "@services/diaryService";
import { HiOutlineTrash } from "react-icons/hi";

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

  const handleBackClick = () => {
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
    } catch {
      showError("일기를 불러오는데 실패했습니다.");
    }
  };

  const stopRecognizerIfExists = () => {
    if (recognizerRef.current) {
      speechRecognitionService.stopRecognition(recognizerRef.current);
      speechRecognitionService.closeRecognizer(recognizerRef.current);
      recognizerRef.current = null;
    }
  };

  const handleStartRecording = async () => {
    try {
      await navigator.mediaDevices.getUserMedia({ audio: true });

      stopRecognizerIfExists();

      const { token, region } = await getSttToken();
      const recognizer = await speechRecognitionService.createRecognizer(
        token,
        region,
      );
      recognizerRef.current = recognizer;

      const callbacks = {
        onRecognizing: (text) => setInterimText(text),
        onRecognized: (text) => {
          const cleanedText = text.replace(/\.$/, "").trim();
          setContent((prev) => (prev ? `${prev} ${cleanedText}` : cleanedText));
          setInterimText("");
        },
        onCanceled: async (reason, errorDetails) => {
          if (errorDetails && errorDetails.includes("authorization")) {
            showSuccess("토큰을 갱신하고 있습니다...");
            await handleTokenExpiredAndRestart();
          } else if (errorDetails) {
            showError(`음성 인식 오류: ${errorDetails}`);
            setIsRecording(false);
          } else {
            setIsRecording(false);
          }
        },
        onError: (error) => {
          showError(error.message || "음성 인식 오류가 발생했습니다.");
          setIsRecording(false);
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
    } catch {
      showError("마이크를 사용할 수 없습니다. 권한과 장치를 확인해주세요.");
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
    stopRecognizerIfExists();
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
    return () => stopRecognizerIfExists();
  }, []);

  return (
    <div className="min-h-screen">
      <Header showTodoButton={false} onBackClick={handleBackClick} />
      <div className="mx-auto mb-24 max-w-2xl p-4 sm:p-4">
        <div className="mb-1 flex items-center justify-between">
          <div
            className="ml-3 text-sm font-extralight"
            style={{ whiteSpace: "pre-line" }}
          >
            {getEmotionContent(selectedEmotion).text}
          </div>
          <div className="mr-4 flex items-center">
            <div
              className={`mr-2 rounded-full px-3 py-1 text-xs text-[#333333] shadow-xs ${getEmotionContent(selectedEmotion).bgColor}`}
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
            onChange={(e) => setContent(e.target.value)}
            placeholder="오늘 하루는 어땠나요?"
            className="text-medium h-[50vh] w-full resize-none rounded-xl border border-gray-300 p-3 focus:ring-2 focus:ring-blue-500 focus:outline-none sm:h-[60vh] sm:p-4 sm:text-base"
          />
        </div>

        <div className="mb-2 px-2 sm:px-0">
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
        primaryButtonColor="bg-[#343434] hover:bg-black"
        onPrimaryClick={handleExitConfirm}
        onSecondaryClick={handleExitCancel}
      >
        <div className="text-center">
          <div className="mb-4 flex justify-center">
            <div className="bg-warning flex h-16 w-16 items-center justify-center rounded-full">
              <HiOutlineTrash className="text-white" size={32} />
            </div>
          </div>
          <h3 className="mb-2 text-xl font-bold text-black">
            정말 나가시겠습니까?
          </h3>
          <p className="mb-1 text-sm text-black">
            작성하던 내용이 저장되지 않았습니다.
          </p>
        </div>
      </Modal>
    </div>
  );
}
