import { useNavigate } from "react-router-dom";
import PrimaryButton from "@components/ui/PrimaryButton";
import SecondaryButton from "@components/ui/SecondaryButton";
import Logo from "@components/ui/Logo";

export default function ErrorPageManager({
  errorCode = "ERROR",
  title = "오류가 발생했습니다",
  message = "문제가 발생했습니다. 다시 시도해주세요.",
  details,
  buttonType = "both",
}) {
  const navigate = useNavigate();

  const handleBackClick = () => navigate(-1);
  const handleHomeClick = () => navigate("/diaries");

  const renderButton = () => {
    switch (buttonType) {
      case "back":
        return (
          <SecondaryButton onClick={handleBackClick} className="px-6 py-2">
            이전 페이지로
          </SecondaryButton>
        );
      case "home":
        return (
          <PrimaryButton onClick={handleHomeClick} className="px-6 py-2">
            홈으로 돌아가기
          </PrimaryButton>
        );
      default:
        return (
          <>
            <SecondaryButton onClick={handleBackClick} className="px-6 py-2">
              이전 페이지로
            </SecondaryButton>
            <PrimaryButton onClick={handleHomeClick} className="px-6 py-2">
              홈으로 돌아가기
            </PrimaryButton>
          </>
        );
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 text-center">
      <div className="mb-8">
        <Logo />
      </div>

      <div className="mb-6">
        <div className="mb-2 bg-gradient-to-r from-yellow-400 to-lime-400 bg-clip-text text-6xl font-bold text-transparent">
          {errorCode}
        </div>
      </div>

      <h1 className="mb-4 text-2xl font-bold text-gray-800">{title}</h1>
      <p className="mb-6 max-w-md leading-relaxed text-gray-600">{message}</p>

      {details && (
        <div className="mb-8 max-w-md leading-relaxed text-gray-600">
          {details}
        </div>
      )}

      <div className="flex flex-col justify-center gap-3 sm:flex-row">
        {renderButton()}
      </div>
    </div>
  );
}
