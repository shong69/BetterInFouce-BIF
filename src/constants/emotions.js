import angry from "@assets/angry.png";
import happy from "@assets/happy.png";
import sad from "@assets/sad.png";
import soso from "@assets/soso.png";
import veryhappy from "@assets/veryhappy.png";

export const EMOTIONS = [
  { id: "ANGER", name: "화나요!", icon: angry },
  { id: "SAD", name: "우울해요", icon: sad },
  { id: "NEUTRAL", name: "평범해요", icon: soso },
  { id: "JOY", name: "좋아요", icon: happy },
  { id: "EXCELLENT", name: "최고예요!", icon: veryhappy },
];

export function findEmotionById(id) {
  return EMOTIONS.find((emotion) => emotion.id === id);
}

export function findEmotionByName(name) {
  return EMOTIONS.find((emotion) => emotion.name === name);
}
