import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { simulationService } from "@services/simulationService";

import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import Bubble from "@components/common/Bubble";
import BackButton from "@components/ui/BackButton";
import logo2 from "@assets/logo2.png";

function ProgressBar({ progress = 0, label = "진행도" }) {
  return (
    <div className="flex flex-col gap-0.5">
      <div className="flex items-center justify-between">
        <span className="text-[9px] font-medium text-gray-500">{label}</span>
        <span className="text-[9px] font-medium text-gray-500">
          {Math.round(progress)}%
        </span>
      </div>
      <div className="h-2 rounded-full bg-[#D3EACA]">
        <div
          className="bg-toggle h-2 rounded-full transition-all duration-300"
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
  const [showTyping, setShowTyping] = useState(false);
  const [hiddenFeedbackButtons, setHiddenFeedbackButtons] = useState(new Set());
  const conversationRef = useRef(null);
  const simrunCreatedRef = useRef(false);
  const isInitializedRef = useRef(false);
  const [isPlayingGlobal, setIsPlayingGlobal] = useState(false);

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

  useEffect(
    function () {
      if (conversationRef.current) {
        conversationRef.current.scrollTop =
          conversationRef.current.scrollHeight;
      }
    },
    [conversationHistory],
  );

  useEffect(
    function () {
      if (simulation && simulation.steps && simulation.steps[currentStep]) {
        const currentChoices = simulation.steps[currentStep].choices || [];
        const shuffled = shuffleChoices(currentChoices);
        setShuffledChoices(shuffled);
      }
    },
    [currentStep, simulation],
  );

  useEffect(function () {
    const handleTTSStateChange = (isPlaying) => {
      setIsPlayingGlobal(isPlaying);
    };

    simulationService.tts.addListener(handleTTSStateChange);
    setIsPlayingGlobal(simulationService.tts.isPlaying());

    return () => {
      simulationService.tts.removeListener(handleTTSStateChange);
    };
  }, []);

  async function handleOptionSelect(optionIndex) {
    if (selectedOption !== null) return;

    setSelectedOption(optionIndex);

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
    setShowTyping(true);

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

      const feedbackMessageObj = {
        type: "feedback",
        message: feedbackMessage,
        feedbackType: feedbackType,
        step: currentStep,
      };

      setConversationHistory(function (prev) {
        return [...prev, feedbackMessageObj];
      });
      setShowTyping(false);
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

                setShowFinal(true);
                setFinalMessage(feedbackMessage);
              } catch {
                localStorage.removeItem(`sim_${id}_total`);
                setShowFinal(true);
                setFinalMessage("시뮬레이션을 완료했습니다!");
              }
            })();
          })
          .catch(function () {
            localStorage.removeItem(`sim_${id}_total`);
            setShowFinal(true);
            setFinalMessage("시뮬레이션을 완료했습니다!");
          });
      } else {
        localStorage.removeItem(`sim_${id}_total`);
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

  function getCurrentDate() {
    const now = new Date();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
    return `${month}월 ${date}일 ${day}요일`;
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
      ? ((currentStep + 1) / simulation.steps.length) * 100
      : 0;

  return (
    <>
      <Header />

      <div className="bg-white px-5 py-5">
        <div className="text-black-600 mb-3 text-[13px] font-medium">
          {getCurrentDate()}
        </div>
        <BackButton onClick={handleBackToMain} />
      </div>

      <main className="w-full max-w-full flex-1 bg-gray-50 px-5 pb-24">
        <div className="sticky top-20 z-10 mb-6 rounded-xl bg-white p-5 shadow-sm">
          <div className="mb-3 flex items-center justify-between">
            <h3
              className={`text-[14px] font-bold ${
                simulation.category === "업무"
                  ? "text-[#EF4444]"
                  : simulation.category === "일상"
                    ? "text-[#F59E0B]"
                    : "text-[#0B70F5]"
              }`}
            >
              {simulation.title}
            </h3>
            <span
              className={`rounded-xl px-3 py-1 text-xs font-medium ${
                simulation.category === "업무"
                  ? "bg-[#FEE2E2] text-[#EF4444]"
                  : simulation.category === "일상"
                    ? "bg-[#FEF3C7] text-[#F59E0B]"
                    : "bg-[#C2DCFF] text-[#0B70F5]"
              }`}
            >
              {simulation.category}
            </span>
          </div>
          <p className="mb-4 text-sm text-gray-600">{simulation.description}</p>
          <ProgressBar progress={progress} />
        </div>

        <div className="mb-6 space-y-4" ref={conversationRef}>
          {conversationHistory.map(function (item) {
            return (
              <div
                key={`${item.type}-${item.step}-${Date.now()}-${Math.random()}`}
                className={`flex ${item.type === "user" ? "justify-end" : "justify-start"}`}
              >
                {item.type === "opponent" && item.message && (
                  <div className="flex max-w-[80%] items-start gap-2">
                    <div className="rounded-2xl rounded-tl-md bg-[#F2F7FB] px-3 py-2 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
                      <span className="text-[12px] font-medium">
                        {item.message}
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
                          simulationService.playTTS(
                            item.message,
                            "ko-KR-Chirp3-HD-Aoede",
                          );
                        }
                      }}
                      onKeyDown={function (event) {
                        if (
                          (event.key === "Enter" || event.key === " ") &&
                          !isPlayingGlobal
                        ) {
                          simulationService.playTTS(
                            item.message,
                            "ko-KR-Chirp3-HD-Aoede",
                          );
                        }
                      }}
                      title={isPlayingGlobal ? "재생 중입니다..." : "음성 재생"}
                      disabled={isPlayingGlobal}
                    >
                      {isPlayingGlobal ? "🔈" : "🔊"}
                    </button>
                  </div>
                )}

                {item.type === "user" && item.message && (
                  <div className="flex max-w-[80%] justify-end">
                    <div className="rounded-2xl rounded-tr-md bg-[#88C16F] px-3 py-2 text-black shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
                      <span className="text-[12px] font-medium">
                        {item.message}
                      </span>
                    </div>
                  </div>
                )}

                {item.type === "feedback" && (
                  <Bubble
                    message={item.message}
                    onNextStep={handleNextStep}
                    isLastStep={currentStep === simulation.steps.length - 1}
                    isHidden={hiddenFeedbackButtons.has(item.step)}
                    onPlayTTS={(message, voice) =>
                      simulationService.playTTS(message, voice)
                    }
                    isPlaying={simulationService.tts.isPlaying()}
                  />
                )}
              </div>
            );
          })}

          {showTyping && (
            <div className="flex justify-start">
              <div className="flex max-w-[90%] items-start gap-2">
                <img
                  src="/src/assets/logo2.png"
                  alt="현명한 거북이"
                  className="h-7 w-7"
                />
                <div className="max-w-full min-w-[200px] rounded-2xl rounded-tl-md bg-white bg-gradient-to-t from-[#00FFF2]/0 to-[#08BDFF]/20 px-4 py-3 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
                  <div className="mb-2 flex items-center gap-2">
                    <span className="text-[13px] font-semibold text-gray-800">
                      현명한 거북이
                    </span>
                  </div>
                  <span className="text-sm text-gray-800">
                    <span className="flex gap-1">
                      <span className="h-1 w-1 animate-pulse rounded-full bg-blue-500" />
                      <span
                        className="h-1 w-1 animate-pulse rounded-full bg-blue-500"
                        style={{ animationDelay: "0.2s" }}
                      />
                      <span
                        className="h-1 w-1 animate-pulse rounded-full bg-blue-500"
                        style={{ animationDelay: "0.4s" }}
                      />
                    </span>
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        {selectedOption === null && (
          <>
            <div className="mb-4 text-center text-sm text-gray-600">
              상황에 알맞은 답변을 선택해주세요!
            </div>
            <div className="space-y-3">
              {shuffledChoices.length > 0 ? (
                shuffledChoices.map(function (choice, index) {
                  return (
                    <button
                      key={`choice-${currentStep}-${choice.choiceText || choice}-${Date.now()}`}
                      className="w-full rounded-xl border border-gray-200 bg-white p-4 text-left shadow-sm transition-shadow hover:border-gray-300 hover:shadow-md"
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
          </>
        )}
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
                className="bg-secondary text-sb w-20 rounded-full px-4 py-1.5 font-medium text-white transition-colors hover:bg-[#7db800]"
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
