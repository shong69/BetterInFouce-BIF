import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
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

  const [content, setContent] = useState("");
  const [selectedEmotion, setSelectedEmotion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [diary, setDiary] = useState(null);
  const { fetchDiary, updateDiary } = useDiaryStore();
  const { showSuccess, showError } = useToastStore();
  const [showExitModal, setShowExitModal] = useState(false);
  const [showSaveModal, setShowSaveModal] = useState(false);

  useEffect(() => {
    const loadDiary = async () => {
      try {
        const diaryData = await fetchDiary(id);
        if (diaryData) {
          setDiary(diaryData);
          const savedContent = localStorage.getItem(`diaryEditContent_${id}`);
          if (savedContent) {
            setContent(savedContent);
            localStorage.removeItem(`diaryEditContent_${id}`); // 복원 후 삭제
          } else {
            setContent(diaryData.content);
          }
          setSelectedEmotion(diaryData.emotion);
        }
      } catch (error) {
        console.error("일기 로딩 실패:", error);
        showError("일기를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    loadDiary();
  }, [id]);

  useEffect(() => {
    window.currentDiaryEditContent = content;
    window.currentDiaryEditDiary = diary;
    window.currentDiaryEditId = id; // id도 저장
  }, [content, diary, id]);

  useEffect(function () {
    if (!window.handleDiaryEditPopState) {
      window.handleDiaryEditPopState = function (_event) {
        const currentContent = window.currentDiaryEditContent || "";
        const currentDiary = window.currentDiaryEditDiary || null;

        if (currentContent.trim() && currentContent !== currentDiary?.content) {
          const confirm = window.confirm(
            "수정 중인 일기가 있습니다. 나가시겠습니까?",
          );
          if (!confirm) {
            window.history.replaceState(null, "", window.location.href);
          } else {
            const currentId = window.currentDiaryEditId;
            localStorage.setItem(
              `diaryEditContent_${currentId}`,
              currentContent,
            );
            window.history.pushState(null, "", `/diaries/${currentId}`);
            navigate(`/diaries/${currentId}`);
          }
        }
      };
    }

    window.history.replaceState(null, "", window.location.href);

    if (!window.diaryEditPopStateListenerAdded) {
      window.addEventListener("popstate", window.handleDiaryEditPopState);
      window.diaryEditPopStateListenerAdded = true;
    }

    return function () {};
  }, []);

  useEffect(function () {
    function handleBeforeUnload(event) {
      if (content.trim() && content !== diary?.content) {
        event.preventDefault();
        event.returnValue =
          "수정 중인 일기가 있습니다. 나가시면 저장되지 않습니다.";
        return event.returnValue;
      }
    }

    window.addEventListener("beforeunload", handleBeforeUnload);
    return function () {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, []);

  const handleBack = function () {
    if (content.trim() && content !== diary?.content) {
      setShowExitModal(true);
    } else {
      navigate(`/diaries/${id}`);
    }
  };

  const handleExitConfirm = function () {
    setShowExitModal(false);
    navigate(`/diaries/${id}`);
  };

  const handleExitCancel = function () {
    setShowExitModal(false);
  };

  const handleSave = async function () {
    if (!content.trim()) {
      showError("일기 내용을 입력해주세요.");
      return;
    }

    if (content.trim() === diary?.content.trim()) {
      showError("수정할 내용이 없습니다.");
      return;
    }

    setShowSaveModal(true);
  };

  const handleSaveConfirm = async function () {
    try {
      await updateDiary(id, { content });
      showSuccess("일기가 성공적으로 수정되었습니다!");
      setShowSaveModal(false);
      navigate(`/diaries/${id}`);
    } catch (error) {
      showError("일기 수정에 실패했습니다.");
      console.error("일기 수정 실패:", error);
    }
  };

  const handleSaveCancel = function () {
    setShowSaveModal(false);
  };

  if (loading) {
    return (
      <>
        <Header />
        <div className="flex h-64 items-center justify-center">
          <div className="text-lg">로딩 중...</div>
        </div>
        <TabBar />
      </>
    );
  }

  if (!diary) {
    return (
      <>
        <Header />
        <div className="flex h-64 items-center justify-center">
          <div className="text-lg">일기를 찾을 수 없습니다.</div>
        </div>
        <TabBar />
      </>
    );
  }

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
            {formatDate(diary.createdAt)}의 일기
          </div>
          <div className="mr-4">
            <img
              src={
                EMOTIONS.find(function (e) {
                  return e.id === selectedEmotion;
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
            onChange={function (e) {
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
