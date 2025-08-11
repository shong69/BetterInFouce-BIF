import { useNavigate } from "react-router-dom";
import PrimaryButton from "./PrimaryButton";
import SecondaryButton from "./SecondaryButton";
import Logo from "./Logo";

// 개별 에러 페이지 컴포넌트
function ErrorPage({
  errorCode,
  title,
  message,
  details,
  onBackClick,
  onHomeClick,
}) {
  const navigate = useNavigate();

  const handleBackClick = () => {
    if (onBackClick) {
      onBackClick();
    } else {
      navigate(-1);
    }
  };

  const handleHomeClick = () => {
    if (onHomeClick) {
      onHomeClick();
    } else {
      navigate("/diaries");
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 text-center">
      {/* 로고 */}
      <div className="mb-8">
        <Logo />
      </div>

      {/* 에러 코드 */}
      <div className="mb-6">
        <div className="mb-2 bg-gradient-to-r from-yellow-400 to-lime-400 bg-clip-text text-6xl font-bold text-transparent">
          {errorCode}
        </div>
      </div>

      {/* 에러 제목 */}
      <h1 className="mb-4 text-2xl font-bold text-gray-800">{title}</h1>

      {/* 에러 메시지 */}
      <p className="mb-6 max-w-md leading-relaxed text-gray-600">{message}</p>

      {/* 상세 정보 (있는 경우에만 표시) */}
      {details && (
        <div className="mb-8 max-w-md leading-relaxed text-gray-600">
          {details}
        </div>
      )}

      {/* 버튼들 */}
      <div className="flex flex-col justify-center gap-3 sm:flex-row">
        <SecondaryButton onClick={handleBackClick} className="px-6 py-2">
          이전 페이지로
        </SecondaryButton>

        <PrimaryButton onClick={handleHomeClick} className="px-6 py-2">
          홈으로 돌아가기
        </PrimaryButton>
      </div>
    </div>
  );
}

// 에러 코드별 에러 페이지 컴포넌트들
function ErrorPage404({ message, details, onBackClick, onHomeClick }) {
  return (
    <ErrorPage
      errorCode="404"
      title="페이지를 찾을 수 없어요!"
      message={message || "찾으시는 페이지를 발견할 수 없었습니다."}
      details={details || "주소가 올바른지 확인해 주세요."}
      onBackClick={onBackClick}
      onHomeClick={onHomeClick}
    />
  );
}

function ErrorPage403({ message, details, onBackClick, onHomeClick }) {
  return (
    <ErrorPage
      errorCode="403"
      title="접근 권한이 없습니다"
      message={message}
      details={details}
      onBackClick={onBackClick}
      onHomeClick={onHomeClick}
    />
  );
}

function ErrorPage500({ message, details, onBackClick, onHomeClick }) {
  return (
    <ErrorPage
      errorCode="500"
      title="서버 오류가 발생했습니다"
      message={message}
      details={details}
      onBackClick={onBackClick}
      onHomeClick={onHomeClick}
    />
  );
}

function ErrorPage400({ message, details, onBackClick, onHomeClick }) {
  return (
    <ErrorPage
      errorCode="400"
      title="잘못된 요청입니다"
      message={message}
      details={details}
      onBackClick={onBackClick}
      onHomeClick={onHomeClick}
    />
  );
}

// 기본 에러 페이지
function ErrorPageDefault({ message, details, onBackClick, onHomeClick }) {
  return (
    <ErrorPage
      errorCode="ERROR"
      title="오류가 발생했습니다"
      message={message}
      details={details}
      onBackClick={onBackClick}
      onHomeClick={onHomeClick}
    />
  );
}

// 에러 코드별 컴포넌트 매핑
const ERROR_PAGE_COMPONENTS = {
  404: ErrorPage404,
  403: ErrorPage403,
  500: ErrorPage500,
  400: ErrorPage400,
  default: ErrorPageDefault,
};

// 메인 에러 페이지 매니저 컴포넌트
export default function ErrorPageManager({
  errorCode,
  message,
  details,
  onBackClick,
  onHomeClick,
}) {
  const ErrorPageComponent =
    ERROR_PAGE_COMPONENTS[errorCode] || ERROR_PAGE_COMPONENTS.default;

  return (
    <ErrorPageComponent
      message={message}
      details={details}
      onBackClick={onBackClick}
      onHomeClick={onHomeClick}
    />
  );
}
