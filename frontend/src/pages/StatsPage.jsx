import { useEffect, useState } from "react";

import "./StatsPage.css";

export default function StatsPage() {
  const [currentDate, setCurrentDate] = useState(new Date(2026, 9, 1));

  const [days, setDays] = useState([]);

  const [loading, setLoading] = useState(true);

  const [selectedEntry, setSelectedEntry] = useState(null);

  const year = currentDate.getFullYear();

  const month = currentDate.getMonth() + 1;

  useEffect(() => {
    async function fetchCalendar() {
      setLoading(true);

      try {
        const response = await fetch(
          `/api/calendar?userId=1&year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include"
          }
        );

        if (!response.ok) {
          throw new Error("캘린더 조회 실패");
        }

        const data = await response.json();

        setDays(data.days ?? []);
      } catch (error) {
        console.error(error);

        setDays([]);
      } finally {
        setLoading(false);
      }
    }

    fetchCalendar();
  }, [year, month]);

  async function fetchCalendarDetail(moodEntryId) {
    try {
      const response = await fetch(`/api/calendar/${moodEntryId}?userId=1`, {
        method: "GET",
        credentials: "include"
      });

      if (!response.ok) {
        throw new Error("일기 상세 조회 실패");
      }

      const data = await response.json();

      setSelectedEntry(data);
    } catch (error) {
      console.error(error);
    }
  }

  const prevMonth = () => {
    setCurrentDate(new Date(year, month - 2, 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(year, month, 1));
  };

  const daysInMonth = new Date(year, month, 0).getDate();

  const firstDay = new Date(year, month - 1, 1).getDay();

  const findEntry = (day) => {
    const date = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;

    return days.find((entry) => entry.date === date);
  };

  const calendarCells = [];

  for (let i = 0; i < firstDay; i++) {
    calendarCells.push(
      <div key={`empty-${i}`} className="calendar-cell empty" />
    );
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const entry = findEntry(day);

    calendarCells.push(
      <div
        key={day}
        className={`calendar-cell ${entry ? "has-entry" : ""}`}
        onClick={() => {
          if (entry) {
            fetchCalendarDetail(entry.moodEntryId);
          }
        }}
      >
        <span className="day-number">{day}</span>

        {entry && (
          <div className="calendar-mood">
            <span className="calendar-emoji">{entry.emoji}</span>

            <span className="calendar-score">{entry.moodScore}점</span>
          </div>
        )}
      </div>
    );
  }

  return (
    <main className="stats-page">
      <header className="stats-header">
        <div>
          <span className="stats-eyebrow">MONTHLY MOOD</span>

          <h1>나의 감정 기록</h1>

          <p>한 달 동안의 마음을 한눈에 확인해보세요.</p>
        </div>
      </header>

      <section className="calendar-card">
        <div className="calendar-header">
          <button type="button" onClick={prevMonth}>
            ‹
          </button>

          <h2>
            {year}년 {month}월
          </h2>

          <button type="button" onClick={nextMonth}>
            ›
          </button>
        </div>

        <div className="calendar-weekdays">
          <span>일</span>
          <span>월</span>
          <span>화</span>
          <span>수</span>
          <span>목</span>
          <span>금</span>
          <span>토</span>
        </div>

        {loading ? (
          <p className="calendar-loading">불러오는 중...</p>
        ) : (
          <div className="calendar-grid">{calendarCells}</div>
        )}
      </section>

      {selectedEntry && (
        <section className="diary-detail">
          <div className="diary-detail-header">
            <div>
              <span className="detail-date">{selectedEntry.date}</span>

              <h2>
                {selectedEntry.emoji} {selectedEntry.emotionName}
              </h2>
            </div>

            <button
              type="button"
              className="detail-close"
              onClick={() => setSelectedEntry(null)}
            >
              ×
            </button>
          </div>

          <div className="detail-score">
            <span>
              기분 점수
              <strong>{selectedEntry.moodScore}점</strong>
            </span>

            <span>
              감정 강도
              <strong>{selectedEntry.intensity}</strong>
            </span>
          </div>

          <div className="detail-content">
            <span>오늘의 기록</span>

            <p>{selectedEntry.diaryContent || "작성된 내용이 없습니다."}</p>
          </div>
        </section>
      )}
    </main>
  );
}
