import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import Modal from "@components/ui/Modal";
import SecondaryButton from "@components/ui/SecondaryButton";
import RecordButton from "@components/ui/RecordButton";
import { useDiaryStore } from "@stores/diaryStore";
import { useToastStore } from "@stores/toastStore";
import { getEmotionContent } from "@utils/emotionUtils";
import useSpeechRecorder from "@components/ui/SpeechRecorder";
import { HiOutlineTrash } from "react-icons/hi";

export default function DiaryCreate() {
  const navigate = useNavigate();
  const [content, setContent] = useState("");
  const { createDiary, selectedEmotion, clearSelectedEmotion } =
    useDiaryStore();
  const { showSuccess, showError } = useToastStore();
  const [showExitModal, setShowExitModal] = useState(false);

  const handleTextRecognized = (text) => {
    setContent((prev) => prev + (prev ? " " : "") + text);
  };

  const { isRecording, interimText, toggleRecording } =
    useSpeechRecorder(handleTextRecognized);

  const handleExitConfirm = () => {
    setShowExitModal(false);
    clearSelectedEmotion();
    navigate("/diaries");
  };

  const handleExitCancel = () => {
    setShowExitModal(false);
  };

  const handleBackClick = () => {
    if (content.trim()) {
      setShowExitModal(true);
    } else {
      navigate("/diaries");
    }
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
    } catch (error) {
      if (error.response && error.response.data) {
        showError("일기를 불러오는데 실패했습니다.");
      }
    }
  };
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
            onChange={(e) => {
              setContent(e.target.value);
            }}
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
