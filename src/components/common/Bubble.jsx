import logo2 from "@assets/logo2.png";
export default function Bubble({
  message,
  onNextStep = null,
  isLastStep = false,
  isHidden = false,
  showSpeaker = true,
  speakerIcon = "🔊",
  showNextButton = true,
  onPlayTTS = null,
  isPlaying = false,
  voiceId = "ko-KR-Chirp3-HD-Alnilam",
}) {
  function handleSpeakerClick() {
    if (message && !isPlaying && onPlayTTS) {
      onPlayTTS(message, voiceId);
    }
  }

  function handleSpeakerKeyDown(event) {
    if (
      (event.key === "Enter" || event.key === " ") &&
      !isPlaying &&
      onPlayTTS
    ) {
      onPlayTTS(message, voiceId);
    }
  }

  return (
    <div className="flex max-w-[85%] items-start gap-2">
      <img src={logo2} alt="현명한 거북이" className="h-7 w-7" />
      <div className="max-w-full min-w-[180px] rounded-2xl rounded-tl-md bg-white bg-gradient-to-t from-[#00FFF2]/0 to-[#08BDFF]/20 px-4 py-3 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
        <div className="mb-2 flex items-center gap-2">
          <span className="text-[13px] font-semibold text-gray-800">
            현명한 거북이
          </span>
        </div>
        <span className="mb-3 block text-sm text-gray-800">{message}</span>
        {!isHidden && showNextButton && onNextStep && (
          <div className="flex justify-end">
            <button
              className="bg-secondary w-13 rounded-full px-3 py-1 text-sm font-medium text-white transition-colors hover:bg-[#7db800]"
              onClick={onNextStep}
            >
              {isLastStep ? "완료" : "다음"}
            </button>
          </div>
        )}
      </div>

      {showSpeaker && message && (
        <div className="flex items-center gap-1">
          <button
            className={`cursor-pointer transition-colors ${
              isPlaying
                ? "cursor-not-allowed text-blue-500"
                : "text-gray-400 hover:text-gray-600"
            }`}
            onClick={handleSpeakerClick}
            onKeyDown={handleSpeakerKeyDown}
            title={isPlaying ? "재생 중입니다..." : "음성 재생"}
            disabled={isPlaying}
          >
            {isPlaying ? "🔈" : speakerIcon}
          </button>
        </div>
      )}
    </div>
  );
}
