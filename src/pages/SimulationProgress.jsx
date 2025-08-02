import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { simulationService } from "../services/simulationService";
import Header from "@components/common/Header";
import TabBar from "@components/common/TabBar";
import LoadingSpinner from "@components/ui/LoadingSpinner";
import ProgressBar from "@components/ui/ProgressBar";
import Bubble from "@components/common/Bubble";
import { useLoadingStore } from "@stores/loadingStore";

function SimulationProgress() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showLoading, hideLoading } = useLoadingStore();
  const [simulation, setSimulation] = useState(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedOption, setSelectedOption] = useState(null);
  const [score, setScore] = useState(0);
  const [showFinal, setShowFinal] = useState(false);
  const [conversationHistory, setConversationHistory] = useState([]);
  const [setFinalScore] = useState(0);
  const [finalMessage, setFinalMessage] = useState("");
  const [shuffledChoices, setShuffledChoices] = useState([]);
  const [showTyping, setShowTyping] = useState(false);
  const [hiddenFeedbackButtons, setHiddenFeedbackButtons] = useState(new Set());
  const conversationRef = useRef(null);

  useEffect(
    function () {
      const loadSimulation = async function () {
        try {
          showLoading("시뮬레이션을 불러오는 중...");
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
          } else {
            return;
          }
        } catch (error) {
          console.error("시뮬레이션 로드 오류:", error);
        } finally {
          hideLoading();
        }
      };

      loadSimulation();
    },
    [id, navigate, showLoading, hideLoading],
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

  const speakText = function (text) {
    if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "ko-KR";
      utterance.rate = 0.95;
      utterance.pitch = 1.1;
      speechSynthesis.speak(utterance);
    }
  };

  const handleSpeakerClick = function (text) {
    speakText(text);
  };

  const handleOptionSelect = function (optionIndex) {
    if (selectedOption !== null) return;

    setSelectedOption(optionIndex);

    const selectedChoice = shuffledChoices[optionIndex];
    const selectedOptionText = selectedChoice
      ? typeof selectedChoice === "string"
        ? selectedChoice
        : selectedChoice.choiceText || "선택지"
      : "선택지 없음";

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

    if (
      selectedChoice &&
      typeof selectedChoice === "object" &&
      selectedChoice.choiceScore !== undefined
    ) {
      setScore(function (prev) {
        return prev + selectedChoice.choiceScore;
      });
    } else {
      if (optionIndex === 0) {
        setScore(function (prev) {
          return prev + 10;
        });
      } else if (optionIndex === 1) {
        setScore(function (prev) {
          return prev + 5;
        });
      } else {
        setScore(function (prev) {
          return prev + 0;
        });
      }
    }
  };

  const handleNextStep = function () {
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
      const totalPossibleScore = simulation.steps.length * 10;
      const percentage = Math.round((score / totalPossibleScore) * 100);

      let completionMessage;
      if (percentage >= 80) {
        completionMessage = simulation.completion.excellent;
      } else if (percentage >= 50) {
        completionMessage = simulation.completion.good;
      } else {
        completionMessage = simulation.completion.poor;
      }

      setShowFinal(true);
      setFinalScore(percentage);
      setFinalMessage(completionMessage);
    }
  };

  const handleBackToMain = function () {
    if ("speechSynthesis" in window) {
      speechSynthesis.cancel();
    }
    navigate("/simulations");
  };

  const shuffleChoices = function (choices) {
    if (!choices || choices.length === 0) return [];

    const shuffled = [...choices];
    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
  };

  const getCurrentDate = function () {
    const now = new Date();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = ["일", "월", "화", "수", "목", "금", "토"][now.getDay()];
    return `${month}월 ${date}일 ${day}요일`;
  };

  if (!simulation) {
    return (
      <>
        <LoadingSpinner />
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
      <LoadingSpinner />
      <Header />

      <div className="bg-white px-5 py-5">
        <div className="text-black-600 mb-3 text-[13px] font-medium">
          {getCurrentDate()}
        </div>
        <button
          className="text-black-600 flex items-center text-[15px] font-extrabold transition-colors hover:text-gray-900"
          onClick={handleBackToMain}
        >
          <span className="mr-2">&#x276E;</span>
          뒤로가기
        </button>
      </div>

      <main className="w-full max-w-full flex-1 bg-gray-50 px-5 pb-24">
        <div className="mb-6 rounded-xl bg-white p-5 shadow-sm">
          <div className="mb-3 flex items-center justify-between">
            <h3
              className={`font-bold text-[14ox] ${
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
                      className="cursor-pointer text-gray-400 transition-colors hover:text-gray-600"
                      onClick={function () {
                        handleSpeakerClick(item.message);
                      }}
                      onKeyDown={function (event) {
                        if (event.key === "Enter" || event.key === " ") {
                          handleSpeakerClick(item.message);
                        }
                      }}
                      title="음성 재생"
                    >
                      🔊
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
                <div className="max-w-full min-w-[200px] rounded-2xl rounded-tl-md bg-[#F2F7FB] px-2 py-4 shadow-[0px_1px_8px_0px_rgba(0,0,0,0.25)]">
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
        <div className="bg-opacity-50 fixed inset-0 z-50 flex items-center justify-center bg-black">
          <div className="mx-4 w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <div className="mb-6 text-center">
              <div className="relative mb-4 inline-block">
                <div className="from-primary flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br to-[#0a7a06]">
                  <div className="text-2xl">🐢</div>
                </div>
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
              <h3 className="mb-4 text-xl font-bold text-gray-800">
                🎉 시뮬레이션 완료!
              </h3>
              <div className="mb-6 text-gray-600">{finalMessage}</div>
              <div className="space-y-4 text-left">
                <div>
                  <h4 className="mb-2 font-semibold text-gray-800">
                    🌟 잘한 점
                  </h4>
                  <div className="space-y-1 text-sm text-gray-600">
                    <div>• 대화 상황에 적절히 대응했습니다</div>
                    <div>• 정중하고 예의 바른 태도를 보여주었습니다</div>
                  </div>
                </div>
                <div>
                  <h4 className="mb-2 font-semibold text-gray-800">
                    💡 개선할 점
                  </h4>
                  <div className="space-y-1 text-sm text-gray-600">
                    <div>• 더 구체적이고 명확한 표현을 연습해보세요</div>
                    <div>• 상대방의 상황을 더 고려한 배려심을 기르세요</div>
                  </div>
                </div>
              </div>
            </div>
            <button
              className="bg-primary mt-6 w-full rounded-lg py-3 font-medium text-white transition-colors hover:bg-[#0a7a06]"
              onClick={handleBackToMain}
            >
              확인
            </button>
          </div>
        </div>
      )}
      <TabBar />
    </>
  );
}

export default SimulationProgress;
