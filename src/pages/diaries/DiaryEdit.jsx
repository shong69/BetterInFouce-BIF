import { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
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

export default function DiaryEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [content, setContent] = useState("");
  const [showExitModal, setShowExitModal] = useState(false);
  const [showSaveModal, setShowSaveModal] = useState(false);
  const [originalContent, setOriginalContent] = useState("");

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
        <Header />
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
    <>
      <Header />
      <div className="mx-auto mb-24 max-w-2xl p-4 sm:p-4">
        <DateBox />
        <div className="mb-2">
          <BackButton onClick={handleBack} />
        </div>
        <div className="flex items-center justify-between">
          <div className="mx-4 mb-2 text-sm font-semibold">
            {formatDate(diaryData.createdAt)}의 일기
          </div>
          <div className="mr-4">
            <img
              src={
                EMOTIONS.find((e) => {
                  return e.id === diaryData.emotion;
                })?.icon
              }
              alt="emotion"
              className="mb-1 h-8 w-8 sm:h-10 sm:w-10"
            />
          </div>
        </div>
        <div className="mb-8 px-2 sm:mb-10 sm:px-0">
          <textarea
            id="diary-content"
            value={content}
            onChange={(e) => {
              setContent(e.target.value);
            }}
            placeholder="오늘 하루는 어땠나요?"
            className="h-[50vh] w-full resize-none rounded-lg border border-gray-300 p-3 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none sm:h-[60vh] sm:p-4 sm:text-base"
          />
        </div>

        <div className="px-2 sm:px-0">
          <SecondaryButton onClick={handleSave} title={"일기 수정"} />
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
            <div className="text-sm">저장하지 않은 수정사항이 있습니다.</div>
          </div>
        }
      />

      <Modal
        isOpen={showSaveModal}
        onClose={handleSaveCancel}
        primaryButtonText="수정"
        secondaryButtonText="취소"
        onPrimaryClick={handleSaveConfirm}
        onSecondaryClick={handleSaveCancel}
        children={
          <div className="text-center">
            <h2 className="mb-2 text-lg font-semibold">
              일기를 수정하시겠습니까?
            </h2>
            <div className="text-sm">
              수정된 내용으로 일기가 업데이트됩니다.
            </div>
          </div>
        }
      />
    </>
  );
}
