import { useEffect, useState } from "react";
import "./CalendarPage.css";

import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from "recharts";
import { useNavigate } from "react-router-dom";

const EMOTION_STYLE = {
  JOY: {
    color: "#F6C445",
    bg: "#FFF6D8",
    text: "#8A6200",
  },

  CALM: {
    color: "#7BC96F",
    bg: "#EAF7E7",
    text: "#2E6B2E",
  },

  NEUTRAL: {
    color: "#B8BDC7",
    bg: "#F3F4F6",
    text: "#4B5563",
  },

  SAD: {
    color: "#5B8DEF",
    bg: "#EAF1FF",
    text: "#244C9A",
  },

  ANXIOUS: {
    color: "#F39C4A",
    bg: "#FFF1E5",
    text: "#9A4F12",
  },

  ANGRY: {
    color: "#E85D5D",
    bg: "#FFEAEA",
    text: "#992B2B",
  },
};

function renderEmotionLabel({
  cx,
  cy,
  midAngle,
  innerRadius,
  outerRadius,
  payload,
}) {
  const RADIAN = Math.PI / 180;

  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;

  const x = cx + radius * Math.cos(-midAngle * RADIAN);

  const y = cy + radius * Math.sin(-midAngle * RADIAN);

  return (
    <text
      x={x}
      y={y}
      textAnchor="middle"
      dominantBaseline="central"
      fontSize="22"
      pointerEvents="none"
      tabIndex={-1}
      focusable="false"
      aria-hidden="true"
    >
      {payload.emoji}
    </text>
  );
}

function CalendarPage() {
  // 현재 보고 있는 월
  const [currentDate, setCurrentDate] = useState(new Date());

  // 캘린더 데이터
  const [days, setDays] = useState([]);

  const [loading, setLoading] = useState(true);

  // 선택한 일기
  const [selectedEntry, setSelectedEntry] = useState(null);

  // 월간 감정 통계
  const [monthlyStats, setMonthlyStats] = useState(null);

  // 감정 분포 상세보기
  const [showDistribution, setShowDistribution] = useState(false);

  const year = currentDate.getFullYear();

  const month = currentDate.getMonth() + 1;

  const navigate = useNavigate();

  const [selectedMoodEntryId, setSelectedMoodEntryId] = useState(null);

  /*
   * =========================
   * 월별 캘린더 조회
   * =========================
   */
  useEffect(() => {
    async function fetchCalendar() {
      try {
        setLoading(true);

        const response = await fetch(
          `/api/calendar?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("캘린더 조회 실패");
        }

        const data = await response.json();

        setDays(data.days || []);
      } catch (error) {
        console.error(error);

        setDays([]);
      } finally {
        setLoading(false);
      }
    }

    fetchCalendar();
  }, [year, month]);

  /*
   * =========================
   * 월간 감정 통계 조회
   * =========================
   */
  useEffect(() => {
    async function fetchMonthlyStats() {
      try {
        const response = await fetch(
          `/api/stats/monthly?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("월간 통계 조회 실패");
        }

        const data = await response.json();

        setMonthlyStats(data);
      } catch (error) {
        console.error(error);

        setMonthlyStats(null);
      }
    }

    fetchMonthlyStats();
  }, [year, month]);

  /*
   * =========================
   * 일기 상세 조회
   * =========================
   */
  async function fetchCalendarDetail(moodEntryId) {
    try {
      const response = await fetch(`/api/calendar/${moodEntryId}`, {
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

  const handleEntryClick = (entry) => {
    if (!entry) {
      return;
    }

    if (selectedMoodEntryId === entry.moodEntryId) {
      setSelectedMoodEntryId(null);
      setSelectedEntry(null);

      return;
    }

    setSelectedMoodEntryId(entry.moodEntryId);
    fetchCalendarDetail(entry.moodEntryId);
  };

  /*
   * 이전 달
   */
  const prevMonth = () => {
    setCurrentDate(new Date(year, month - 2, 1));

    setSelectedEntry(null);

    setShowDistribution(false);
  };

  /*
   * 다음 달
   */
  const nextMonth = () => {
    setCurrentDate(new Date(year, month, 1));

    setSelectedEntry(null);

    setShowDistribution(false);
  };

  /*
   * =========================
   * 캘린더 날짜 생성
   * =========================
   */

  const daysInMonth = new Date(year, month, 0).getDate();

  const firstDay = new Date(year, month - 1, 1).getDay();

  const findEntry = (day) => {
    const date = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;

    return days.find((entry) => entry.date === date);
  };

  const calendarCells = [];

  /*
   * 월 시작 전 빈 칸
   */
  for (let i = 0; i < firstDay; i++) {
    calendarCells.push(
      <div key={`empty-${i}`} className="calendar-cell empty" />,
    );
  }

  /*
   * 실제 날짜
   */
  for (let day = 1; day <= daysInMonth; day++) {
    const entry = findEntry(day);

    calendarCells.push(
      <button
        type="button"
        key={day}
        className={`calendar-cell 
          ${entry ? "has-entry" : ""}
          ${entry?.moodEntryId === selectedMoodEntryId ? "selected" : ""}
          `}
        onClick={() => {
          handleEntryClick(entry);
        }}
      >
        <span className="day-number">{day}</span>

        {entry && (
          <span className="calendar-mood">
            <span className="calendar-emoji">{entry.emoji}</span>
          </span>
        )}
      </button>,
    );
  }

  /*
   * =========================
   * 감정 그래프 데이터
   * =========================
   */

  const emotionChartData =
    monthlyStats?.emotions?.map((emotion) => {
      const style = EMOTION_STYLE[emotion.emotionCode] || {
        color: "#B8BDC7",
        bg: "#F3F4F6",
        text: "#4B5563",
      };

      return {
        name: emotion.emotionName,

        emotionCode: emotion.emotionCode,

        emoji: emotion.emoji,

        value: emotion.count,

        rate: emotion.rate,

        fill: style.color,

        bg: style.bg,

        text: style.text,
      };
    }) || [];

  /*
   * 이번 달 가장 많이 등장한 감정
   */
  const dominantEmotion = emotionChartData.reduce((max, emotion) => {
    if (!max) {
      return emotion;
    }

    return emotion.value > max.value ? emotion : max;
  }, null);

  return (
    <main className="calendar-page">
      {/* =========================
          Header
      ========================= */}

      <header className="calendar-page-title">
        <span>MOOD CALENDAR</span>

        <h1>마음 캘린더</h1>

        <p>하루하루 기록한 감정을 돌아보세요.</p>
      </header>

      {/* =========================
          Calendar
      ========================= */}

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
          <p className="calendar-loading">기록을 불러오는 중...</p>
        ) : (
          <div className="calendar-grid">{calendarCells}</div>
        )}
      </section>

      {/* 이번 달 통계 보로 가기 */}
      <button
        type="button"
        className="stats-link-button"
        onClick={() => navigate(`/stats?year=${year}&month=${month}`)}
      >
        <span>이 달 통계 보로가기</span>
        <span>→</span>
      </button>

      {/* =========================
          선택한 일기
      ========================= */}

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
              onClick={() => {
                setSelectedEntry(null);
                setSelectedMoodEntryId(null);
              }}
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

      {/* =========================
          이번 달 대표 감정
      ========================= */}

      {dominantEmotion && (
        <section className="dominant-emotion-card">
          <div className="dominant-title">
            <span>이번 달의 마음</span>

            <p>{month}월에 가장 많이 기록한 감정이에요.</p>
          </div>

          <div className="dominant-content">
            <span className="dominant-emoji">{dominantEmotion.emoji}</span>

            <div>
              <strong>{dominantEmotion.name}</strong>

              <p>
                {dominantEmotion.value}회 · {dominantEmotion.rate}%
              </p>
            </div>
          </div>

          <button
            type="button"
            className="distribution-button"
            onClick={() => setShowDistribution(!showDistribution)}
          >
            {showDistribution ? "감정 분포 접기" : "감정 분포 상세보기"}
          </button>
        </section>
      )}

      {/* =========================
          감정 분포 상세
      ========================= */}

      {showDistribution && emotionChartData.length > 0 && (
        <section className="emotion-summary">
          <h2>{month}월 감정 분포</h2>

          <div className="emotion-donut-card">
            <div className="emotion-donut-chart">
              <ResponsiveContainer width="100%" height={260}>
                <PieChart accessibilityLayer={false}>
                  <Pie
                    data={emotionChartData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={65}
                    outerRadius={100}
                    paddingAngle={3}
                    labelLine={false}
                    label={renderEmotionLabel}
                    rootTabIndex={-1}
                  >
                    {emotionChartData.map((emotion) => (
                      <Cell
                        key={emotion.emotionCode}
                        fill={emotion.fill}
                        tabIndex={-1}
                      />
                    ))}
                  </Pie>

                  <Tooltip
                    formatter={(value, name, props) => [
                      `${value}회 (${props.payload.rate}%)`,
                      `${props.payload.emoji} ${name}`,
                    ]}
                  />
                </PieChart>
              </ResponsiveContainer>

              <div className="emotion-donut-center">
                <strong>{month}월</strong>

                <span>총 {monthlyStats?.diaryCount || 0}회</span>
              </div>
            </div>

            <div className="emotion-legend-list">
              {emotionChartData.map((emotion) => (
                <div
                  key={emotion.emotionCode}
                  className="emotion-legend-item"
                  style={{
                    backgroundColor: emotion.bg,

                    borderLeft: `5px solid ${emotion.fill}`,
                  }}
                >
                  <div className="emotion-legend-left">
                    <span className="emotion-legend-emoji">
                      {emotion.emoji}
                    </span>

                    <div>
                      <strong
                        style={{
                          color: emotion.text,
                        }}
                      >
                        {emotion.name}
                      </strong>

                      <p>{emotion.value}회</p>
                    </div>
                  </div>

                  <strong
                    className="emotion-legend-rate"
                    style={{
                      color: emotion.text,
                    }}
                  >
                    {emotion.rate}%
                  </strong>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}
    </main>
  );
}

export default CalendarPage;
