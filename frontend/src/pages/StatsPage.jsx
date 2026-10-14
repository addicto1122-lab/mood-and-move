import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LabelList,
} from "recharts";

import { authFetch } from "../api/authApi";

import "./StatsPage.css";

/* =========================================================
   날짜 상수
========================================================= */

const DAY_NAMES = ["일", "월", "화", "수", "목", "금", "토"];

/* =========================================================
   날짜 유틸
========================================================= */

function normalizeDate(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function addDays(date, amount) {
  const result = normalizeDate(date);

  result.setDate(result.getDate() + amount);

  return result;
}

function parseDateKey(dateKey) {
  const [year, month, day] = dateKey.split("-").map(Number);

  return new Date(year, month - 1, day);
}

function formatDateKey(date) {
  const year = date.getFullYear();

  const month = String(date.getMonth() + 1).padStart(2, "0");

  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function formatShortDate(date) {
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
}

function formatAxisDate(date) {
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function formatDetailDate(dateKey) {
  if (!dateKey) {
    return "";
  }

  const [, month, day] = dateKey.split("-").map(Number);

  return `${month}월 ${day}일`;
}

function getDaysBetween(start, end) {
  const millisecondsPerDay = 1000 * 60 * 60 * 24;

  return Math.floor(
    (normalizeDate(end) - normalizeDate(start)) / millisecondsPerDay,
  );
}

/* =========================================================
   처음 선택할 월
========================================================= */

function createInitialMonth(searchParams) {
  const today = normalizeDate(new Date());

  const queryYear = Number(searchParams.get("year"));

  const queryMonth = Number(searchParams.get("month"));

  const currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);

  if (!queryYear || queryMonth < 1 || queryMonth > 12) {
    return currentMonth;
  }

  const requestedMonth = new Date(queryYear, queryMonth - 1, 1);

  /*
   * 미래 월 접근 방지
   */
  if (requestedMonth > currentMonth) {
    return currentMonth;
  }

  return requestedMonth;
}

/* =========================================================
   평균
========================================================= */

function calculateAverage(values) {
  const validValues = values.filter(
    (value) => value != null && Number.isFinite(Number(value)),
  );

  if (validValues.length === 0) {
    return null;
  }

  const total = validValues.reduce((sum, value) => sum + Number(value), 0);

  return Math.round(total / validValues.length);
}

/* =========================================================
   Tooltip
========================================================= */

function MoodBarTooltip({ active, payload }) {
  if (!active || !payload?.length) {
    return null;
  }

  const data = payload[0]?.payload;

  if (!data) {
    return null;
  }

  return (
    <div className="mood-chart-tooltip">
      <strong>{data.fullDate}</strong>

      {data.emotion && <p>{data.emotion}</p>}

      {data.status === "EMPTY" ? (
        <p>감정 기록이 없습니다.</p>
      ) : (
        <>
          <p>행동 전: {data.beforeScore}점</p>

          {data.status === "SKIPPED" ? (
            <p>추천 행동: 건너뜀</p>
          ) : data.afterScore != null ? (
            <p>행동 후: {data.afterScore}점</p>
          ) : (
            <p>행동 후: 기록 없음</p>
          )}
        </>
      )}
    </div>
  );
}

/* =========================================================
   X축 Tick
========================================================= */

function WeekAxisTick({ x, y, payload, dataMap }) {
  const item = dataMap.get(payload.value);

  if (!item) {
    return null;
  }

  return (
    <g transform={`translate(${x},${y})`}>
      <text x={0} y={0} textAnchor="middle" fill="#66756d">
        <tspan x={0} dy={14} fontSize="9" fontWeight="700">
          {item.axisDate}
        </tspan>

        <tspan x={0} dy={13} fontSize="9">
          ({item.weekDay})
        </tspan>

        {item.status === "EMPTY" && (
          <tspan x={0} dy={13} fontSize="8" fill="#9aa49f">
            기록 없음
          </tspan>
        )}

        {item.status === "SKIPPED" && (
          <tspan x={0} dy={13} fontSize="8" fill="#8b8178">
            스킵
          </tspan>
        )}

        {item.status === "PENDING" && (
          <tspan x={0} dy={13} fontSize="8" fill="#8b8178">
            재측정 전
          </tspan>
        )}
      </text>
    </g>
  );
}

export default function StatsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  /* =========================================================
     오늘
  ========================================================= */

  const today = useMemo(() => normalizeDate(new Date()), []);

  /* =========================================================
     현재 상단에서 선택한 월
  ========================================================= */

  const [currentMonth, setCurrentMonth] = useState(() =>
    createInitialMonth(searchParams),
  );

  const year = currentMonth.getFullYear();

  const month = currentMonth.getMonth() + 1;

  /* =========================================================
     선택한 월의 전체 일기

     용도:
     - 첫 번째 일기 찾기
     - 월 평균
  ========================================================= */

  const [monthMoodEntries, setMonthMoodEntries] = useState([]);

  /* =========================================================
     현재 7일 그래프용 일기

     9/29 ~ 10/5처럼
     월을 넘을 수 있으므로 별도 관리
  ========================================================= */

  const [chartMoodEntries, setChartMoodEntries] = useState([]);

  /* =========================================================
     사용자가 주간 < >를 눌렀을 때
     직접 선택된 시작 날짜

     null이면:
     선택 월 첫 일기 날짜 사용
  ========================================================= */

  const [manualWeekStartDate, setManualWeekStartDate] = useState(null);

  /* =========================================================
     기타 상태
  ========================================================= */

  const [monthlyStats, setMonthlyStats] = useState(null);

  const [actionEffects, setActionEffects] = useState({
    bestAction: null,
    executionRanking: [],
    effectRanking: [],
  });

  const [categoryMeta, setCategoryMeta] = useState({});

  const [loading, setLoading] = useState(true);

  const [chartLoading, setChartLoading] = useState(true);

  const [activeTab, setActiveTab] = useState("SUMMARY");

  const [rankingModal, setRankingModal] = useState(null);

  const [selectedDateKey, setSelectedDateKey] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchActionCategories() {
      try {
        const response = await authFetch("/api/action-categories", {
          method: "GET",
          credentials: "include",
        });

        const data = await response.json();

        if (cancelled) {
          return;
        }

        const meta = Object.fromEntries(
          (data ?? []).map((item) => [
            item.category,
            {
              categoryName: item.categoryName,
              emoji: item.emoji,
            },
          ]),
        );

        setCategoryMeta(meta);
      } catch (error) {
        console.error("행동 카테고리 조회 실패:", error);

        if (!cancelled) {
          setCategoryMeta({});
        }
      }
    }

    fetchActionCategories();

    return () => {
      cancelled = true;
    };
  }, []);

  const getCategoryName = (category) => {
    return categoryMeta[category]?.categoryName ?? category;
  };

  const getCategoryEmoji = (category) => {
    return categoryMeta[category]?.emoji ?? "✨";
  };

  /* =========================================================
     선택 월 Calendar 조회
  ========================================================= */

  useEffect(() => {
    let cancelled = false;

    async function fetchMonthMoodEntries() {
      try {
        const response = await authFetch(
          `/api/calendar?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("월 감정 기록 조회 실패");
        }

        const data = await response.json();

        if (cancelled) {
          return;
        }

        setMonthMoodEntries(data.days ?? []);

        setLoading(false);
      } catch (error) {
        console.error("월 감정 기록 조회 실패:", error);

        if (!cancelled) {
          setMonthMoodEntries([]);
          setLoading(false);
        }
      }
    }

    fetchMonthMoodEntries();

    return () => {
      cancelled = true;
    };
  }, [year, month]);

  /* =========================================================
     선택 월의 데이터만 정렬

     이전 월 데이터가 잠시 state에 남아 있어도
     현재 선택 월만 사용하게 방어
  ========================================================= */

  const sortedMoodEntries = useMemo(() => {
    const prefix = `${year}-${String(month).padStart(2, "0")}`;

    return [...monthMoodEntries]
      .filter(
        (entry) =>
          entry?.date &&
          entry.moodScore != null &&
          entry.date.startsWith(prefix),
      )
      .sort((a, b) => a.date.localeCompare(b.date));
  }, [monthMoodEntries, year, month]);

  /* =========================================================
     선택 월 첫 번째 일기 날짜
  ========================================================= */

  const firstDiaryDate = useMemo(() => {
    if (sortedMoodEntries.length === 0) {
      return null;
    }

    return parseDateKey(sortedMoodEntries[0].date);
  }, [sortedMoodEntries]);

  /* =========================================================
     기본 주간 시작 날짜

     상단에서 월을 바꾸면
     그 달 첫 번째 일기부터
  ========================================================= */

  const defaultWeekStartDate = firstDiaryDate;

  /* =========================================================
     실제 그래프 시작 날짜

     주간 화살표를 안 눌렀으면:
     첫 일기

     눌렀으면:
     manualWeekStartDate
  ========================================================= */

  const activeWeekStartDate = manualWeekStartDate ?? defaultWeekStartDate;

  /* =========================================================
     그래프 마지막 날짜

     무조건 시작 + 6일.

     단 오늘 이후만 자른다.

     월말은 절대로 자르지 않음.

     예:
     9/29
     →
     10/5
  ========================================================= */

  const weekEndDate = useMemo(() => {
    if (!activeWeekStartDate) {
      return null;
    }

    let end = addDays(activeWeekStartDate, 6);

    /*
     * 미래는 표시하지 않는다.
     */
    if (end > today) {
      end = today;
    }

    return end;
  }, [activeWeekStartDate, today]);

  /* =========================================================
     현재 월 여부
  ========================================================= */

  const isCurrentMonth =
    year === today.getFullYear() && month === today.getMonth() + 1;

  /* =========================================================
     현재 7일 그래프 데이터 조회

     9/29 ~ 10/5
     →
     9월 + 10월 Calendar API 모두 조회
  ========================================================= */

  useEffect(() => {
    if (!activeWeekStartDate || !weekEndDate) {
      return;
    }

    let cancelled = false;

    async function fetchChartMoodEntries() {
      try {
        const startYear = activeWeekStartDate.getFullYear();

        const startMonth = activeWeekStartDate.getMonth() + 1;

        const endYear = weekEndDate.getFullYear();

        const endMonth = weekEndDate.getMonth() + 1;

        const targets = [
          {
            year: startYear,
            month: startMonth,
          },
          {
            year: endYear,
            month: endMonth,
          },
        ];

        /*
         * 같은 달이면 중복 제거
         */
        const uniqueTargets = targets.filter(
          (target, index, array) =>
            array.findIndex(
              (item) =>
                item.year === target.year && item.month === target.month,
            ) === index,
        );

        const responses = await Promise.all(
          uniqueTargets.map((target) =>
            authFetch(
              `/api/calendar?year=${target.year}&month=${target.month}`,
              {
                method: "GET",
                credentials: "include",
              },
            ),
          ),
        );

        if (responses.some((response) => !response.ok)) {
          throw new Error("주간 감정 기록 조회 실패");
        }

        const dataList = await Promise.all(
          responses.map((response) => response.json()),
        );

        if (cancelled) {
          return;
        }

        const merged = dataList.flatMap((data) => data.days ?? []);

        setChartMoodEntries(merged);

        setChartLoading(false);
      } catch (error) {
        console.error("주간 감정 기록 조회 실패:", error);

        if (!cancelled) {
          setChartMoodEntries([]);
          setChartLoading(false);
        }
      }
    }

    fetchChartMoodEntries();

    return () => {
      cancelled = true;
    };
  }, [activeWeekStartDate, weekEndDate]);

  /* =========================================================
     월 변경
  ========================================================= */

  const moveToMonth = (targetMonth) => {
    const current = new Date(today.getFullYear(), today.getMonth(), 1);

    /*
     * 미래 월 이동 금지
     */
    if (targetMonth > current) {
      return;
    }

    /*
     * 여기서는 사용자 이벤트이므로
     * setState 사용 문제 없음
     */

    setManualWeekStartDate(null);
    setSelectedDateKey(null);

    setMonthMoodEntries([]);
    setChartMoodEntries([]);

    setMonthlyStats(null);

    setActionEffects({
      bestAction: null,
      executionRanking: [],
      effectRanking: [],
    });

    setLoading(true);
    setChartLoading(true);

    setCurrentMonth(targetMonth);

    setSearchParams({
      year: targetMonth.getFullYear(),

      month: targetMonth.getMonth() + 1,
    });
  };

  /* =========================================================
     상단 이전 월
  ========================================================= */

  const prevMonth = () => {
    const target = new Date(year, month - 2, 1);

    moveToMonth(target);
  };

  /* =========================================================
     상단 다음 월
  ========================================================= */

  const nextMonth = () => {
    const target = new Date(year, month, 1);

    moveToMonth(target);
  };

  /*
   * =========================================================
   * 주간 그래프 날짜에 맞춰
   * 상단 년/월도 변경
   * =========================================================
   *
   * 예:
   *
   * 9/29 ~ 10/5
   * → 시작일이 9월이므로 상단 9월
   *
   * 10/6 ~ 10/12
   * → 시작일이 10월이므로 상단 10월
   *
   * 주의:
   * moveToMonth()는 사용하지 않는다.
   *
   * moveToMonth()를 호출하면
   * manualWeekStartDate가 null로 초기화되어
   * 해당 월 첫 일기로 돌아가버리기 때문.
   */
  const syncMonthWithWeek = (weekStart) => {
    const targetMonth = new Date(
      weekStart.getFullYear(),
      weekStart.getMonth(),
      1,
    );

    const sameMonth =
      targetMonth.getFullYear() === currentMonth.getFullYear() &&
      targetMonth.getMonth() === currentMonth.getMonth();

    if (sameMonth) {
      return;
    }

    /*
     * 월이 실제로 바뀔 때
     * 월간 데이터 로딩 준비
     */
    setMonthMoodEntries([]);
    setMonthlyStats(null);

    setActionEffects({
      bestAction: null,
      executionRanking: [],
      effectRanking: [],
    });

    setLoading(true);

    /*
     * manualWeekStartDate는 건드리지 않는다.
     * 그래프 위치를 그대로 유지하기 위해서.
     */
    setCurrentMonth(targetMonth);

    setSearchParams({
      year: targetMonth.getFullYear(),
      month: targetMonth.getMonth() + 1,
    });
  };

  /* =========================================================
     이전 7일

     선택한 월의 첫 일기 이전으로는 못 감
  ========================================================= */

  const prevWeek = () => {
    if (!activeWeekStartDate) {
      return;
    }

    const previousStart = addDays(activeWeekStartDate, -7);

    setManualWeekStartDate(previousStart);

    setSelectedDateKey(null);

    setChartMoodEntries([]);
    setChartLoading(true);

    /*
     * 10월 → 9월로 넘어가면
     * 상단 월도 9월로 변경
     */
    syncMonthWithWeek(previousStart);
  };

  /* =========================================================
     다음 7일

     월말은 신경 쓰지 않고
     7일씩 계속 이동

     단 미래로는 못 감
  ========================================================= */

  const nextWeek = () => {
    if (!activeWeekStartDate || !weekEndDate) {
      return;
    }

    const nextStart = addDays(activeWeekStartDate, 7);

    /*
     * 미래로는 이동 불가
     */
    if (nextStart > today) {
      return;
    }

    /*
     * 그래프 시작 날짜 변경
     */
    setManualWeekStartDate(nextStart);

    setSelectedDateKey(null);

    setChartMoodEntries([]);
    setChartLoading(true);

    /*
     * 9월 → 10월처럼
     * 주간 시작 월이 달라지면
     * 상단 월도 변경
     */
    syncMonthWithWeek(nextStart);
  };

  /* =========================================================
     이전 주 버튼 가능 여부
  ========================================================= */

  const canGoPrevWeek = Boolean(activeWeekStartDate);

  /* =========================================================
     다음 주 버튼 가능 여부
  ========================================================= */

  const canGoNextWeek = useMemo(() => {
    if (!activeWeekStartDate) {
      return false;
    }

    const nextStart = addDays(activeWeekStartDate, 7);

    return nextStart <= today;
  }, [activeWeekStartDate, today]);

  /* =========================================================
     월간 요약
  ========================================================= */

  useEffect(() => {
    let cancelled = false;

    async function fetchMonthlyStats() {
      try {
        const response = await authFetch(
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

        if (!cancelled) {
          setMonthlyStats(data);
        }
      } catch (error) {
        console.error("월간 통계 조회 실패:", error);

        if (!cancelled) {
          setMonthlyStats(null);
        }
      }
    }

    fetchMonthlyStats();

    return () => {
      cancelled = true;
    };
  }, [year, month]);

  /* =========================================================
     행동 효과
  ========================================================= */

  useEffect(() => {
    let cancelled = false;

    async function fetchActionEffects() {
      try {
        const response = await authFetch(
          `/api/stats/monthly/actions?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("행동별 효과 통계 조회 실패");
        }

        const data = await response.json();

        if (cancelled) {
          return;
        }

        setActionEffects({
          bestAction: data.bestAction ?? null,

          executionRanking: data.executionRanking ?? [],

          effectRanking: data.effectRanking ?? [],
        });
      } catch (error) {
        console.error("행동별 효과 통계 조회 실패:", error);

        if (!cancelled) {
          setActionEffects({
            bestAction: null,
            executionRanking: [],
            effectRanking: [],
          });
        }
      }
    }

    fetchActionEffects();

    return () => {
      cancelled = true;
    };
  }, [year, month]);

  /* =========================================================
     그래프용 일기 Map
  ========================================================= */

  const chartMoodEntryMap = useMemo(() => {
    return new Map(chartMoodEntries.map((entry) => [entry.date, entry]));
  }, [chartMoodEntries]);

  /* =========================================================
     현재 7일 BarChart 데이터
  ========================================================= */

  const weekChartData = useMemo(() => {
    if (!activeWeekStartDate || !weekEndDate) {
      return [];
    }

    /*
     * 과거 구간:
     * 대부분 7
     *
     * 현재 구간:
     * 오늘 때문에 7보다 짧을 수 있음
     */
    const length = getDaysBetween(activeWeekStartDate, weekEndDate) + 1;

    return Array.from(
      {
        length,
      },
      (_, index) => {
        const date = addDays(activeWeekStartDate, index);

        const dateKey = formatDateKey(date);

        const entry = chartMoodEntryMap.get(dateKey);

        /* =========================
             기록 없음
          ========================= */

        if (!entry) {
          return {
            dateKey,

            fullDate: dateKey,

            axisDate: formatAxisDate(date),

            weekDay: DAY_NAMES[date.getDay()],

            beforeScore: null,

            afterScore: null,

            status: "EMPTY",

            emotion: null,

            actionName: null,
          };
        }

        const beforeScore =
          entry.moodScore == null ? null : Number(entry.moodScore);

        /* =========================
             SKIP
          ========================= */

        if (entry.skipped === true) {
          return {
            dateKey,

            fullDate: dateKey,

            axisDate: formatAxisDate(date),

            weekDay: DAY_NAMES[date.getDay()],

            beforeScore,

            afterScore: null,

            status: "SKIPPED",

            emotion: `${entry.emoji ?? ""} ${entry.emotionName ?? ""}`.trim(),

            actionName: entry.actionName ?? null,
          };
        }

        /* =========================
             재측정 전
          ========================= */

        if (entry.afterScore == null) {
          return {
            dateKey,

            fullDate: dateKey,

            axisDate: formatAxisDate(date),

            weekDay: DAY_NAMES[date.getDay()],

            beforeScore,

            afterScore: null,

            status: "PENDING",

            emotion: `${entry.emoji ?? ""} ${entry.emotionName ?? ""}`.trim(),

            actionName: entry.actionName ?? null,
          };
        }

        /* =========================
             완료
          ========================= */

        return {
          dateKey,

          fullDate: dateKey,

          axisDate: formatAxisDate(date),

          weekDay: DAY_NAMES[date.getDay()],

          beforeScore,

          afterScore: Number(entry.afterScore),

          status: "COMPLETED",

          emotion: `${entry.emoji ?? ""} ${entry.emotionName ?? ""}`.trim(),

          actionName: entry.actionName ?? null,
        };
      },
    );
  }, [activeWeekStartDate, weekEndDate, chartMoodEntryMap]);

  /* =========================================================
     X축 Tick Map
  ========================================================= */

  const weekChartDataMap = useMemo(() => {
    return new Map(weekChartData.map((item) => [item.dateKey, item]));
  }, [weekChartData]);

  /* =========================================================
     선택한 월 평균 행동 전
  ========================================================= */

  const averageBefore = useMemo(() => {
    return calculateAverage(sortedMoodEntries.map((entry) => entry.moodScore));
  }, [sortedMoodEntries]);

  /* =========================================================
     선택한 월 평균 행동 후

     SKIP 제외
     재측정 없는 경우 제외
  ========================================================= */

  const averageAfter = useMemo(() => {
    return calculateAverage(
      sortedMoodEntries
        .filter((entry) => entry.skipped !== true && entry.afterScore != null)
        .map((entry) => entry.afterScore),
    );
  }, [sortedMoodEntries]);

  /* =========================================================
     기본 선택 날짜

     현재 구간에서
     가장 최근 작성된 일기
  ========================================================= */

  const defaultSelectedDateKey = useMemo(() => {
    const latest = [...weekChartData]
      .reverse()
      .find((item) => item.beforeScore != null);

    return latest?.dateKey ?? null;
  }, [weekChartData]);

  /* =========================================================
     실제 선택 날짜
  ========================================================= */

  const effectiveSelectedDateKey = useMemo(() => {
    if (selectedDateKey) {
      const exists = weekChartData.some(
        (item) => item.dateKey === selectedDateKey && item.beforeScore != null,
      );

      if (exists) {
        return selectedDateKey;
      }
    }

    return defaultSelectedDateKey;
  }, [selectedDateKey, weekChartData, defaultSelectedDateKey]);

  /* =========================================================
     선택 데이터
  ========================================================= */

  const selectedRecord = useMemo(() => {
    if (!effectiveSelectedDateKey) {
      return null;
    }

    return (
      weekChartData.find((item) => item.dateKey === effectiveSelectedDateKey) ??
      null
    );
  }, [effectiveSelectedDateKey, weekChartData]);

  /* =========================================================
     막대 클릭
  ========================================================= */

  const handleBarClick = (barData) => {
    const item = barData?.payload ?? barData;

    if (!item?.dateKey || item.beforeScore == null) {
      return;
    }

    setSelectedDateKey(item.dateKey);
  };

  /* =========================================================
     변화량
  ========================================================= */

  const selectedDelta =
    selectedRecord?.beforeScore != null && selectedRecord?.afterScore != null
      ? selectedRecord.afterScore - selectedRecord.beforeScore
      : null;

  const getDeltaText = () => {
    if (!selectedRecord) {
      return "";
    }

    if (selectedRecord.status === "SKIPPED") {
      return "추천 건너뜀";
    }

    if (selectedRecord.afterScore == null) {
      return "재측정 없음";
    }

    if (selectedDelta > 0) {
      return `${selectedDelta}점 상승`;
    }

    if (selectedDelta < 0) {
      return `${Math.abs(selectedDelta)}점 하락`;
    }

    return "변화 없음";
  };

  const getDeltaClass = () => {
    if (selectedDelta == null) {
      return "neutral";
    }

    if (selectedDelta > 0) {
      return "positive";
    }

    if (selectedDelta < 0) {
      return "negative";
    }

    return "neutral";
  };

  /* =========================================================
     Render
  ========================================================= */

  return (
    <main className="stats-page">
      {/* =====================================================
          Header
      ===================================================== */}

      <header className="stats-header">
        <div>
          <span className="stats-eyebrow">MOOD REPORT</span>

          <h1>나의 마음 통계</h1>

          <p>기록과 행동을 통해 달라진 마음을 확인해보세요.</p>
        </div>
      </header>

      {/* =====================================================
          월 선택
      ===================================================== */}

      <section className="stats-month-selector">
        <button type="button" onClick={prevMonth} aria-label="이전 달">
          ‹
        </button>

        <h2>
          {year}년 {month}월
        </h2>

        <button
          type="button"
          onClick={nextMonth}
          disabled={isCurrentMonth}
          aria-label="다음 달"
        >
          ›
        </button>
      </section>

      {/* =====================================================
          Tabs
      ===================================================== */}

      <div className="stats-tabs">
        <button
          type="button"
          className={activeTab === "SUMMARY" ? "active" : ""}
          onClick={() => setActiveTab("SUMMARY")}
        >
          요약
        </button>

        <button
          type="button"
          className={activeTab === "TREND" ? "active" : ""}
          onClick={() => setActiveTab("TREND")}
        >
          기분 변화
        </button>

        <button
          type="button"
          className={activeTab === "ACTION" ? "active" : ""}
          onClick={() => setActiveTab("ACTION")}
        >
          행동 효과
        </button>
      </div>

      {/* =====================================================
          SUMMARY
      ===================================================== */}

      {activeTab === "SUMMARY" && monthlyStats && (
        <>
          <section className="monthly-summary">
            <h2>{month}월 요약</h2>

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
                <span>평균 변화량</span>

                <strong>
                  {monthlyStats.averageDelta > 0 ? "+" : ""}
                  {monthlyStats.averageDelta}점
                </strong>
              </div>
            </div>
          </section>

          <div className="summary-links">
            <button type="button" onClick={() => setActiveTab("TREND")}>
              <span>기분 변화 자세히 보기</span>

              <span>→</span>
            </button>

            <button type="button" onClick={() => setActiveTab("ACTION")}>
              <span>행동 효과 자세히 보기</span>

              <span>→</span>
            </button>
          </div>
        </>
      )}

      {/* =====================================================
          TREND
      ===================================================== */}

      {activeTab === "TREND" && (
        <>
          {/* =========================
              선택 월 평균
          ========================= */}

          <section className="trend-average-section">
            <div className="trend-average-card">
              <span>평균 행동 전</span>

              <strong>
                {averageBefore == null ? "—" : `${averageBefore}점`}
              </strong>
            </div>

            <div className="trend-average-divider" />

            <div className="trend-average-card">
              <span>평균 행동 후</span>

              <strong>
                {averageAfter == null ? "—" : `${averageAfter}점`}
              </strong>
            </div>
          </section>

          {loading ? (
            <p className="stats-loading">통계를 불러오는 중...</p>
          ) : !firstDiaryDate ? (
            <section className="stats-empty">
              이 달에는 감정 기록이 없습니다.
            </section>
          ) : (
            <>
              {/* =========================
                  Chart Card
              ========================= */}

              <section className="mood-chart-section">
                <div className="mood-chart-header">
                  <h2>
                    하루의 기분,
                    <br className="mobile-break" />
                    어떻게 달라졌나요?
                  </h2>

                  {activeWeekStartDate && weekEndDate && (
                    <div className="stats-week-selector">
                      <button
                        type="button"
                        onClick={prevWeek}
                        disabled={!canGoPrevWeek}
                        aria-label="이전 7일"
                      >
                        ‹
                      </button>

                      <strong>
                        {formatShortDate(activeWeekStartDate)}

                        {" - "}

                        {formatShortDate(weekEndDate)}
                      </strong>

                      <button
                        type="button"
                        onClick={nextWeek}
                        disabled={!canGoNextWeek}
                        aria-label="다음 7일"
                      >
                        ›
                      </button>
                    </div>
                  )}
                </div>

                {/* =========================
                    Legend
                ========================= */}

                <div className="bar-legend">
                  <span>
                    <i className="legend-before" />
                    행동 전
                  </span>

                  <span>
                    <i className="legend-after" />
                    행동 후
                  </span>
                </div>

                {/* =========================
                    Chart
                ========================= */}

                {chartLoading ? (
                  <p className="stats-loading chart-loading">
                    그래프를 불러오는 중...
                  </p>
                ) : (
                  <>
                    <div className="mood-bar-chart">
                      <ResponsiveContainer width="100%" height={330}>
                        <BarChart
                          data={weekChartData}
                          margin={{
                            top: 28,
                            right: 0,
                            left: -20,
                            bottom: 52,
                          }}
                          barGap={2}
                          barCategoryGap="22%"
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            vertical={false}
                          />

                          <XAxis
                            dataKey="dateKey"
                            interval={0}
                            height={62}
                            tick={<WeekAxisTick dataMap={weekChartDataMap} />}
                            tickLine={false}
                            axisLine={{
                              stroke: "#d8dfda",
                            }}
                          />

                          <YAxis
                            domain={[0, 60]}
                            ticks={[0, 15, 30, 45, 60]}
                            width={36}
                            tick={{
                              fontSize: 10,
                              fill: "#7a857f",
                            }}
                            axisLine={false}
                            tickLine={false}
                          />

                          <Tooltip
                            content={<MoodBarTooltip />}
                            cursor={{
                              fill: "rgba(221, 239, 227, 0.35)",
                            }}
                          />

                          {/* 행동 전 */}

                          <Bar
                            dataKey="beforeScore"
                            name="행동 전"
                            fill="#5f91d8"
                            radius={[4, 4, 0, 0]}
                            maxBarSize={15}
                            onClick={handleBarClick}
                          >
                            <LabelList
                              dataKey="beforeScore"
                              position="top"
                              fontSize={9}
                              fill="#4779bd"
                              formatter={(value) =>
                                value == null ? "" : value
                              }
                            />
                          </Bar>

                          {/* 행동 후 */}

                          <Bar
                            dataKey="afterScore"
                            name="행동 후"
                            fill="#43855f"
                            radius={[4, 4, 0, 0]}
                            maxBarSize={15}
                            onClick={handleBarClick}
                          >
                            <LabelList
                              dataKey="afterScore"
                              position="top"
                              fontSize={9}
                              fill="#34704e"
                              formatter={(value) =>
                                value == null ? "" : value
                              }
                            />
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    </div>

                    <p className="mood-chart-note">
                      기록 없음·스킵한 날의 행동 후 점수는 표시하지 않아요.
                    </p>
                  </>
                )}
              </section>

              {/* =========================
                  상세 카드
              ========================= */}

              {selectedRecord && (
                <section className="trend-detail-card">
                  <h3>
                    {formatDetailDate(selectedRecord.dateKey)}

                    {selectedRecord.actionName
                      ? ` · ${selectedRecord.actionName}`
                      : selectedRecord.emotion
                        ? ` · ${selectedRecord.emotion}`
                        : ""}
                  </h3>

                  <div className="trend-detail-content">
                    <div className="trend-detail-score before">
                      <span>행동 전</span>

                      <strong>{selectedRecord.beforeScore}점</strong>
                    </div>

                    <div className="trend-detail-arrow">→</div>

                    <div className="trend-detail-score after">
                      <span>행동 후</span>

                      <strong>
                        {selectedRecord.afterScore == null
                          ? "—"
                          : `${selectedRecord.afterScore}점`}
                      </strong>
                    </div>

                    <div className={`trend-detail-delta ${getDeltaClass()}`}>
                      {getDeltaText()}
                    </div>
                  </div>
                </section>
              )}
            </>
          )}
        </>
      )}

      {/* =====================================================
          ACTION
      ===================================================== */}

      {activeTab === "ACTION" && (
        <>
          {actionEffects.bestAction ||
          actionEffects.executionRanking.length > 0 ||
          actionEffects.effectRanking.length > 0 ? (
            <section className="action-effect-section">
              <h2>{month}월 행동 효과</h2>

              <p className="action-effect-description">
                이번 달 어떤 행동이 마음에 도움이 되었는지 확인해보세요.
              </p>

              {/* =========================
                  최고 효과
              ========================= */}

              {actionEffects.bestAction && (
                <div className="best-action-card">
                  <div className="best-action-badge">
                    🏆 가장 효과가 좋았던 행동
                  </div>

                  <div className="best-action-content">
                    <div>
                      <span className="best-action-category">
                        {getCategoryEmoji(actionEffects.bestAction.category)}{" "}
                        {getCategoryName(actionEffects.bestAction.category)}
                      </span>

                      <strong>{actionEffects.bestAction.actionName}</strong>
                    </div>

                    <div
                      className={
                        actionEffects.bestAction.delta > 0
                          ? "best-action-delta positive"
                          : actionEffects.bestAction.delta < 0
                            ? "best-action-delta negative"
                            : "best-action-delta neutral"
                      }
                    >
                      {actionEffects.bestAction.delta > 0 ? "+" : ""}
                      {actionEffects.bestAction.delta}점
                    </div>
                  </div>
                </div>
              )}

              {/* =========================
                  실행 순위
              ========================= */}

              <div className="ranking-section">
                <div className="ranking-header">
                  <div>
                    <span className="ranking-eyebrow">MOST ACTIVE</span>

                    <h3>많이 실행한 활동</h3>
                  </div>

                  {actionEffects.executionRanking.length > 3 && (
                    <button
                      type="button"
                      className="ranking-more-button"
                      onClick={() => setRankingModal("EXECUTION")}
                    >
                      전체보기
                      <span>›</span>
                    </button>
                  )}
                </div>

                <div className="ranking-list">
                  {actionEffects.executionRanking
                    .slice(0, 3)
                    .map((item, index) => (
                      <div key={item.category} className="ranking-item">
                        <span className="ranking-number">{index + 1}</span>

                        <span className="ranking-category-emoji">
                          {getCategoryEmoji(item.category)}
                        </span>

                        <div className="ranking-name">
                          <strong>{getCategoryName(item.category)}</strong>

                          <span>{item.category}</span>
                        </div>

                        <strong className="ranking-value">
                          {item.executionCount}회
                        </strong>
                      </div>
                    ))}
                </div>
              </div>

              {/* =========================
                  효과 순위
              ========================= */}

              <div className="ranking-section">
                <div className="ranking-header">
                  <div>
                    <span className="ranking-eyebrow">BEST EFFECT</span>

                    <h3>효과가 좋았던 활동</h3>
                  </div>

                  {actionEffects.effectRanking.length > 3 && (
                    <button
                      type="button"
                      className="ranking-more-button"
                      onClick={() => setRankingModal("EFFECT")}
                    >
                      전체보기
                      <span>›</span>
                    </button>
                  )}
                </div>

                <div className="ranking-list">
                  {actionEffects.effectRanking
                    .slice(0, 3)
                    .map((item, index) => (
                      <div key={item.category} className="ranking-item">
                        <span className="ranking-number">{index + 1}</span>

                        <span className="ranking-category-emoji">
                          {getCategoryEmoji(item.category)}
                        </span>

                        <div className="ranking-name">
                          <strong>{getCategoryName(item.category)}</strong>

                          <span>재측정 {item.sampleCount}회</span>
                        </div>

                        <strong
                          className={
                            item.averageDelta > 0
                              ? "ranking-value positive"
                              : item.averageDelta < 0
                                ? "ranking-value negative"
                                : "ranking-value"
                          }
                        >
                          {item.averageDelta > 0 ? "+" : ""}
                          {item.averageDelta}점
                        </strong>
                      </div>
                    ))}
                </div>
              </div>
            </section>
          ) : (
            <section className="stats-empty action-empty">
              <div className="empty-icon">🌱</div>

              <strong>아직 행동 효과 데이터가 없어요</strong>

              <p>
                추천 행동을 실행하고 기분을 다시 기록하면
                <br />
                어떤 활동이 도움이 되었는지 보여드릴게요.
              </p>
            </section>
          )}
        </>
      )}

      {/* =====================================================
          Ranking Modal
      ===================================================== */}

      {rankingModal && (
        <div
          className="ranking-modal-overlay"
          onClick={() => setRankingModal(null)}
        >
          <section
            className="ranking-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="ranking-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="ranking-modal-header">
              <div>
                <span className="ranking-eyebrow">
                  {rankingModal === "EXECUTION" ? "MOST ACTIVE" : "BEST EFFECT"}
                </span>

                <h2 id="ranking-modal-title">
                  {rankingModal === "EXECUTION"
                    ? `${month}월 활동 전체 순위`
                    : `${month}월 효과 전체 순위`}
                </h2>
              </div>

              <button
                type="button"
                className="ranking-modal-close"
                onClick={() => setRankingModal(null)}
                aria-label="닫기"
              >
                ×
              </button>
            </div>

            <div className="ranking-modal-list">
              {rankingModal === "EXECUTION"
                ? actionEffects.executionRanking.map((item, index) => (
                    <div
                      key={item.category}
                      className="ranking-item ranking-modal-item"
                    >
                      <span className="ranking-number">{index + 1}</span>

                      <span className="ranking-category-emoji">
                        {getCategoryEmoji(item.category)}
                      </span>

                      <div className="ranking-name">
                        <strong>{getCategoryName(item.category)}</strong>

                        <span>{item.category}</span>
                      </div>

                      <strong className="ranking-value">
                        {item.executionCount}회
                      </strong>
                    </div>
                  ))
                : actionEffects.effectRanking.map((item, index) => (
                    <div
                      key={item.category}
                      className="ranking-item ranking-modal-item"
                    >
                      <span className="ranking-number">{index + 1}</span>

                      <span className="ranking-category-emoji">
                        {getCategoryEmoji(item.category)}
                      </span>

                      <div className="ranking-name">
                        <strong>{getCategoryName(item.category)}</strong>

                        <span>재측정 {item.sampleCount}회</span>
                      </div>

                      <strong
                        className={
                          item.averageDelta > 0
                            ? "ranking-value positive"
                            : item.averageDelta < 0
                              ? "ranking-value negative"
                              : "ranking-value"
                        }
                      >
                        {item.averageDelta > 0 ? "+" : ""}
                        {item.averageDelta}점
                      </strong>
                    </div>
                  ))}
            </div>
          </section>
        </div>
      )}
    </main>
  );
}
