export const AGE_GROUP_OPTIONS = [
  { value: "TEEN", label: "10대" },
  { value: "TWENTIES", label: "20대" },
  { value: "THIRTIES", label: "30대" },
  { value: "FORTIES", label: "40대" },
  { value: "FIFTIES", label: "50대" },
  { value: "SIXTIES_PLUS", label: "60대 이상" }
];

export const GENDER_OPTIONS = [
  {
    value: "MALE",
    label: "남성",
    emoji: "👨"
  },
  {
    value: "FEMALE",
    label: "여성",
    emoji: "👩"
  },
  {
    value: "OTHER",
    label: "기타",
    emoji: "🧑"
  },
  {
    value: "PREFER_NOT_TO_SAY",
    label: "응답하지 않음",
    emoji: "🌿"
  }
];

export const ACTIVITY_STYLE_OPTIONS = [
  {
    value: "ACTIVE",
    label: "활동적인 편",
    description: "몸을 움직이는 활동이 좋아요.",
    emoji: "🏃"
  },
  {
    value: "CALM",
    label: "차분한 편",
    description: "편하게 쉬거나 집중하는 활동이 좋아요.",
    emoji: "🌿"
  },
  {
    value: "ANY",
    label: "상관없어요",
    description: "상황에 맞게 추천받고 싶어요.",
    emoji: "✨"
  }
];

export const ENVIRONMENT_OPTIONS = [
  {
    value: "INDOOR",
    label: "실내",
    emoji: "🏠"
  },
  {
    value: "OUTDOOR",
    label: "실외",
    emoji: "🌳"
  },
  {
    value: "ANY",
    label: "상관없어요",
    emoji: "✨"
  }
];

export const SOCIAL_OPTIONS = [
  {
    value: "ALONE",
    label: "혼자",
    emoji: "🙂"
  },
  {
    value: "SOCIAL",
    label: "함께",
    emoji: "👥"
  },
  {
    value: "ANY",
    label: "상관없어요",
    emoji: "✨"
  }
];

export const AVAILABLE_TIME_OPTIONS = [
  {
    value: 5,
    label: "5분"
  },
  {
    value: 10,
    label: "10분"
  },
  {
    value: 15,
    label: "15분"
  },
  {
    value: 30,
    label: "30분"
  },
  {
    value: null,
    label: "상관없어요"
  }
];

export const DEFAULT_ACTIVITY_STYLE = "ANY";
export const DEFAULT_ENVIRONMENT = "ANY";
export const DEFAULT_SOCIAL = "ANY";
export const DEFAULT_AVAILABLE_TIME = null;
