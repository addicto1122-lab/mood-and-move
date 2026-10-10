// import { useState } from "react";
// import { NavLink, useNavigate } from "react-router-dom";
// import { getHome } from "../api/homeApi";
import { NavLink } from "react-router-dom";

export default function RecordNavLink() {
  // const navigate = useNavigate();
  // const [checking, setChecking] = useState(false);

  // const handleClick = async (e) => {
  //   e.preventDefault();

  //   if (checking) {
  //     return;
  //   }

  //   try {
  //     setChecking(true);

  //     const data = await getHome();

  //     const now = new Date();

  //     const today = [
  //       now.getFullYear(),
  //       String(now.getMonth() + 1).padStart(2, "0"),
  //       String(now.getDate()).padStart(2, "0")
  //     ].join("-");

  //     if (data.latestMood?.entryDate === today) {
  //       alert("오늘의 일기는 이미 작성했습니다.");
  //       return;
  //     }

  //     navigate("/mood");
  //   } catch (error) {
  //     console.error(error);

  //     alert("오늘의 기록 여부를 확인하지 못했습니다.");
  //   } finally {
  //     setChecking(false);
  //   }
  // };

  return <NavLink to="/mood">기록</NavLink>;
}
