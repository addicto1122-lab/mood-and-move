import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { getHome } from "../api/homeApi";

export default function MoodRouteGuard() {
  const navigate = useNavigate();
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    async function checkTodayMood() {
      try {
        const data = await getHome();

        const now = new Date();

        const today = [
          now.getFullYear(),
          String(now.getMonth() + 1).padStart(2, "0"),
          String(now.getDate()).padStart(2, "0")
        ].join("-");

        if (data.latestMood?.entryDate === today) {
          alert("오늘의 일기는 이미 작성했습니다.");

          navigate("/", {
            replace: true
          });

          return;
        }

        setChecking(false);
      } catch (error) {
        console.error(error);

        alert("오늘의 기록 여부를 확인하지 못했습니다.");

        navigate("/", {
          replace: true
        });
      }
    }

    checkTodayMood();
  }, [navigate]);

  if (checking) {
    return null;
  }

  return <Outlet />;
}
