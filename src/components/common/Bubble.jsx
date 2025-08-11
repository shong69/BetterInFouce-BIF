export default function Bubble({
  message,
  onNextStep = null,
  isLastStep = false,
  isHidden = false,
  showSpeaker = true,
  speakerIcon = "🔊",
  showNextButton = true,
}) {
  function speakText(text) {
    if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "ko-KR";

      utterance.rate = 1.1;
      utterance.pitch = 1.0;
      utterance.volume = 0.9;

      // 사용 가능한 음성들을 가져옵니다.
      const voices = speechSynthesis.getVoices();

      const preferredVoiceName = "Google 한국의";

      let selectedVoice = voices.find(
        (voice) => voice.name === preferredVoiceName,
      );

      // 만약 선호하는 음성이 없으면, 일반적인 한국어 여성 음성을 찾습니다.
      if (!selectedVoice) {
        selectedVoice =
          voices.find(
            (voice) =>
              voice.lang.includes("ko") &&
              voice.name.toLowerCase().includes("female"),
          ) ||
          voices.find(
            (voice) => voice.lang.includes("ko"), // 마지막으로, 그냥 한국어 음성 아무거나
          );
      }

      if (selectedVoice) {
        utterance.voice = selectedVoice;
      }

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
        {!isHidden && showNextButton && onNextStep && (
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
