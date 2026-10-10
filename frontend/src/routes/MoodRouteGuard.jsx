import { useEffect, useRef, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { authFetch } from "../api/authApi";

export default function MoodRouteGuard() {
  const navigate = useNavigate();

  const [checking, setChecking] = useState(true);

  const redirectingRef = useRef(false);

  useEffect(() => {
    async function checkMoodAccess() {
      try {
        /*
         * 1. 미재측정 행동 확인
         */
        const executionResponse = await authFetch(
          "/api/action-executions/current",
          {
            method: "GET",
            credentials: "include",
          },
        );

        /*
         * STARTED 행동 존재
         */
        if (executionResponse.status === 200) {
          const execution = await executionResponse.json();

          /*
           * Toast / Navigate 중복 실행 방지
           */
          if (redirectingRef.current) {
            return;
          }

          redirectingRef.current = true;

          navigate(`/action-executions/${execution.executionId}/recheck`, {
            replace: true,
            state: {
              showRecheckToast: true,
            },
          });

          return;
        }

        /*
         * 204 = 미재측정 행동 없음
         */
        if (executionResponse.status !== 204 && !executionResponse.ok) {
          throw new Error("진행 중인 행동 조회 실패");
        }

        /*
         * 2. 오늘 일기 존재 여부 확인
         */
        const todayMoodResponse = await authFetch("/api/moods/today/exists", {
          method: "GET",
          credentials: "include",
        });

        if (!todayMoodResponse.ok) {
          throw new Error("오늘 일기 조회 실패");
        }

        const hasTodayMood = await todayMoodResponse.json();

        if (hasTodayMood) {
          toast("오늘의 일기는 이미 작성했습니다.", {
            icon: "📝",
            id: "today-mood-exists",
          });

          navigate("/", {
            replace: true,
          });

          return;
        }

        /*
         * 3. 작성 가능
         */
        setChecking(false);
      } catch (error) {
        console.error(error);

        toast.error("일기 작성 가능 여부를 확인하지 못했습니다.");

        navigate("/", {
          replace: true,
        });
      }
    }

    checkMoodAccess();
  }, [navigate]);

  if (checking) {
    return null;
  }

  return <Outlet />;
}
