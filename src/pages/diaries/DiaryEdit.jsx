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

  const emotions = EMOTIONS;

  useEffect(() => {
    const loadDiary = async () => {
      try {
        const diaryData = await fetchDiary(id);
        if (diaryData) {
          setDiary(diaryData);
          // localStorage에서 저장된 content 복원
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

  // content와 diary가 변경될 때마다 window 객체에 저장
  useEffect(() => {
    window.currentDiaryEditContent = content;
    window.currentDiaryEditDiary = diary;
    window.currentDiaryEditId = id; // id도 저장
  }, [content, diary, id]);

  useEffect(function () {
    // 전역 함수로 등록 (컴포넌트 재마운트 방지)
    if (!window.handleDiaryEditPopState) {
      window.handleDiaryEditPopState = function (_event) {
        // popstate 이벤트가 발생하면 이미 URL이 변경된 상태
        // 이전 경로가 /diaries/edit/:id였는지 확인

        // window 객체에 저장된 최신 값들 사용
        const currentContent = window.currentDiaryEditContent || "";
        const currentDiary = window.currentDiaryEditDiary || null;

        if (currentContent.trim() && currentContent !== currentDiary?.content) {
          const confirm = window.confirm(
            "수정 중인 일기가 있습니다. 나가시겠습니까?",
          );
          if (!confirm) {
            window.history.replaceState(null, "", window.location.href);
          } else {
            // 저장된 id 사용
            const currentId = window.currentDiaryEditId;
            // content를 localStorage에 저장하여 DiaryView에서 돌아올 때 복원
            localStorage.setItem(
              `diaryEditContent_${currentId}`,
              currentContent,
            );
            // navigate 대신 pushState 사용하여 popstate 이벤트 발생시키기
            window.history.pushState(null, "", `/diaries/${currentId}`);
            navigate(`/diaries/${currentId}`);
          }
        }
      };
    }

    // 컴포넌트 마운트 시 history entry 교체 (중복 방지)
    window.history.replaceState(null, "", window.location.href);

    // 이벤트 리스너가 이미 추가되었는지 확인
    if (!window.diaryEditPopStateListenerAdded) {
      window.addEventListener("popstate", window.handleDiaryEditPopState);
      window.diaryEditPopStateListenerAdded = true;
    }

    return function () {
      // 컴포넌트가 언마운트되어도 이벤트 리스너는 유지
    };
  }, []);

  useEffect(
    function () {
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
    },
    [], // 빈 의존성 배열로 변경
  );

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

    // 변경사항이 있는지 확인
    if (content.trim() === diary?.content.trim()) {
      showError("수정할 내용이 없습니다.");
      return;
    }

    // 수정 확인 모달 표시
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
                emotions.find(function (e) {
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
