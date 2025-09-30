import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { simulationService } from "@services/simulationService";

import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import Bubble from "@components/common/Bubble";
import logo2 from "@assets/logo2.png";

import managerImage from "@assets/manager.png";
import mamaImage from "@assets/mama.png";
import minaImage from "@assets/mina.png";

function ProgressBar({ progress = 0, label = "진행도" }) {
  return (
    <div className="flex flex-col gap-0.5">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-600">{label}</span>
        <span className="text-sm font-medium text-gray-600">
          {Math.round(progress)}%
        </span>
      </div>
      <div className="bg-primary/20 h-1 rounded-full">
        <div
          className="bg-primary h-1 rounded-full transition-all duration-300"
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
}

export default function SimulationProgress() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [simulation, setSimulation] = useState(null);
  const [simrunId, setsimrunId] = useState(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedOption, setSelectedOption] = useState(null);
  const [score, setScore] = useState(0);
  const [conversationHistory, setConversationHistory] = useState([]);
  const [showFinal, setShowFinal] = useState(false);
  const [finalMessage, setFinalMessage] = useState("");
  const [shuffledChoices, setShuffledChoices] = useState([]);
  const [hiddenFeedbackButtons, setHiddenFeedbackButtons] = useState(new Set());
  const conversationRef = useRef(null);
  const simrunCreatedRef = useRef(false);
  const isInitializedRef = useRef(false);
  const [isPlayingGlobal, setIsPlayingGlobal] = useState(false);
  const choicesRef = useRef(null);
  const [isCardExpanded, setIsCardExpanded] = useState(true);

  const [isTypingFeedback, setIsTypingFeedback] = useState(false);
  const [currentFeedbackMessage, setCurrentFeedbackMessage] = useState("");
  const [displayedFeedbackTexts, setDisplayedFeedbackTexts] = useState({});
  const [typingBubbleId, setTypingBubbleId] = useState(null);
  const [showChoices, setShowChoices] = useState(false);

  useEffect(
    function () {
      if (isInitializedRef.current) {
        return;
      }

      isInitializedRef.current = true;

      async function loadSimulationAndStartsimrun() {
        try {
          const simrunKey = `sim_${id}_simrun`;
          const existingsimrunId = localStorage.getItem(simrunKey);

          if (existingsimrunId) {
            setsimrunId(existingsimrunId);
            simrunCreatedRef.current = true;
            const totalKey = `sim_${id}_total`;
            localStorage.setItem(totalKey, "0");
            setScore(0);
          } else {
            clearAllsimruns();
            try {
              const startRes = await simulationService.startSimulation(
                parseInt(id),
              );

              const startsimrunId = startRes?.data || startRes?.simrunId;

              if (startsimrunId) {
                setsimrunId(startsimrunId);
                localStorage.setItem(simrunKey, startsimrunId);
                simrunCreatedRef.current = true;

                const totalKey = `sim_${id}_total`;
                localStorage.setItem(totalKey, "0");
                setScore(0);
              }
            } catch (simrunError) {
              throw ("세션 생성 실패:", simrunError);
            }
          }
          try {
            const data = await simulationService.getSimulationDetails(
              parseInt(id),
            );
            if (data) {
              setSimulation(data);
              if (data.steps && data.steps.length > 0) {
                const firstMessage = {
                  type: "opponent",
                  message:
                    data.steps[0].scenarioText ||
                    data.steps[0].character_line ||
                    "",
                  step: 0,
                };
                setConversationHistory([firstMessage]);
              }
            }
          } catch (simulationError) {
            throw ("시뮬레이션 상세 조회 실패:", simulationError);
          }
        } catch (error) {
          throw ("시뮬레이션 로드 또는 세션 시작 오류:", error);
        }
      }

      loadSimulationAndStartsimrun();
    },
    [id],
  );

  useEffect(() => {
    if (isTypingFeedback && currentFeedbackMessage && typingBubbleId) {
      setDisplayedFeedbackTexts((prev) => ({ ...prev, [typingBubbleId]: "" }));

      let index = 0;
      const interval = setInterval(() => {
        if (index < currentFeedbackMessage.length) {
          setDisplayedFeedbackTexts((prev) => ({
            ...prev,
            [typingBubbleId]: currentFeedbackMessage.slice(0, index + 1),
          }));
          index++;
        } else {
          setIsTypingFeedback(false);
          setTypingBubbleId(null);
          clearInterval(interval);
        }
      }, 50);

      return () => clearInterval(interval);
    }
  }, [isTypingFeedback, currentFeedbackMessage, typingBubbleId]);

  useEffect(
    function () {
      const scrollToTabBar = () => {
        const tabBar = document.querySelector(
          '[class*="rounded-full border border-gray-200/50 bg-white/90"]',
        );
        if (tabBar) {
          const tabBarRect = tabBar.getBoundingClientRect();
          const scrollTop = window.scrollY + tabBarRect.top - 20;
          window.scrollTo({
            top: scrollTop,
            behavior: "smooth",
          });
        }
      };

      if (isTypingFeedback) {
        const interval = setInterval(scrollToTabBar, 300);
        return () => clearInterval(interval);
      } else {
        setTimeout(scrollToTabBar, 200);
      }
    },
    [conversationHistory, isTypingFeedback],
  );

  useEffect(() => {
    if (choicesRef.current && selectedOption === null) {
      const tabBar = document.querySelector(
        '[class*="rounded-full border border-gray-200/50 bg-white/90"]',
      );
      if (tabBar) {
        const tabBarRect = tabBar.getBoundingClientRect();
        const scrollTop = window.scrollY + tabBarRect.top - 20;
        window.scrollTo({
          top: scrollTop,
          behavior: "smooth",
        });
      }
    }
  }, [shuffledChoices, selectedOption]);

  useEffect(
    function () {
      if (simulation && simulation.steps && simulation.steps[currentStep]) {
        setShowChoices(false);
        setTimeout(() => {
          const currentChoices = simulation.steps[currentStep].choices || [];
          const shuffled = shuffleChoices(currentChoices);
          setShuffledChoices(shuffled);
          setShowChoices(true);
        }, 100);
      }
    },
    [currentStep, simulation],
  );

  useEffect(function () {
    setIsPlayingGlobal(simulationService.tts.isPlaying());
  }, []);

  useEffect(() => {
    const lastMessage = conversationHistory[conversationHistory.length - 1];

    if (lastMessage && lastMessage.type === "opponent") {
      setTimeout(() => {
        const messageElements = document.querySelectorAll(
          '[class*="bg-[#F2F7FB]"]',
        );
        const lastElement = messageElements[messageElements.length - 1];

        if (lastElement) {
          const rect = lastElement.getBoundingClientRect();
          const stickyHeaderHeight = 160;
          const topOffset = 80;
          const extraSpace = 20;

          window.scrollTo({
            top:
              window.scrollY +
              rect.top -
              stickyHeaderHeight -
              topOffset -
              extraSpace,
            behavior: "smooth",
          });
        }
      }, 200);
    }
  }, [conversationHistory]);

  async function handleTTSPlay(message, voice) {
    try {
      setIsPlayingGlobal(true);
      const result = await simulationService.tts.playTTS(message, voice);

      if (!result) {
        setIsPlayingGlobal(false);
      }

      const checkTTSStatus = setInterval(() => {
        if (!simulationService.tts.isPlaying()) {
          setIsPlayingGlobal(false);
          clearInterval(checkTTSStatus);
        }
      }, 100);
    } catch {
      setIsPlayingGlobal(false);
    }
  }

  async function handleOptionSelect(optionIndex) {
    if (selectedOption !== null) return;

    setSelectedOption(optionIndex);
    setShowChoices(false);

    const selectedChoice = shuffledChoices[optionIndex];
    const selectedOptionText = selectedChoice
      ? typeof selectedChoice === "string"
        ? selectedChoice
        : selectedChoice.choiceText || "선택지"
      : "선택지 없음";

    let choiceScore = 0;
    if (selectedChoice && typeof selectedChoice === "object") {
      choiceScore =
        selectedChoice.choice_score || selectedChoice.choiceScore || 0;
      if (choiceScore > 0) {
        const simrunKey = `sim_${id}_total`;
        const existingTotal = Number(localStorage.getItem(simrunKey) || 0);
        const newTotal = existingTotal + choiceScore;

        localStorage.setItem(simrunKey, newTotal.toString());
        setScore(newTotal);
      }
    }
    const userResponse = {
      type: "user",
      message: selectedOptionText,
      step: currentStep,
    };

    setConversationHistory(function (prev) {
      return [...prev, userResponse];
    });

    setTimeout(function () {
      let feedbackType;
      let feedbackMessage;

      if (
        selectedChoice &&
        typeof selectedChoice === "object" &&
        selectedChoice.feedbackText
      ) {
        feedbackType = "success";
        feedbackMessage = selectedChoice.feedbackText;
      } else {
        if (optionIndex === 0) {
          feedbackType = "success";
          feedbackMessage = "아주 잘하셨어요!";
        } else if (optionIndex === 1) {
          feedbackType = "warning";
          feedbackMessage = "좀 더 친절하게 말해보세요.";
        } else {
          feedbackType = "error";
          feedbackMessage = "다른 방법을 시도해보세요.";
        }
      }

      const bubbleId = `feedback-${currentStep}-${Date.now()}`;
      const feedbackMessageObj = {
        type: "feedback",
        message: feedbackMessage,
        feedbackType: feedbackType,
        step: currentStep,
        id: bubbleId,
      };

      setCurrentFeedbackMessage(feedbackMessage);
      setTypingBubbleId(bubbleId);
      setIsTypingFeedback(true);

      setTimeout(() => {
        setConversationHistory(function (prev) {
          return [...prev, feedbackMessageObj];
        });
      }, 50);
    }, 1400);
  }

  function handleNextStep() {
    setHiddenFeedbackButtons(function (prev) {
      return new Set([...prev, currentStep]);
    });

    if (currentStep < simulation.steps.length - 1) {
      const nextStep = currentStep + 1;
      setCurrentStep(nextStep);
      setSelectedOption(null);
      setShowChoices(false);

      const nextStepData = simulation.steps[nextStep];
      const opponentMessage = {
        type: "opponent",
        message: nextStepData.scenarioText || nextStepData.character_line || "",
        step: nextStep,
      };

      setConversationHistory(function (prev) {
        return [...prev, opponentMessage];
      });
    } else {
      const totalScore = localStorage.getItem(`sim_${id}_total`) || score;

      if (simrunId) {
        simulationService
          .completeSimulation(simrunId, totalScore)
          .then(function () {
            (async function () {
              try {
                const feedbackResponse = await simulationService.getFeedback(
                  parseInt(id),
                  totalScore,
                );
                const feedbackData = feedbackResponse?.data ?? feedbackResponse;
                const feedbackMessage =
                  feedbackData?.feedbackText ||
                  feedbackData?.feedback ||
                  "시뮬레이션을 완료했습니다!";

                localStorage.removeItem(`sim_${id}_total`);
                localStorage.setItem(`sim_${id}_completed`, "true");

                setShowFinal(true);
                setFinalMessage(feedbackMessage);
              } catch {
                localStorage.removeItem(`sim_${id}_total`);
                localStorage.setItem(`sim_${id}_completed`, "true");
                setShowFinal(true);
                setFinalMessage("시뮬레이션을 완료했습니다!");
              }
            })();
          })
          .catch(function () {
            localStorage.removeItem(`sim_${id}_total`);
            localStorage.setItem(`sim_${id}_completed`, "true");
            setShowFinal(true);
            setFinalMessage("시뮬레이션을 완료했습니다!");
          });
      } else {
        localStorage.removeItem(`sim_${id}_total`);
        localStorage.setItem(`sim_${id}_completed`, "true");
        setShowFinal(true);
        setFinalMessage("시뮬레이션을 완료했습니다!");
      }
    }
  }

  function handleBackToMain() {
    const simrunKey = `sim_${id}_simrun`;
    localStorage.removeItem(simrunKey);
    navigate("/simulations");
  }

  function shuffleChoices(choices) {
    if (!choices || choices.length === 0) return [];

    const shuffled = [...choices];
    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
  }

  function clearAllsimruns() {
    const keys = Object.keys(localStorage);
    keys.forEach(function (key) {
      if (key.startsWith("sim_") && key.endsWith("_simrun")) {
        localStorage.removeItem(key);
      }
      if (key.startsWith("sim_") && key.endsWith("_total")) {
        localStorage.removeItem(key);
      }
    });
  }

  function getCategoryImage(category) {
    switch (category) {
      case "업무":
        return managerImage;
      case "사회":
        return mamaImage;
      case "일상":
        return minaImage;
      default:
        return managerImage;
    }
  }

  function getCategoryVoice(category) {
    switch (category) {
      case "업무":
        return "ko-KR-Chirp3-HD-Algieba";
      case "사회":
        return "ko-KR-Chirp3-HD-Vindemiatrix";
      case "일상":
        return "ko-KR-Chirp3-HD-Sulafat";
      default:
        return "ko-KR-Chirp3-HD-Algieba";
    }
  }

  function splitMessageIntoSentences(message) {
    const sentences = [];
    let currentSentence = "";

    for (let i = 0; i < message.length; i++) {
      const char = message[i];
      currentSentence += char;

      if (char === "." || char === "!" || char === "?") {
        if (i + 1 < message.length) {
          const nextChar = message[i + 1];
          if (nextChar === ")" || nextChar === '"') {
            currentSentence += nextChar;
            i++;
          }
        }

        let dotCount = 1;
        let j = i + 1;
        while (j < message.length && message[j] === ".") {
          dotCount++;
          j++;
        }

        if (dotCount >= 2) {
          for (let k = 1; k < dotCount; k++) {
            currentSentence += ".";
            i++;
          }
          continue;
        }

        if (currentSentence.trim()) {
          sentences.push(currentSentence.trim());
        }
        currentSentence = "";
      }
    }

    if (currentSentence.trim()) {
      sentences.push(currentSentence.trim());
    }

    return sentences;
  }

  if (!simulation) {
    return (
      <>
        <Header />
        <TabBar />
      </>
    );
  }

  const progress =
    simulation.steps && simulation.steps.length > 0
      ? (currentStep / simulation.steps.length) * 100
      : 0;

  return (
    <>
      <Header showTodoButton={false} />

      <main className="w-full pt-8 pb-26">
        <div className="fixed top-25 right-4 left-4 z-10 mx-auto mb-4 max-w-4xl rounded-xl border border-gray-300 bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              {isCardExpanded && (
                <img
                  src={getCategoryImage(simulation.category)}
                  alt={`${simulation.category} 캐릭터`}
                  className="h-10 w-10 object-cover object-top"
                />
              )}
              <div className="flex-1">
                <h3 className="text-sm font-bold text-black">
                  {simulation.title}
                </h3>
                {isCardExpanded && (
                  <p className="mt-1 text-xs text-gray-600">
                    {simulation.description}
                  </p>
                )}
              </div>
            </div>
            <button
              onClick={() => setIsCardExpanded(!isCardExpanded)}
              className="flex items-center gap-1 text-gray-400 hover:text-gray-600"
            >
              <svg
                className={`h-4 w-4 transition-transform ${
                  isCardExpanded ? "rotate-180" : ""
                }`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 9l-7 7-7-7"
                />
              </svg>
            </button>
          </div>
          <ProgressBar progress={progress} />
        </div>

        <div className="mx-auto w-full max-w-4xl px-4 pt-24">
          <div className="mb-6 space-y-4" ref={conversationRef}>
            {conversationHistory.map(function (item, index) {
              const nextItem = conversationHistory[index + 1];
              const shouldAddSpacing =
                (item.type === "feedback" &&
                  nextItem &&
                  nextItem.type === "opponent") ||
                (item.type === "opponent" &&
                  nextItem &&
                  nextItem.type === "feedback");

              return (
                <div
                  key={`${item.type}-${item.step}-${Date.now()}-${Math.random()}`}
                  className={`flex ${item.type === "user" ? "justify-end" : "justify-start"} ${shouldAddSpacing ? "mb-10" : ""}`}
                >
                  {item.type === "opponent" && item.message && (
                    <div className="flex max-w-[85%] items-start gap-2">
                      <img
                        src={getCategoryImage(simulation.category)}
                        alt={`${simulation.category} 캐릭터`}
                        className="h-10 w-10 flex-shrink-0 object-cover object-top"
                      />
                      <div className="flex flex-col gap-2">
                        {splitMessageIntoSentences(item.message).map(
                          (sentence) => (
                            <div
                              key={`sentence-${item.step}-${sentence.slice(0, 20).replace(/[^a-zA-Z0-9가-힣]/g, "")}`}
                              className="flex items-start gap-2"
                            >
                              <div className="rounded-2xl rounded-tl-md bg-white px-3 py-2 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
                                <span className="text-sm font-medium text-gray-800">
                                  {sentence.trim()}
                                </span>
                              </div>
                              <button
                                className={`cursor-pointer transition-colors ${
                                  isPlayingGlobal
                                    ? "cursor-not-allowed text-blue-500"
                                    : "text-gray-400 hover:text-gray-600"
                                }`}
                                onClick={function () {
                                  if (!isPlayingGlobal) {
                                    handleTTSPlay(
                                      sentence.trim(),
                                      getCategoryVoice(simulation.category),
                                    );
                                  }
                                }}
                                onKeyDown={function (event) {
                                  if (
                                    (event.key === "Enter" ||
                                      event.key === " ") &&
                                    !isPlayingGlobal
                                  ) {
                                    handleTTSPlay(
                                      sentence.trim(),
                                      getCategoryVoice(simulation.category),
                                    );
                                  }
                                }}
                                title={
                                  isPlayingGlobal
                                    ? "재생 중입니다..."
                                    : "음성 재생"
                                }
                                disabled={isPlayingGlobal}
                              >
                                {isPlayingGlobal ? "🔈" : "🔊"}
                              </button>
                            </div>
                          ),
                        )}
                      </div>
                    </div>
                  )}

                  {item.type === "user" && item.message && (
                    <div className="flex max-w-[80%] justify-end">
                      <div className="flex flex-col gap-2">
                        {splitMessageIntoSentences(item.message).map(
                          (sentence) => (
                            <div
                              key={`user-sentence-${item.step}-${sentence.slice(0, 20).replace(/[^a-zA-Z0-9가-힣]/g, "")}`}
                              className="rounded-2xl rounded-tr-md bg-white px-3 py-2 text-black shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]"
                            >
                              <span className="text-sm font-medium text-black">
                                {sentence.trim()}
                              </span>
                            </div>
                          ),
                        )}
                      </div>
                    </div>
                  )}

                  {item.type === "feedback" && (
                    <Bubble
                      message={
                        item.id && displayedFeedbackTexts[item.id] !== undefined
                          ? displayedFeedbackTexts[item.id]
                          : item.message
                      }
                      onNextStep={handleNextStep}
                      isLastStep={currentStep === simulation.steps.length - 1}
                      isHidden={hiddenFeedbackButtons.has(item.step)}
                      onPlayTTS={(message, voice) =>
                        handleTTSPlay(message, voice)
                      }
                      isPlaying={isPlayingGlobal}
                      showNextButton={
                        !(isTypingFeedback && item.id === typingBubbleId)
                      }
                    />
                  )}
                </div>
              );
            })}
          </div>

          {selectedOption === null &&
            showChoices &&
            simulation &&
            simulation.steps &&
            simulation.steps[currentStep] && (
              <div ref={choicesRef}>
                <div className="mb-4 text-center text-sm text-gray-600">
                  상황에 알맞은 답변을 선택해주세요!
                </div>
                <div className="space-y-3">
                  {shuffledChoices.length > 0 ? (
                    shuffledChoices.map(function (choice, index) {
                      const choiceId =
                        choice.id || choice.choiceId || `choice-${index}`;
                      return (
                        <button
                          key={`${currentStep}-${choiceId}`}
                          className="w-full rounded-xl border-1 border-gray-300 bg-[#343434] p-2 text-center text-sm text-[#ffffff] shadow-sm"
                          onClick={function () {
                            handleOptionSelect(index);
                          }}
                        >
                          {typeof choice === "string"
                            ? choice
                            : choice.choiceText || "선택지"}
                        </button>
                      );
                    })
                  ) : (
                    <div className="py-8 text-center text-gray-500">
                      선택지가 없습니다.
                    </div>
                  )}
                </div>
              </div>
            )}
        </div>
      </main>

      {showFinal && (
        <div className="bg-opacity-50 fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
          <div className="mx-4 w-full max-w-md rounded-2xl bg-white p-5 shadow-xl">
            <div className="text-center">
              <div className="relative mb-4 inline-block">
                <img
                  src={logo2}
                  alt="현명한거북이"
                  className="h-32 w-32 object-contain"
                />
                <div className="absolute -top-2 -right-2">
                  <span className="animate-pulse text-lg">✨</span>
                </div>
                <div className="absolute -bottom-2 -left-2">
                  <span
                    className="animate-pulse text-lg"
                    style={{ animationDelay: "0.5s" }}
                  >
                    ✨
                  </span>
                </div>
              </div>
            </div>
            <div className="text-center">
              <div className="mb-6">
                {(function () {
                  const titleEndIndex = Math.min(
                    finalMessage.indexOf("!") !== -1
                      ? finalMessage.indexOf("!") + 1
                      : finalMessage.length,
                    finalMessage.indexOf(".") !== -1
                      ? finalMessage.indexOf(".") + 1
                      : finalMessage.length,
                  );

                  const title = finalMessage.substring(0, titleEndIndex);
                  const content = finalMessage.substring(titleEndIndex).trim();

                  return (
                    <>
                      <div className="mb-3 text-lg font-bold text-black">
                        {title}
                      </div>
                      {content && (
                        <div className="text-12 text-black">{content}</div>
                      )}
                    </>
                  );
                })()}
              </div>
            </div>
            <div className="mt-6 flex justify-center">
              <button
                className="text-sb w-20 rounded-full bg-[#343434] px-4 py-1.5 font-medium text-white transition-colors"
                onClick={handleBackToMain}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}

      <TabBar />
    </>
  );
}
