import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "@components/ui/Logo";
import PrimaryButton from "@components/ui/PrimaryButton";
import SecondaryButton from "@components/ui/SecondaryButton";
import Footer from "@components/common/Footer";

export default function LoginSelectRole() {
  const [sessionInfo, setSessionInfo] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetch("/api/auth/session-info", { credentials: "include" })
      .then((res) => res.json())
      .then((data) => {
        if (data.success && data.data.registrationInfo) {
          setSessionInfo(data.data.registrationInfo);
        } else {
          navigate("/login");
        }
      });
  }, [navigate]);

  async function handleBifSelect() {
    const response = await fetch("/api/auth/register/bif", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({
        socialId: sessionInfo.socialId,
        email: sessionInfo.email,
      }),
    });

    if (response.ok) {
      navigate("/");
    }
  }

  function handleGuardianSelect() {
    navigate("/login/invite-code");
  }

  return (
    <>
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
    </>
  );
}
