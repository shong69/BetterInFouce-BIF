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

  const [error, setError] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const { fetchDiary, deleteDiary, currentDiary } = useDiaryStore();
  const { showSuccess, showError } = useToastStore();

  useEffect(() => {
    async function loadDiary() {
      try {
        await fetchDiary(id);
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
            details:
              error.message ||
              "네트워크 오류가 발생했습니다.\n다시 시도해주세요",
          });
        }
      }
    }

    loadDiary();
  }, [id, fetchDiary, showError]);

  const handleEdit = () => {
    navigate(`/diaries/edit/${id}`, {
      state: { diaryData: currentDiary },
    });
  };

  const handleDelete = async () => {
    setShowDeleteModal(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await deleteDiary(id);
      showSuccess("일기가 성공적으로 삭제되었습니다.");
      navigate("/diaries");
    } catch (error) {
      if (error.response && error.response.data) {
        const { message, details } = error.response.data;
        setError({
          errorCode: error.response.status.toString(),
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
  };

  const handleDeleteCancel = () => {
    setShowDeleteModal(false);
  };

  const generateWarningMessage = (categories) => {
    if (!categories || categories.length === 0) {
      return "부적절한 내용이 감지되었습니다.\n\n건강한 마음가짐을 위해 다른 관점에서 생각해보세요.";
    }

    const warningMap = {
      hate: "혐오 표현이 감지되었습니다.\n• 심리 상담센터 상담: 1577-0199\n• 온라인 상담 지원을 통해 도움을 받아보세요",
      violence:
        "폭력적 내용이 감지되었습니다.\n• 가정폭력/성폭력 상담: 1366\n• 지역 경찰서나 상담센터에 도움을 요청하세요",
      sexual:
        "부적절한 성적 내용이 감지되었습니다.\n• 아동/여성/장애인 경찰지원센터: 117\n• 지역 경찰서나 상담센터에 도움을 요청하세요",
      self_harm:
        "자해 관련 내용이 감지되었습니다.\n• 자살예방 상담전화: 1393\n• 언제든 연락해주세요.",
      harassment:
        "괴롭힘 관련 내용이 감지되었습니다.\n• 심리 상담센터 상담: 1577-0199\n• 사이버 상담센터: 117\n• 법적 도움도 가능합니다",
      jailbreak:
        "시스템 우회 시도가 감지되었습니다.\n• 정상적인 방법으로 서비스를 이용해주세요\n• 문제가 있다면 고객센터에 문의해주세요",
      unknown: "콘텐츠 검토중...",
    };

    const detectedCategories = categories
      .map((category) => category.trim())
      .map((category) => {
        if (category === "selfharm") {
          return "self_harm";
        }
        return category;
      })
      .filter((category) => warningMap[category])
      .map((category) => `${warningMap[category]}`)
      .join("\n");

    return (
      <div className="space-y-4">
        <div className="rounded-lg border border-gray-200 bg-white p-4">
          <div className="space-y-1">
            {detectedCategories.split("\n").map((category) => (
              <div
                key={category}
                className="text-center text-xs leading-relaxed text-gray-700"
              >
                {category}
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-lg border border-blue-200 bg-blue-50 p-4">
          <div className="mb-2 flex items-center justify-center gap-2">
            <span className="text-blue-600">🔒</span>
            <span className="text-sm font-medium text-blue-800">
              서비스 안전 정책
            </span>
          </div>
          <p className="text-center text-xs leading-relaxed text-blue-700">
            안전한 서비스 경험을 위해 민감한 주제의
            <br />
            감정일기에는 거북이의 답장이 오지 않아요.
          </p>
        </div>
      </div>
    );
  };

  const renderTurtleMessage = () => {
    if (!currentDiary) return null;

    const getMessageConfig = () => {
      const baseConfig = {
        title: "현명한 거북이",
        titleColor: "text-gray-800",
        contentColor: "text-gray-700",
        titleWeight: "font-medium",
        contentWeight: "font-medium",
      };

      if (currentDiary.contentFlagged) {
        const categories = currentDiary.contentFlaggedCategories
          ? currentDiary.contentFlaggedCategories
              .split(",")
              .map((cat) => cat.trim())
          : [];

        return {
          ...baseConfig,
          title: "",
          content: generateWarningMessage(categories),
          bgGradient: "from-[#F8F9FA] to-[#FFFFFF]",
        };
      }

      if (currentDiary.aiFeedback && currentDiary.aiFeedback.trim()) {
        return {
          ...baseConfig,
          content: currentDiary.aiFeedback,
          bgGradient: "from-[#C1EFFF] to-[#F2F7FB]",
        };
      }

      return {
        ...baseConfig,
        content: "현명한 거북이의 답장이 생성중입니다...! 🐢",
        bgGradient: "from-[#84deff] to-[#F2F7FB]",
      };
    };

    const config = getMessageConfig();

    return (
      <div
        className={`max-w-full min-w-[200px] rounded-2xl rounded-tl-md bg-gradient-to-b ${config.bgGradient} px-4 py-4 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]`}
      >
        <div className="mb-2 flex items-center gap-2">
          <span
            className={`text-md sm:text-base ${config.titleWeight} ${config.titleColor}`}
          >
            {config.title}
          </span>
        </div>
        <div
          className={`text-sm sm:text-base ${config.contentWeight} ${config.contentColor} leading-relaxed ${
            currentDiary.contentFlagged ? "whitespace-pre-line" : ""
          }`}
        >
          {config.content}
        </div>
      </div>
    );
  };

  if (error) {
    return (
      <>
        <ErrorPageManager
          errorCode={error.errorCode}
          message={error.message}
          details={error.details}
          buttonType={error.errorCode === "500" ? "home" : "back"}
        />
      </>
    );
  }

  if (!currentDiary) {
    return (
      <>
        <Header />
        <div className="mx-auto max-w-2xl p-4 sm:p-4">
          <DateBox />
          <div className="mb-4">
            <BackButton
              onClick={() => {
                navigate("/diaries");
              }}
            />
          </div>
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
        <div className="mb-4">
          <BackButton
            onClick={() => {
              navigate("/diaries");
            }}
          />
        </div>
        <div className="mb-2 flex items-end justify-between">
          <div className="mx-4 text-sm font-semibold">
            {formatDate(currentDiary.createdAt)}의 일기
          </div>
          <div className="mr-4">
            <img
              src={
                EMOTIONS.find((e) => {
                  return e.id === currentDiary.emotion;
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
              {currentDiary.content}
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
