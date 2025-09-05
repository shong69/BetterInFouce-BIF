import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import Logo from "@components/ui/LoginLogo";
import Footer from "@components/common/Footer";
import ErrorPageManager from "@pages/errors/ErrorPage";

export default function Login() {
  const [searchParams] = useSearchParams();
  const [error, setError] = useState(null);

  const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

  useEffect(() => {
    const errorParam = searchParams.get("error");
    if (errorParam) {
      setError({
        errorCode: "401",
        message: "로그인 중 문제가 발생했습니다.",
        details: "다시 시도해주세요.",
      });
    }
  }, [searchParams]);

  function handleGoogleLogin() {
    window.location.href = `${API_BASE_URL}/api/oauth2/authorization/google`;
  }
  function handleKakaoLogin() {
    window.location.href = `${API_BASE_URL}/api/oauth2/authorization/kakao`;
  }
  function handleNaverLogin() {
    window.location.href = `${API_BASE_URL}/api/oauth2/authorization/naver`;
  }

  if (error) {
    return (
      <ErrorPageManager
        errorCode={error.errorCode}
        message={error.message}
        details={error.details}
        buttonType="home"
      />
    );
  }

  return (
    <>
      <div className="min-h-screen px-6 py-8">
        <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center">
          <div className="flex w-full max-w-sm flex-col items-center space-y-8">
            <div className="flex justify-center">
              <Logo />
            </div>
            <div className="space-y-2 text-center">
              <h1 className="mb-2 text-sm font-bold text-gray-900">
                함께하니까, 더 나은 집중
              </h1>
            </div>

            <div className="w-full space-y-4">
              <button
                onClick={handleGoogleLogin}
                className="flex w-full items-center justify-center gap-3 rounded-xl border border-gray-300 bg-white px-4 py-3 transition-colors duration-200 hover:bg-gray-50"
              >
                <svg className="h-5 w-5" viewBox="0 0 24 24">
                  <path
                    fill="#4285F4"
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  />
                  <path
                    fill="#34A853"
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                  />
                  <path
                    fill="#EA4335"
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                  />
                </svg>
                <span className="font-medium text-gray-700">
                  Google로 시작하기
                </span>
              </button>

              <button
                onClick={handleKakaoLogin}
                className="flex w-full items-center justify-center gap-3 rounded-xl bg-[#FEE500] px-4 py-3 transition-colors duration-200 hover:bg-yellow-400"
              >
                <svg className="h-5 w-5" viewBox="0 0 24 24">
                  <path
                    fill="#3C1E1E"
                    d="M12 3C6.486 3 2 6.486 2 10.667c0 2.667 1.758 5.029 4.384 6.459l-.891 3.267c-.058.213.106.427.323.427a.324.324 0 00.249-.12l3.896-3.065A11.73 11.73 0 0012 17.333c5.514 0 10-3.486 10-7.666S17.514 3 12 3z"
                  />
                </svg>
                <span className="font-medium text-gray-900">
                  카카오로 시작하기
                </span>
              </button>

              <button
                onClick={handleNaverLogin}
                className="flex w-full items-center justify-center gap-3 rounded-xl bg-green-500 px-4 py-3 transition-colors duration-200 hover:bg-green-600"
              >
                <svg className="h-5 w-5" viewBox="0 0 24 24">
                  <path
                    fill="white"
                    d="M16.273 12.845L7.376 0H0v24h7.726V11.156L16.624 24H24V0h-7.727v12.845z"
                  />
                </svg>
                <span className="font-medium text-white">
                  네이버로 시작하기
                </span>
              </button>
            </div>
            <p className="text-tiny mt-8 text-center leading-relaxed text-gray-500">
              로그인 시 이용약관 및 개인정보처리방침에 동의하게 됩니다.
            </p>
          </div>
          <div className="mt-16">
            <Footer />
          </div>
        </div>
      </div>
    </>
  );
}
