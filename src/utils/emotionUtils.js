import { EMOTIONS } from "@constants/emotions";

export const getEmotionContent = (emotionId) => {
  const emotion = EMOTIONS.find((e) => e.id === emotionId);
  if (!emotion) return { text: "", icon: null, name: "", bgColor: "" };

  const emotionTexts = {
    ANGER: "오늘 많이 화가 나셨나요?\n무엇이 당신을 그렇게 만들었나요?",
    SAD: "오늘 우울한 기분이시군요.\n혹시 무슨 이유가 있는 걸까요?",
    NEUTRAL: "오늘은 평범한 하루였나요?\n특별한 순간을 생각해봐요.",
    JOY: "오늘 기분이 좋으셨군요!\n무엇이 당신을 행복하게 했나요?",
    EXCELLENT: "오늘 매우 기분이 좋으셨군요!\n어떤 좋은 일이 있었나요?",
  };

  const emotionColors = {
    ANGER: "bg-red-50",
    SAD: "bg-blue-50",
    NEUTRAL: "bg-gray-50",
    JOY: "bg-pink-50",
    EXCELLENT: "bg-yellow-50",
  };

  return {
    text: emotionTexts[emotionId] || "",
    icon: emotion.icon,
    name: emotion.name,
    bgColor: emotionColors[emotionId] || "bg-gray-50 text-gray-700",
  };
};

export const getEmotionInfo = (emotionId) => {
  const emotion = EMOTIONS.find((e) => e.id === emotionId);
  if (!emotion) return { icon: null, name: "", bgColor: "" };

  const emotionColors = {
    ANGER: "bg-red-50",
    SAD: "bg-blue-50",
    NEUTRAL: "bg-gray-50",
    JOY: "bg-pink-50",
    EXCELLENT: "bg-yellow-50",
  };

  return {
    icon: emotion.icon,
    name: emotion.name,
    bgColor: emotionColors[emotionId] || "bg-gray-50 text-gray-700",
  };
};
