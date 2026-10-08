import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { recoverSocialAccount, cancelSocialRecovery } from "../api/authApi";
import "./DislikePage.css";

import "./AccountRecoveryPage.css";

export default function AccountRecoveryPage() {
  const navigate = useNavigate();

  const [recovering, setRecovering] = useState(false);
  const [error, setError] = useState("");

  const handleRecover = async () => {
    try {
      setRecovering(true);
      setError("");

      await recoverSocialAccount();

      navigate("/", {
        replace: true
      });
    } catch (error) {
      console.error(error);

      setError(error.message || "계정 복구에 실패했습니다.");
    } finally {
      setRecovering(false);
    }
  };

  const handleCancel = async () => {
    try {
      await cancelSocialRecovery();
    } catch (error) {
      console.error(error);
    } finally {
      navigate("/login", {
        replace: true
      });
    }
  };

  return (
    <main className="account-recovery-page">
      <section className="account-recovery-card">
        <div className="account-recovery-icon">↻</div>

        <h1>탈퇴 신청된 계정입니다</h1>

        <p>
          현재 회원탈퇴 신청이 진행 중입니다.
          <br />
          다시 Mood&amp;Move를 이용하시겠어요?
        </p>

        {error && <span className="account-recovery-error">{error}</span>}

        <div className="account-recovery-buttons">
          <button
            type="button"
            className="account-recovery-cancel"
            onClick={handleCancel}
            disabled={recovering}
          >
            취소
          </button>

          <button
            type="button"
            className="account-recovery-confirm"
            onClick={handleRecover}
            disabled={recovering}
          >
            {recovering ? "복구 중..." : "계정 복구"}
          </button>
        </div>
      </section>
    </main>
  );
}
