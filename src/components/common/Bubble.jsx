import { useState, useEffect } from "react";
import { simulationService } from "@services/simulationService";

export default function Bubble({
  message,
  onNextStep = null,
  isLastStep = false,
  isHidden = false,
  showSpeaker = true,
  speakerIcon = "🔊",
  showNextButton = true,
  allowVoiceSelection = false,
}) {
  const [selectedVoice, setSelectedVoice] = useState("ko-KR-Chirp3-HD-Alnilam");
  const [showVoiceMenu, setShowVoiceMenu] = useState(false);
  const [isPlayingGlobal, setIsPlayingGlobal] = useState(false);

  useEffect(() => {
    const handleTTSStateChange = (isPlaying) => {
      setIsPlayingGlobal(isPlaying);
    };

    simulationService.tts.addListener(handleTTSStateChange);
    setIsPlayingGlobal(simulationService.tts.isPlaying());

    return () => {
      simulationService.tts.removeListener(handleTTSStateChange);
    };
  }, []);

  function handleSpeakerClick() {
    if (message && !isPlayingGlobal) {
      simulationService.playTTS(message, selectedVoice);
    }
  }

  function handleSpeakerKeyDown(event) {
    if ((event.key === "Enter" || event.key === " ") && !isPlayingGlobal) {
      simulationService.playTTS(message, selectedVoice);
    }
  }

  function handleVoiceChange(voiceId) {
    if (isPlayingGlobal) return;

    setSelectedVoice(voiceId);
    setShowVoiceMenu(false);
    simulationService.playTTS("안녕하세요! 새로운 목소리입니다.", voiceId);
  }

  const availableVoices = simulationService.getAvailableVoices();

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
          {allowVoiceSelection && (
            <div className="relative">
              <button
                onClick={() =>
                  !isPlayingGlobal && setShowVoiceMenu(!showVoiceMenu)
                }
                className={`text-xs hover:text-gray-700 ${
                  isPlayingGlobal
                    ? "cursor-not-allowed text-gray-300"
                    : "text-gray-500"
                }`}
                title={
                  isPlayingGlobal
                    ? "재생 중에는 음성 변경이 불가능합니다"
                    : "음성 변경"
                }
                disabled={isPlayingGlobal}
              >
                🎵
              </button>
              {showVoiceMenu && !isPlayingGlobal && (
                <div className="absolute top-full left-0 z-10 mt-1 min-w-[200px] rounded-lg border bg-white shadow-lg">
                  <div className="border-b p-2 text-xs text-gray-600">
                    음성 선택
                  </div>
                  {availableVoices.map((voice) => (
                    <button
                      key={voice.id}
                      onClick={() => handleVoiceChange(voice.id)}
                      className={`w-full px-3 py-2 text-left text-xs hover:bg-gray-100 ${
                        selectedVoice === voice.id
                          ? "bg-blue-50 text-blue-600"
                          : ""
                      }`}
                    >
                      {voice.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
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
              isPlayingGlobal
                ? "cursor-not-allowed text-blue-500"
                : "text-gray-400 hover:text-gray-600"
            }`}
            onClick={handleSpeakerClick}
            onKeyDown={handleSpeakerKeyDown}
            title={isPlayingGlobal ? "재생 중입니다..." : "음성 재생"}
            disabled={isPlayingGlobal}
          >
            {isPlayingGlobal ? "🔈" : speakerIcon}
          </button>
        </div>
      )}
    </div>
  );
}
