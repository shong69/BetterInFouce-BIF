import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import PageHeader from "@components/common/PageHeader";
import TabBar from "@components/common/TabBar";
import Modal2 from "@components/ui/Modal2";
import RecordButton from "@components/ui/RecordButton";
import PrimaryButton from "@components/ui/PrimaryButton";
import BackButton from "@components/ui/BackButton";
import DateBox from "@components/ui/DateBox";
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
  const [isRecording, setIsRecording] = useState(false);
  const [interimText, setInterimText] = useState("");
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
  }, [id, location.state, fetchDiary, showError, navigate]);

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
    function handleBeforeUnload(event) {
      if (content.trim() && content !== originalContent) {
        event.preventDefault();
        event.returnValue =
          "수정 중인 일기가 있습니다. 나가시면 저장되지 않습니다.";
        return event.returnValue;
      }
    }

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [content, originalContent]);

  useEffect(() => {
    return () => {
      if (recognizerRef.current) {
        speechRecognitionService.closeRecognizer(recognizerRef.current);
      }
    };
  }, []);

  const handleBack = () => {
    if (content.trim() && content !== originalContent) {
      setShowExitModal(true);
    } else {
      navigate(`/diaries/${id}`);
    }
  };

  const handleExitConfirm = () => {
    setShowExitModal(false);
    navigate(`/diaries/${id}`);
  };

  const handleExitCancel = () => {
    setShowExitModal(false);
  };

  const handleSave = async () => {
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
    } catch (error) {
      if (error.response && error.response.data) {
        showError("일기 수정에 실패했습니다.");
      }
    }
  };

  const handleSaveCancel = () => {
    setShowSaveModal(false);
  };

  if (!location.state?.diaryData && !content) {
    return (
      <>
        <PageHeader title="감정일기" />
        <div className="mx-auto max-w-2xl p-4 sm:p-4">
          <DateBox />
          <div className="mb-2">
            <BackButton onClick={handleBack} />
          </div>
        </div>
        <TabBar />
      </>
    );
  }

  const diaryData = location.state?.diaryData || {
    content,
    createdAt: new Date(),
    emotion: "NEUTRAL",
  };

  return (
    <div className="bg-radial-gradient min-h-screen">
      <PageHeader title="감정일기 수정" />
      <div className="mx-auto mb-24 max-w-2xl p-4 sm:p-4">
        <div className="mb-2">
          <BackButton onClick={handleBack} />
        </div>
        <div className="flex items-center justify-between">
          <div className="mx-4 mb-2 text-sm font-semibold">
            {formatDate(diaryData.createdAt)}의 일기
          </div>
          <div className="mr-4 flex items-center">
            <div
              className={`mr-2 rounded-full px-3 py-1 text-xs text-[#333333] ${getEmotionInfo(diaryData.emotion).bgColor}`}
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
            onChange={(e) => {
              setContent(e.target.value);
            }}
            placeholder="오늘 하루는 어땠나요?"
            className="text-medium h-[50vh] w-full resize-none rounded-xl border border-gray-300 p-3 focus:ring-2 focus:ring-blue-500 focus:outline-none sm:h-[60vh] sm:p-4 sm:text-base"
          />
        </div>

        <div className="px-2 sm:px-0">
          <RecordButton isRecording={isRecording} onClick={toggleRecording} />
          <PrimaryButton onClick={handleSave} title={"일기 수정하기"} />
        </div>
      </div>
      <TabBar />

      <Modal2
        isOpen={showExitModal}
        onClose={handleExitCancel}
        icon={<HiArrowLeft className="h-12 w-12 text-white" />}
        iconBgColor="bg-[#343434]"
        primaryButtonText="나가기"
        secondaryButtonText="취소"
        primaryButtonColor="bg-[#CACACA]"
        secondaryButtonColor="bg-[#343434]"
        onPrimaryClick={handleExitConfirm}
        onSecondaryClick={handleExitCancel}
        children={
          <div className="text-center">
            <h2 className="mb-2 text-lg font-semibold">정말 나가시겠습니까?</h2>
            <div className="text-sm">저장하지 않은 수정사항이 있습니다.</div>
          </div>
        }
      />

      <Modal2
        isOpen={showSaveModal}
        onClose={handleSaveCancel}
        icon={<HiPencilAlt className="h-8 w-8 text-white" />}
        iconBgColor="bg-primary"
        primaryButtonText="수정"
        secondaryButtonText="취소"
        primaryButtonColor="bg-[#CACACA]"
        secondaryButtonColor="bg-primary"
        onPrimaryClick={handleSaveConfirm}
        onSecondaryClick={handleSaveCancel}
        children={
          <div className="text-center">
            <h2 className="mb-2 text-lg font-semibold">
              일기를 수정하시겠습니까?
            </h2>
            <div className="text-sm">
              수정된 내용으로 일기가 <br />
              업데이트됩니다.
            </div>
          </div>
        }
      />
    </div>
  );
}
