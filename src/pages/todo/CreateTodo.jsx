import { useState, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";

import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import SecondaryButton from "@components/ui/SecondaryButton";

import { createTodoByAi } from "@services/todoService";
import { useToastStore, useLoadingStore } from "@stores";

export default function CreateTodo() {
  const navigate = useNavigate();
  const textareaRef = useRef(null);
  const [userInput, setUserInput] = useState("");
  const [showValidation, setShowValidation] = useState(false);
  const [lastSubmitTime, setLastSubmitTime] = useState(0);

  const { showSuccess, showError } = useToastStore();
  const {
    showLoading,
    hideLoading,
    isLoading: globalLoading,
  } = useLoadingStore();

  const adjustTextareaHeight = useCallback(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = "auto";
      const scrollHeight = Math.max(textarea.scrollHeight, 200);
      textarea.style.height = `${scrollHeight}px`;
    }
  }, []);

  function isRecentSubmit() {
    const now = Date.now();
    if (now - lastSubmitTime < 300) {
      return true;
    }
    setLastSubmitTime(now);
    return false;
  }

  function handleTitleChange(e) {
    const value = e.target.value;

    if (value.length > 1000) {
      return;
    }

    setUserInput(value);

    if (showValidation && e.target.value.trim()) {
      setShowValidation(false);
    }

    setTimeout(adjustTextareaHeight, 0);
  }

  async function handleSubmit() {
    if (globalLoading || isRecentSubmit()) {
      return;
    }

    const trimmedInput = userInput.trim();

    if (!trimmedInput) {
      setShowValidation(true);
      textareaRef.current?.focus();
      return;
    }

    if (trimmedInput.length < 3) {
      setShowValidation(true);
      return;
    }

    showLoading("할 일을 생성하는 중...");

    try {
      await createTodoByAi({
        userInput: userInput.trim(),
      });

      showSuccess("할 일이 생성되었습니다.");
      navigate("/");
    } catch (error) {
      const errorMessage = error?.message?.includes("network")
        ? "네트워크 연결을 확인해주세요."
        : "할 일 생성에 실패했습니다. 다시 시도해주세요.";

      showError(errorMessage);
    } finally {
      hideLoading();
    }
  }

  const characterCount = userInput.length;
  const hasValidationError = showValidation || characterCount >= 1000;

  return (
    <div className="min-h-screen pb-20">
      <Header />

      <div className="mx-auto max-w-4xl p-2 sm:p-4">
        <div className="space-y-6 px-2 sm:px-0">
          <div>
            <label htmlFor="todo-input" className="sr-only">
              할 일 내용
            </label>
            <textarea
              id="todo-input"
              ref={textareaRef}
              value={userInput}
              onChange={handleTitleChange}
              placeholder="시작할 업무나 할 일을 적어주세요. 시간 단계별로 정리해 드릴게요! (예:'오후 3시까지 김대리님께 메일 보내기', '냉장고 청소하기')"
              className={`focus:ring-opacity-50 w-full resize-none rounded-xl border p-4 text-base placeholder-gray-300 transition-colors focus:ring-2 focus:ring-blue-500 focus:outline-none ${
                hasValidationError
                  ? "border-red-500 focus:border-red-300"
                  : "border-gray-300 focus:border-gray-400"
              }`}
              style={{
                minHeight: "300px",
                lineHeight: "1.5",
              }}
              disabled={globalLoading}
              aria-invalid={hasValidationError}
              aria-describedby={
                hasValidationError ? "input-error" : "input-help"
              }
              maxLength={1000}
            />
          </div>

          {showValidation && (
            <div
              id="input-error"
              className="text-center"
              role="alert"
              aria-live="polite"
            >
              <p className="text-sm font-medium text-red-500">
                {!userInput.trim()
                  ? "내용을 입력해 주세요."
                  : "할 일을 3글자 이상 입력해주세요."}
              </p>
            </div>
          )}

          <div className="fixed right-0 bottom-30 left-0 px-2 sm:px-0">
            <div className="mx-auto max-w-4xl px-4">
              <SecondaryButton
                title={globalLoading ? "생성 중..." : "할 일 만들기"}
                onClick={handleSubmit}
                disabled={globalLoading || !userInput.trim()}
              />

              {!globalLoading && (
                <p className="mt-2 text-center text-xs text-gray-400">
                  구체적으로 작성할수록 더 정확한 단계를 만들어드려요
                </p>
              )}
            </div>
          </div>
        </div>
      </div>

      <TabBar />
    </div>
  );
}
