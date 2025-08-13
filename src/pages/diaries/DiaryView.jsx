import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import BackButton from "@components/ui/BackButton";
import EditButton from "@components/ui/EditButton";
import DeleteButton from "@components/ui/DeleteButton";
import DateBox from "@components/ui/DateBox";
import Modal from "@components/ui/Modal";

import ErrorPageManager from "@pages/errors/ErrorPage";
import { useDiaryStore } from "@stores/diaryStore";
import { useToastStore } from "@stores/toastStore";
import { formatDate } from "@utils/dateUtils";
import { EMOTIONS } from "@constants/emotions";

export default function DiaryView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [diary, setDiary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const { fetchDiary, deleteDiary } = useDiaryStore();
  const { showSuccess, showError } = useToastStore();

  useEffect(
    function () {
      async function loadDiary() {
        try {
          const diaryData = await fetchDiary(id);
          if (diaryData) {
            setDiary(diaryData);
          }
        } catch (error) {
          if (error.response && error.response.data) {
            const { message, details } = error.response.data;
            setError({
              errorCode: error.response.status.toString(),
              message: message || "일기를 불러오는데 실패했습니다.",
              details: details || null,
            });
          } else {
            setError({
              errorCode: "500",
              message: "일기를 불러오는데 실패했습니다.",
              details: error.message || "네트워크 오류가 발생했습니다.",
            });
          }
        } finally {
          setLoading(false);
        }
      }

      loadDiary();
    },
    [id, fetchDiary, showError],
  );

  useEffect(function () {
    window.history.replaceState(null, "", window.location.href);
  }, []);

  useEffect(
    function () {
      function handleDiaryViewPopState(_event) {
        if (window.location.pathname.match(/^\/diaries\/\d+$/)) {
          navigate("/diaries");
        }
      }

      window.addEventListener("popstate", handleDiaryViewPopState);

      return function () {
        window.removeEventListener("popstate", handleDiaryViewPopState);
      };
    },
    [navigate],
  );

  function handleEdit() {
    navigate(`/diaries/edit/${id}`);
  }

  async function handleDelete() {
    setShowDeleteModal(true);
  }

  async function handleDeleteConfirm() {
    try {
      await deleteDiary(id);
      showSuccess("일기가 성공적으로 삭제되었습니다.");
      navigate("/diaries");
    } catch (error) {
      if (error.response && error.response.data) {
        const { errorCode, message, details } = error.response.data;
        setError({
          errorCode: errorCode || error.response.status.toString(),
          message: message || "일기 삭제에 실패했습니다.",
          details: details || null,
        });
      } else {
        setError({
          errorCode: "500",
          message: "일기 삭제에 실패했습니다.",
          details: error.message || "네트워크 오류가 발생했습니다.",
        });
      }
    } finally {
      setShowDeleteModal(false);
    }
  }

  function handleDeleteCancel() {
    setShowDeleteModal(false);
  }

  function generateWarningMessage(categories) {
    if (categories.length === 0) {
      return "부적절한 내용이 감지되었습니다.\n\n건강한 마음가짐을 위해 다른 관점에서 생각해보세요.";
    }

    const warningMap = {
      hate: "혐오 표현이 포함되어 있습니다.",
      violence: "폭력적 내용이 포함되어 있습니다.",
      sexual: "부적절한 성적 내용이 포함되어 있습니다.",
      self_harm: "자해 관련 내용이 포함되어 있습니다.",
      harassment: "괴롭힘 관련 내용이 포함되어 있습니다.",
    };

    let warningText = "⚠️ 다음 내용이 감지되었습니다:\n\n";

    categories.forEach(function (category) {
      const trimmedCategory = category.trim();
      const warning =
        warningMap[trimmedCategory] || `${trimmedCategory} 관련 부적절한 내용`;
      warningText += `• ${warning}\n`;
    });

    warningText += "\n건강한 마음가짐을 위해 다른 관점에서 생각해보세요.";
    return warningText;
  }

  function renderTurtleMessage() {
    if (!diary) return null;

    let messageConfig = {
      title: "현명한 거북이",
      content: "현명한 거북이의 답장이 생성중입니다...! 🐢",
      bgGradient: "from-[#84deff] to-[#F2F7FB]",
      titleColor: "text-gray-800",
      contentColor: "text-gray-700",
      titleWeight: "font-semibold",
      contentWeight: "font-medium",
    };

    if (diary.contentFlagged) {
      const categories = diary.contentFlaggedCategories
        ? diary.contentFlaggedCategories.split(",")
        : [];

      messageConfig = {
        title: "⚠️ 주의사항",
        content: generateWarningMessage(categories),
        bgGradient: "from-[#ffb3b3] to-[#ffe6e6]",
        titleColor: "text-red-800",
        contentColor: "text-red-700",
        titleWeight: "font-bold",
        contentWeight: "font-semibold",
      };
    } else if (diary.aiFeedback) {
      messageConfig = {
        title: "현명한 거북이",
        content: diary.aiFeedback,
        bgGradient: "from-[#C1EFFF] to-[#F2F7FB]",
        titleColor: "text-gray-800",
        contentColor: "text-gray-700",
        titleWeight: "font-medium",
        contentWeight: "font-medium",
      };
    }

    return (
      <div
        className={`max-w-full min-w-[200px] rounded-2xl rounded-tl-md bg-gradient-to-b ${messageConfig.bgGradient} px-4 py-4 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]`}
      >
        <div className="mb-2 flex items-center gap-2">
          <span
            className={`text-md sm:text-base ${messageConfig.titleWeight} ${messageConfig.titleColor}`}
          >
            {messageConfig.title}
          </span>
        </div>
        <div
          className={`text-sm sm:text-base ${messageConfig.contentWeight} ${messageConfig.contentColor} leading-relaxed ${diary.contentFlagged ? "whitespace-pre-line" : ""}`}
        >
          {messageConfig.content}
        </div>
      </div>
    );
  }

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

  if (error) {
    return (
      <>
        <ErrorPageManager
          errorCode={error.errorCode}
          message={error.message}
          details={error.details}
          buttonType="back"
        />
      </>
    );
  }

  return (
    <>
      <Header />
      <div className="mx-auto max-w-2xl p-4 sm:p-4">
        <DateBox />
        <div className="mb-4">
          <BackButton
            onClick={function () {
              navigate("/diaries");
            }}
          />
        </div>
        <div className="mb-2 flex items-end justify-between">
          <div className="mx-4 text-sm font-semibold">
            {formatDate(diary.createdAt)}의 일기
          </div>
          <div className="mr-4">
            <img
              src={
                EMOTIONS.find(function (e) {
                  return e.id === diary.emotion;
                })?.icon
              }
              alt="emotion"
              className="h-8 w-8 drop-shadow-md sm:h-10 sm:w-10"
            />
          </div>
        </div>
        <div className="mb-8 px-2 sm:mb-10 sm:px-0">
          <div className="min-h-[10vh] w-full p-3 text-sm sm:min-h-[30vh] sm:p-4 sm:text-base">
            <p className="leading-relaxed font-medium whitespace-pre-wrap text-[#4A4A4A]">
              {diary.content}
            </p>
          </div>
        </div>
        <div className="flex justify-start">
          <div className="flex max-w-[95%] items-start gap-2">
            <img
              src="/src/assets/logo2.png"
              alt="현명한 거북이"
              className="h-10 w-10"
            />
            {renderTurtleMessage()}
          </div>
        </div>
        <div className="mt-4 px-2 sm:mt-4 sm:px-0">
          <div className="mb-[78px] flex justify-end gap-2 sm:mb-[78px]">
            <EditButton onClick={handleEdit} />
            <DeleteButton onClick={handleDelete} />
          </div>
        </div>
      </div>
      <TabBar />

      <Modal
        isOpen={showDeleteModal}
        onClose={handleDeleteCancel}
        primaryButtonText="삭제"
        secondaryButtonText="취소"
        onPrimaryClick={handleDeleteConfirm}
        onSecondaryClick={handleDeleteCancel}
        children={
          <div className="text-center">
            <h2 className="mb-2 text-lg font-semibold">
              일기를 삭제하시겠습니까?
            </h2>
            <div className="text-sm">삭제된 일기는 복구할 수 없습니다.</div>
          </div>
        }
      />
    </>
  );
}
