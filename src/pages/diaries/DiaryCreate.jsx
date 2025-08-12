import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import DateBox from "@components/ui/DateBox";
import Modal from "@components/ui/Modal";
import SecondaryButton from "@components/ui/SecondaryButton";
import BackButton from "@components/ui/BackButton";
import { useDiaryStore } from "@stores/diaryStore";
import { useToastStore } from "@stores/toastStore";
import { EMOTIONS } from "@constants/emotions";
import { formatDate } from "@utils/dateUtils";

export default function DiaryCreate() {
  const navigate = useNavigate();
  const [content, setContent] = useState("");
  const { createDiary, selectedEmotion, clearSelectedEmotion } =
    useDiaryStore();
  const { showSuccess, showError } = useToastStore();
  const [showExitModal, setShowExitModal] = useState(false);

  const todayFormatted = formatDate(new Date().toISOString());

  useEffect(
    function () {
      if (!window.handleDiaryCreatePopState) {
        window.handleDiaryCreatePopState = function (_event) {
          const currentContent = window.currentDiaryCreateContent || "";

          if (currentContent.trim()) {
            const confirm = window.confirm(
              "작성 중인 일기가 있습니다. 나가시겠습니까?",
            );
            if (!confirm) {
              window.history.replaceState(null, "", window.location.href);
            } else {
              clearSelectedEmotion();
              window.history.pushState(null, "", "/diaries");
              navigate("/diaries");
            }
          }
        };
      }

      window.history.replaceState(null, "", window.location.href);

      if (!window.diaryCreatePopStateListenerAdded) {
        window.addEventListener("popstate", window.handleDiaryCreatePopState);
        window.diaryCreatePopStateListenerAdded = true;
      }

      return function () {};
    },
    [clearSelectedEmotion, navigate],
  );

  useEffect(
    function () {
      window.currentDiaryCreateContent = content;
    },
    [content],
  );

  useEffect(
    function () {
      function handleBeforeUnload(event) {
        if (content.trim()) {
          event.preventDefault();
          event.returnValue =
            "작성 중인 일기가 있습니다. 나가시면 저장되지 않습니다.";
          return event.returnValue;
        }
      }

      window.addEventListener("beforeunload", handleBeforeUnload);
      return function () {
        window.removeEventListener("beforeunload", handleBeforeUnload);
      };
    },
    [content],
  );

  const handleBack = function () {
    if (content.trim()) {
      setShowExitModal(true);
    } else {
      navigate("/diaries");
    }
  };

  const handleExitConfirm = function () {
    setShowExitModal(false);
    clearSelectedEmotion();
    navigate("/diaries");
  };
  const handleExitCancel = function () {
    setShowExitModal(false);
  };

  const handleSave = async function () {
    if (!content.trim()) {
      showError("일기 내용을 입력해주세요.");
      return;
    }

    try {
      const formattedDate = new Date().toISOString();

      await createDiary({
        date: formattedDate,
        emotion: selectedEmotion,
        content,
      });
      showSuccess("일기가 성공적으로 저장되었습니다!");
      clearSelectedEmotion();
      navigate("/diaries");
    } catch (error) {
      if (error.response && error.response.data) {
        showError("일기를 불러오는데 실패했습니다.");
      }
    }
  };
  return (
    <>
      <Header />
      <div className="mx-auto max-w-2xl p-4 sm:p-4">
        <DateBox />
        <div className="mb-2">
          <BackButton onClick={handleBack} />
        </div>
        <div className="flex items-center justify-between">
          <div className="mx-4 mb-2 text-sm font-semibold">
            {todayFormatted}의 일기
          </div>
          <div className="mr-4">
            <img
              src={
                EMOTIONS.find(function (e) {
                  return e.id === selectedEmotion;
                })?.icon
              }
              alt="emotion"
              className="mb-1 h-8 w-8 drop-shadow-md sm:h-10 sm:w-10"
            />
          </div>
        </div>

        <div className="mb-8 px-2 sm:mb-10 sm:px-0">
          <textarea
            id="diary-content"
            value={content}
            onChange={function (e) {
              setContent(e.target.value);
            }}
            placeholder="오늘 하루는 어땠나요?"
            className="h-[50vh] w-full resize-none rounded-lg border border-gray-300 p-3 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none sm:h-[60vh] sm:p-4 sm:text-base"
          />
        </div>

        <div className="px-2 sm:px-0">
          <SecondaryButton onClick={handleSave} title={"저장"} />
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
    </>
  );
}
