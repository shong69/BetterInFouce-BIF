import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useUserStore } from "@stores";

import Logo from "@components/ui/LoginLogo";
import PrimaryButton from "@components/ui/PrimaryButton";
import Footer from "@components/common/Footer";

export default function LoginInviteCode() {
  const { registrationInfo, registerGuardian } = useUserStore();
  const navigate = useNavigate();
  const [inviteCode, setInviteCode] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!registrationInfo) {
      navigate("/login");
    }
  }, [registrationInfo, navigate]);

  function handleCodeChange(index, value) {
    const lastChar = value.slice(-1);
    if (!/^[a-zA-Z0-9]*$/.test(lastChar)) return;

    const newCode = [...inviteCode];
    newCode[index] = value;
    setInviteCode(newCode);

    if (value && index < 5) {
      const nextInput = document.getElementById(`code-${index + 1}`);
      if (nextInput) nextInput.focus();
    }
  }

  function handleKeyDown(index, e) {
    if (e.key === "Backspace" && !inviteCode[index] && index > 0) {
      const prevInput = document.getElementById(`code-${index - 1}`);
      if (prevInput) {
        prevInput.focus();
        const newCode = [...inviteCode];
        newCode[index - 1] = "";
        setInviteCode(newCode);
      }
    }
  }

  function handlePaste(e) {
    e.preventDefault();
    const pastedData = e.clipboardData.getData("text");
    const validChars = pastedData.replace(/[^a-zA-Z0-9]/g, "").slice(0, 6);

    if (validChars.length > 0) {
      const newCode = [...inviteCode];
      for (let i = 0; i < 6; i++) {
        newCode[i] = validChars[i] || "";
      }
      setInviteCode(newCode);

      const lastIndex = Math.min(validChars.length - 1, 5);
      const lastInput = document.getElementById(`code-${lastIndex}`);
      if (lastInput) lastInput.focus();
    }
  }

  async function handleSubmit() {
    const code = inviteCode.join("");

    if (code.length !== 6) {
      setError("초대코드 6자리를 모두 입력해주세요.");
      return;
    }

    if (!registrationInfo) {
      setError("세션 정보가 없습니다. 다시 로그인해주세요.");
      navigate("/login");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const result = await registerGuardian(
        registrationInfo.socialId,
        registrationInfo.email,
        code,
      );

      if (result.success) {
        navigate("/");
      } else {
        setError("초대코드가 올바르지 않습니다.");
      }
    } catch {
      setError("네트워크 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  const isCodeComplete = inviteCode.every((digit) => digit !== "");

  return (
    <>
      <div className="flex justify-center pt-60 pb-15">
        <Logo />
      </div>
      <div className="mb-15 text-center">
        <h1 className="mb-2 text-sm font-bold text-gray-900">
          인증번호를 입력해주세요
        </h1>
      </div>

      <div className="mb-10 flex justify-center gap-2">
        {inviteCode.map((digit, index) => (
          <input
            // 인증 코드 입력은 고정된 길이이므로 index 사용 허용
            // eslint-disable-next-line react/no-array-index-key
            key={index}
            id={`code-${index}`}
            type="text"
            value={digit}
            onChange={(e) => handleCodeChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            onPaste={handlePaste}
            maxLength={1}
            className="h-12 w-12 rounded-lg border-2 border-gray-300 text-center text-xl font-semibold transition-colors focus:border-green-500 focus:outline-none"
          />
        ))}
      </div>

      <p className="mb-6 text-center text-sm">
        {error ? (
          <span className="text-red-500">{error}</span>
        ) : (
          <span className="text-gray-500">인증번호 6자를 입력해 주세요.</span>
        )}
      </p>

      <div className="flex justify-center px-6">
        <div className="w-full max-w-sm">
          <PrimaryButton
            onClick={handleSubmit}
            title={loading ? "확인 중..." : "확인"}
            disabled={loading || !isCodeComplete}
          />
        </div>
      </div>
      <Footer />
    </>
  );
}
