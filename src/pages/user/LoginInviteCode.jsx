import { useState } from "react";
import Logo from "@components/ui/Logo";
import PrimaryButton from "@components/ui/PrimaryButton";
import Footer from "@components/common/Footer";

export default function LoginInviteCode() {
  const [inviteCode, setInviteCode] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

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

    setLoading(true);
    setError("");

    try {
      const sessionResponse = await fetch("/api/auth/session-info", {
        credentials: "include",
      });

      if (!sessionResponse.ok) {
        setError("세션 정보를 가져올 수 없습니다. 다시 로그인해주세요.");
        window.location.href = "/login";
        return;
      }

      const sessionData = await sessionResponse.json();

      if (!sessionData.success || !sessionData.data.registrationInfo) {
        setError("세션 정보가 올바르지 않습니다.");
        window.location.href = "/login";
        return;
      }

      const sessionInfo = sessionData.data.registrationInfo;

      const response = await fetch("/api/auth/register/guardian", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          socialId: sessionInfo.socialId,
          email: sessionInfo.email,
          connectionCode: code,
        }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        window.location.href = "/";
      } else {
        setError(data.message || "초대코드가 올바르지 않습니다.");
      }
    } catch (error) {
      console.error("보호자 회원가입 실패:", error);
      setError("네트워크 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  const isCodeComplete = inviteCode.every((digit) => digit !== "");

  return (
    <>
      <div className="flex min-h-screen flex-col bg-white">
        <div className="flex justify-center pt-60 pb-1">
          <Logo />
        </div>
        <div className="-mt-50 flex flex-1 flex-col items-center justify-center px-6">
          <div className="w-full max-w-sm">
            <div className="mb-15 text-center">
              <h1 className="text-md mb-2 font-semibold text-gray-900">
                인증번호를 입력해주세요
              </h1>
            </div>

            <div className="mb-6 flex justify-center gap-2">
              {inviteCode.map((digit, index) => (
                <input
                  key={index}
                  id={`code-${index}`}
                  type="text"
                  value={digit}
                  onChange={(e) => handleCodeChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  onPaste={handlePaste}
                  maxLength={1}
                  className="h-12 w-12 rounded-lg border-2 border-gray-300 text-center text-xl font-semibold transition-colors focus:outline-none"
                />
              ))}
            </div>

            <p className="mb-6 text-center text-sm">
              {error ? (
                <span className="text-red-500">{error}</span>
              ) : (
                <span className="text-gray-500">
                  인증번호 6자를 입력해 주세요.
                </span>
              )}
            </p>

            <PrimaryButton
              onClick={handleSubmit}
              title={loading ? "확인 중..." : "확인"}
              disabled={loading || !isCodeComplete}
            />
          </div>
        </div>
        <Footer />
      </div>
    </>
  );
}
