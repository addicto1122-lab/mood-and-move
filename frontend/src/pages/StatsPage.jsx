import { useEffect, useState } from "react";
import "./StatsPage.css";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

export default function StatsPage() {
  const [currentDate, setCurrentDate] = useState(new Date());

  const [days, setDays] = useState([]);
  const [loading, setLoading] = useState(true);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth() + 1;

  const [selectedEntry, setSelectedEntry] = useState(null);

  const [monthlyStats, setMonthlyStats] = useState(null);

  useEffect(() => {
    async function fetchCalendar() {
      try {
        const response = await fetch(
          `/api/calendar?&year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("캘린더 조회 실패");
        }

        const data = await response.json();

        setDays(data.days);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
      }
    }

    fetchCalendar();
  }, [year, month]);

  async function fetchCalendarDetail(moodEntryId) {
    try {
      const response = await fetch(`/api/calendar/${moodEntryId}?`, {
        method: "GET",
        credentials: "include",
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

  useEffect(() => {
    async function fetchMonthlyStats() {
      try {
        const response = await fetch(
          `/api/stats/monthly?&year=${year}&month=${month}`,
        );

        if (!response.ok) {
          throw new Error("월간 통계 조회 실패");
        }

        const data = await response.json();

        setMonthlyStats(data);
      } catch (error) {
        console.error(error);
      }
    }

    fetchMonthlyStats();
  }, [year, month]);

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
      <div key={`empty-${i}`} className="calendar-cell empty" />,
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
      </div>,
    );
  }

  const moodChartData = days.map((entry) => ({
    date: entry.date.substring(5),
    moodScore: entry.moodScore,
    emotion: `${entry.emoji} ${entry.emotionName}`,
  }));

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
          <button onClick={prevMonth}>‹</button>

          <h2>
            {year}년 {month}월
          </h2>

          <button onClick={nextMonth}>›</button>
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
      {monthlyStats && (
        <section className="monthly-summary">
          <h2>{monthlyStats.month}월 요약</h2>
          <div className="summary-grid">
            <div className="summary-card">
              <span>작성한 일기</span>
              <strong>{monthlyStats.diaryCount}일</strong>
            </div>
            <div className="summary-card">
              <span>평균 기분 점수</span>
              <strong>{monthlyStats.averageMoodScore}점</strong>
            </div>
            <div className="summary-card">
              <span>추천 행동</span>
              <strong>{monthlyStats.recommendationCount}회</strong>
            </div>
            <div className="summary-card">
              <span>행동 실행</span>
              <strong>{monthlyStats.executionCount}회</strong>
            </div>
            <div className="summary-card">
              <span>실행률</span>
              <strong>{monthlyStats.executionRate}%</strong>
            </div>
            <div className="summary-card">
              <span>긍정 변화율</span>
              <strong>{monthlyStats.positiveRate}%</strong>
            </div>
            <div className="summary-card">
              <span>평균 변화량</span>
              <strong>
                {monthlyStats.averageDelta > 0 ? "+" : ""}
                {monthlyStats.averageDelta}
              </strong>
            </div>
          </div>
        </section>
      )}
      {monthlyStats && monthlyStats.emotions && (
        <section className="emotion-summary">
          <h2>{monthlyStats.month}월 감정 분포</h2>
          <div className="emotion-list">
            {monthlyStats.emotions.map((emotion) => (
              <div key={emotion.emotionCode} className="emotion-item">
                <div className="emotion-info">
                  <span className="emotion-emoji">{emotion.emoji}</span>
                  <div>
                    <strong>{emotion.emotionName}</strong>
                    <span className="emotion-count">{emotion.count}회</span>
                  </div>
                </div>
                <div className="emotion-rate-area">
                  <span className="emotion-rate">{emotion.rate}%</span>
                  <div className="emotion-bar">
                    <div
                      className="emotion-bar-fill"
                      style={{ width: `${emotion.rate}%` }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
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
      {moodChartData.length > 0 && (
        <section className="mood-chart-section">
          <h2>{month}월 기분 변화</h2>
          <div className="mood-chart">
            <ResponsiveContainer width="100%" height={250}>
              <LineChart data={moodChartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis domain={[0, 60]} />
                <Tooltip formatter={(value) => [`${value}점`, "기분 점수,"]} />
                <Line type="monotone" dataKey="moodScore" strokeWidth={3} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
      )}
    </main>
  );
}
