import { useNavigate } from "react-router-dom";
import PrimaryButton from "@components/ui/PrimaryButton";
import LoginLogo from "@components/ui/LoginLogo";

export default function ErrorPageManager({
  errorCode = "ERROR",
  message = "문제가 발생했습니다. 다시 시도해주세요.",
  details,
  buttonType = "home",
}) {
  const navigate = useNavigate();

  const handleBackClick = function () {
    navigate(-1);
  };
  const handleHomeClick = function () {
    navigate("/");
  };

  const renderButton = function () {
    switch (buttonType) {
      case "home":
        return (
          <PrimaryButton
            title="홈으로 돌아가기"
            onClick={handleHomeClick}
            className="px-6 py-2"
          />
        );
      default:
        return (
          <PrimaryButton
            title="이전 페이지로 돌아가기"
            onClick={handleBackClick}
            className="px-6 py-2"
          />
        );
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 text-center">
      <div className="mb-8">
        <LoginLogo />
      </div>

      <div className="mb-1">
        <div className="bg-gradient-to-r from-yellow-400 to-lime-400 bg-clip-text text-7xl font-extrabold text-transparent">
          {errorCode}
        </div>
      </div>

      <p className="mb-6 max-w-md text-xl leading-relaxed font-bold text-[#8FCD00]">
        {message}
      </p>

      {details && (
        <div
          className="mb-8 max-w-md text-sm leading-relaxed text-gray-600"
          style={{ whiteSpace: "pre-line" }}
        >
          {details}
        </div>
      )}

      <div className="w-9/10 gap-3 text-center sm:flex-row">
        {renderButton()}
      </div>
    </div>
  );
}
