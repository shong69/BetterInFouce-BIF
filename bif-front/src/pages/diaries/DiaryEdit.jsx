import { useState, useRef, useEffect } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import Modal from "@components/ui/Modal";
import RecordButton from "@components/ui/RecordButton";
import PrimaryButton from "@components/ui/PrimaryButton";
import { useDiaryStore } from "@stores/diaryStore";
import { useToastStore } from "@stores/toastStore";
import { getEmotionInfo } from "@utils/emotionUtils";
import { formatDate } from "@utils/dateUtils";
import { getSttToken, speechRecognitionService } from "@services/diaryService";
import { HiArrowLeft, HiPencilAlt } from "react-icons/hi";

export default function DiaryEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [content, setContent] = useState("");
  const [interimText, setInterimText] = useState("");
  const [isRecording, setIsRecording] = useState(false);
  const [showExitModal, setShowExitModal] = useState(false);
  const [showSaveModal, setShowSaveModal] = useState(false);
  const [originalContent, setOriginalContent] = useState("");

  const recognizerRef = useRef(null);

  const { updateDiary, fetchDiary } = useDiaryStore();
  const { showSuccess, showError } = useToastStore();

  useEffect(() => {
    const routerData = location.state?.diaryData;

    if (routerData) {
      setContent(routerData.content);
      setOriginalContent(routerData.content);
    } else {
      const loadDiary = async () => {
        try {
          const diary = await fetchDiary(id);
          setContent(diary.content);
          setOriginalContent(diary.content);
        } catch {
          showError("일기를 불러올 수 없습니다. 다시 시도해주세요.");
          navigate(`/diaries/${id}`);
        }
      };
      loadDiary();
    }
  }, [id, location.state, fetchDiary, navigate, showError]);

  const stopRecognizerIfExists = () => {
    if (recognizerRef.current) {
      speechRecognitionService.stopRecognition(recognizerRef.current);
      speechRecognitionService.closeRecognizer(recognizerRef.current);
      recognizerRef.current = null;
      setIsRecording(false);
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

  const handleBackClick = () => {
    const hasUnsavedChanges = content.trim() && content !== originalContent;
    if (hasUnsavedChanges || (interimText && interimText.trim().length > 0)) {
      setShowExitModal(true);
    } else {
      navigate(-1);
    }
  };

  const handleExitConfirm = () => {
    setShowExitModal(false);
    navigate(`/diaries/${id}`);
  };

  const handleExitCancel = () => setShowExitModal(false);

  const handleSave = () => {
    if (!content.trim()) {
      showError("일기 내용을 입력해주세요.");
      return;
    }
    if (content.trim() === originalContent.trim()) {
      showError("수정할 내용이 없습니다.");
      return;
    }
    setShowSaveModal(true);
  };

  const handleSaveConfirm = async () => {
    try {
      await updateDiary(id, { content });
      showSuccess("일기가 성공적으로 수정되었습니다!");
      setShowSaveModal(false);
      navigate(`/diaries/${id}`);
    } catch {
      showError("일기 수정에 실패했습니다.");
    }
  };

  const handleSaveCancel = () => setShowSaveModal(false);

  const diaryData = location.state?.diaryData || {
    content,
    createdAt: new Date(),
    emotion: "NEUTRAL",
  };

  return (
    <div className="min-h-screen">
      <Header showTodoButton={false} onBackClick={handleBackClick} />
      <div className="mx-auto mb-24 max-w-2xl p-4 sm:p-4">
        <div className="flex items-center justify-between">
          <div className="mx-4 mb-2 text-sm font-semibold">
            {formatDate(diaryData.createdAt)}의 일기
          </div>
          <div className="mr-4 flex items-center">
            <div
              className={`mr-2 rounded-full px-3 py-1 text-xs text-[#333333] shadow-xs ${getEmotionInfo(diaryData.emotion).bgColor}`}
            >
              {getEmotionInfo(diaryData.emotion).name}
            </div>
            <img
              src={getEmotionInfo(diaryData.emotion).icon}
              alt="emotion"
              className="mb-1 h-8 w-8 sm:h-10 sm:w-10"
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

        <div className="mb-4 px-2 sm:px-0">
          <RecordButton isRecording={isRecording} onClick={toggleRecording} />
          <PrimaryButton onClick={handleSave} title={"일기 수정하기"} />
        </div>
      </div>
      <TabBar />

      <Modal
        isOpen={showExitModal}
        onClose={handleExitCancel}
        primaryButtonText="나가기"
        secondaryButtonText="취소"
        primaryButtonColor="bg-black"
        onPrimaryClick={handleExitConfirm}
        onSecondaryClick={handleExitCancel}
      >
        <div className="text-center">
          <div className="mb-4 flex justify-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-black">
              <HiArrowLeft className="h-12 w-12 text-white" />
            </div>
          </div>
          <h3 className="mb-2 text-xl font-bold text-black">
            정말 나가시겠습니까?
          </h3>
          <p className="mb-1 text-sm text-black">
            저장하지 않은 수정사항이 있습니다.
          </p>
        </div>
      </Modal>

      <Modal
        isOpen={showSaveModal}
        onClose={handleSaveCancel}
        primaryButtonText="수정"
        secondaryButtonText="취소"
        primaryButtonColor="bg-primary"
        onPrimaryClick={handleSaveConfirm}
        onSecondaryClick={handleSaveCancel}
      >
        <div className="text-center">
          <div className="mb-4 flex justify-center">
            <div className="bg-primary flex h-16 w-16 items-center justify-center rounded-full">
              <HiPencilAlt className="text-white" size={32} />
            </div>
          </div>
          <h3 className="mb-2 text-xl font-bold text-black">
            일기를 수정하시겠습니까?
          </h3>
          <p className="mb-1 text-sm text-black">
            수정된 내용으로 일기가 <br />
            업데이트됩니다.
          </p>
        </div>
      </Modal>
    </div>
  );
}
