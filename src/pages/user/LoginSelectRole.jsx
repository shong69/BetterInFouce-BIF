import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useUserStore, useToastStore } from "@stores";

import Logo from "@components/ui/LoginLogo";
import PrimaryButton from "@components/ui/PrimaryButton";
import SecondaryButton from "@components/ui/SecondaryButton";
import Footer from "@components/common/Footer";

export default function LoginSelectRole() {
  const { registrationInfo, registerBif } = useUserStore();
  const { showError } = useToastStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!registrationInfo) {
      navigate("/login");
    }
  }, [registrationInfo, navigate]);

  async function handleBifSelect() {
    if (!registrationInfo) {
      navigate("/login");
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
        showError("회원가입에 실패했습니다.");
      }
    } catch {
      showError("네트워크 오류가 발생했습니다.");
    }
  }

  function handleGuardianSelect() {
    navigate("/login/invite-code");
  }

  if (!registrationInfo) {
    return null;
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
