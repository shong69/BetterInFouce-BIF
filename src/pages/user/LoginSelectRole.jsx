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
<<<<<<< HEAD
      <div className="flex min-h-screen flex-col bg-white">
        <div className="flex justify-center pt-60 pb-1">
          <Logo />
        </div>
        <div className="-mt-40 flex flex-1 flex-col items-center justify-center px-6">
          <div className="w-full max-w-sm">
            <div className="space-y-6">
              <PrimaryButton
                onClick={handleBifSelect}
                title="보호자가 아닙니다."
              />
              <SecondaryButton
                onClick={handleGuardianSelect}
                title="보호자 입니다."
                className="text-shadow-lg/30"
              />
            </div>
          </div>
        </div>
        <Footer />
      </div>
=======
      <div className="flex justify-center pt-60 pb-20">
        <Logo />
      </div>
      <div className="flex flex-1 flex-col items-center justify-center px-6 pt-20">
        <div className="w-full max-w-sm">
          <div className="space-y-6">
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
      </div>
      <Footer />
>>>>>>> e898ae6 (style: CSS 수정 및 기타 코드 수정)
    </>
  );
}
