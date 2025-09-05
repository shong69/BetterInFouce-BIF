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
      const cleanMessage = message
        .replace(
          /[\u{1F600}-\u{1F64F}]|[\u{1F300}-\u{1F5FF}]|[\u{1F680}-\u{1F6FF}]|[\u{1F1E0}-\u{1F1FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/gu,
          "",
        )
        .trim();
      onPlayTTS(cleanMessage, voiceId);
    }
  }

  function handleSpeakerKeyDown(event) {
    if (
      (event.key === "Enter" || event.key === " ") &&
      !isPlaying &&
      onPlayTTS
    ) {
      const cleanMessage = message
        .replace(
          /[\u{1F600}-\u{1F64F}]|[\u{1F300}-\u{1F5FF}]|[\u{1F680}-\u{1F6FF}]|[\u{1F1E0}-\u{1F1FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/gu,
          "",
        )
        .trim();
      onPlayTTS(cleanMessage, voiceId);
    }
  }

  return (
    <div className="flex max-w-[85%] items-start gap-2">
      <img src={logo2} alt="현명한 거북이" className="h-7 w-7" />
      <div className="w-[240px] rounded-2xl rounded-tl-md bg-white bg-gradient-to-b from-[#DAEAF8]/90 to-[#F7E6FF]/100 px-4 py-3 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
        <div className="mb-2 flex items-center gap-2">
          <span className="text-sm font-semibold text-gray-800">
            현명한 거북이
          </span>
        </div>
        <span className="mb-3 block text-sm text-gray-800">{message}</span>
        {!isHidden && showNextButton && onNextStep && (
          <div className="flex justify-end">
            <button
              className="w-13 rounded-full bg-[#343434] px-3 py-1 text-sm font-medium text-white transition-colors"
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
