export function formatDuration(item) {
  if (!item) return "시간 정보 없음";

  const { durationMinutes, durationDays, durationNights } = item;

  // 일정 단위
  if (
    Number.isInteger(durationDays) &&
    durationDays >= 1 &&
    Number.isInteger(durationNights) &&
    durationNights >= 0 &&
    durationNights < durationDays
  ) {
    if (durationNights > 0) {
      return `${durationNights}박 ${durationDays}일`;
    }

    if (durationDays === 1 && item.category === "TRAVEL") {
      return "당일치기";
    }

    return `${durationDays}일`;
  }

  // 분 단위
  if (Number.isInteger(durationMinutes) && durationMinutes > 0) {
    const hours = Math.floor(durationMinutes / 60);
    const minutes = durationMinutes % 60;

    if (hours === 0) return `${minutes}분`;
    if (minutes === 0) return `${hours}시간`;

    return `${hours}시간 ${minutes}분`;
  }

  return "시간 정보 없음";
}
