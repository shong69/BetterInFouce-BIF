import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useUserStore, useToastStore } from "@stores";

import Logo from "@components/ui/LoginLogo";
import PrimaryButton from "@components/ui/PrimaryButton";
import SecondaryButton from "@components/ui/SecondaryButton";
import Footer from "@components/common/Footer";
import ErrorPageManager from "@pages/errors/ErrorPage";

export default function LoginSelectRole() {
  const { registrationInfo, registerBif } = useUserStore();
  const { showError } = useToastStore();
  const navigate = useNavigate();
  const [pageError, setPageError] = useState(null);

  useEffect(() => {
    if (!registrationInfo) {
      setPageError({
        errorCode: "401",
        message: "세션 정보가 만료되었습니다.",
        details: "다시 로그인해주세요.",
      });
    }
  }, [registrationInfo]);

  async function handleBifSelect() {
    if (!registrationInfo) {
      setPageError({
        errorCode: "401",
        message: "세션 정보가 만료되었습니다.",
        details: "다시 로그인해주세요.",
      });
      return;
    }
    try {
      const result = await registerBif(
        registrationInfo.socialId,
        registrationInfo.email,
      );

      if (result.success) {
        navigate("/");
      } else {
        if (
          result.error?.includes("SERVER_ERROR") ||
          result.error?.includes("500")
        ) {
          setPageError({
            errorCode: "500",
            message: "서버 오류가 발생했습니다.",
            details: "잠시 후 다시 시도해주세요.",
          });
        } else {
          showError("회원가입에 실패했습니다.");
        }
      }
    } catch {
      setPageError({
        errorCode: "503",
        message: "네트워크 연결에 문제가 발생했습니다.",
        details: "인터넷 연결을 확인하고 다시 시도해주세요.",
      });
    }
  }

  function handleGuardianSelect() {
    navigate("/login/invite-code");
  }

  if (!registrationInfo) {
    return null;
  }

  if (pageError) {
    return (
      <ErrorPageManager
        errorCode={pageError.errorCode}
        message={pageError.message}
        details={pageError.details}
        buttonType="home"
      />
    );
  }

  return (
    <>
      <div className="min-h-screen px-6 py-8">
        <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center">
          <div className="flex w-full max-w-sm flex-col items-center space-y-8">
            <div className="mb-30 flex justify-center">
              <Logo />
            </div>

            <div className="w-full space-y-4">
              <SecondaryButton
                onClick={handleBifSelect}
                title="보호자가 아닙니다."
              />
              <PrimaryButton
                onClick={handleGuardianSelect}
                title="보호자 입니다."
                className="text-shadow-lg/30"
              />
            </div>
          </div>
          <div className="mt-30">
            <Footer />
          </div>
        </div>
      </div>
    </>
  );
}
