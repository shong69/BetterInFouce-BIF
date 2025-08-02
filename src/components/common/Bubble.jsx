export default function Bubble({
  message,
  onNextStep,
  isLastStep = false,
  isHidden = false,
  showSpeaker = true,
  speakerIcon = "🔊",
}) {
  function speakText(text) {
    if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "ko-KR";
      utterance.rate = 0.95;
      utterance.pitch = 1.1;
      speechSynthesis.speak(utterance);
    }
  }

  function handleSpeakerClick() {
    if (message) {
      speakText(message);
    }
  }

  function handleSpeakerKeyDown(event) {
    if (event.key === "Enter" || event.key === " ") {
      handleSpeakerClick();
    }
  }

  return (
    <div className="flex max-w-[85%] items-start gap-2">
      <img
        src="/src/assets/logo2.png"
        alt="현명한 거북이"
        className="h-7 w-7"
      />
      <div className="max-w-full min-w-[180px] rounded-2xl rounded-tl-md bg-white bg-gradient-to-t from-[#00FFF2]/0 to-[#08BDFF]/20 px-4 py-3 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
        <div className="mb-2 flex items-center gap-2">
          <span className="text-[13px] font-semibold text-gray-800">
            현명한 거북이
          </span>
        </div>
        <span className="mb-3 block text-sm text-gray-800">{message}</span>
        {!isHidden && onNextStep && (
          <div className="flex justify-end">
            <button
              className="bg-secondary rounded-lg px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-[#7db800]"
              onClick={onNextStep}
            >
              {isLastStep ? "완료" : "다음"}
            </button>
          </div>
        )}
      </div>
      {showSpeaker && message && (
        <button
          className="cursor-pointer text-gray-400 transition-colors hover:text-gray-600"
          onClick={handleSpeakerClick}
          onKeyDown={handleSpeakerKeyDown}
          title="음성 재생"
        >
          {speakerIcon}
        </button>
      )}
    </div>
  );
}
