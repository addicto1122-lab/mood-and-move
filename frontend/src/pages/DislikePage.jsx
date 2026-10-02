import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  getDislikeActions,
  addDislikeAction,
  removeDislikeAction
} from "../api/authApi";

import "./DislikePage.css";

export default function DislikePage() {
  const navigate = useNavigate();

  const [toast, setToast] = useState("");
  const [actions, setActions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [error, setError] = useState("");

  const showToast = (message) => {
    setToast(message);

    setTimeout(() => {
      setToast("");
    }, 2000);
  };

  useEffect(() => {
    async function loadActions() {
      try {
        const data = await getDislikeActions();

        setActions(data);
      } catch (error) {
        console.error(error);

        setError(error.message || "비선호 행동 목록을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }

    loadActions();
  }, []);

  const handleToggle = async (action) => {
    try {
      setUpdatingId(action.actionId);
      setError("");

      if (action.disliked) {
        await removeDislikeAction(action.actionId);

        showToast("비선호 행동에서 해제되었습니다.");
      } else {
        await addDislikeAction(action.actionId);

        showToast("비선호 행동으로 등록되었습니다.");
      }

      setActions((prev) =>
        prev.map((item) =>
          item.actionId === action.actionId
            ? {
                ...item,
                disliked: !item.disliked
              }
            : item
        )
      );
    } catch (error) {
      console.error(error);

      setError(error.message || "비선호 행동 변경에 실패했습니다.");
    } finally {
      setUpdatingId(null);
    }
  };

  if (loading) {
    return (
      <main className="dislike-page">
        <div className="dislike-loading">불러오는 중...</div>
      </main>
    );
  }

  return (
    <main className="dislike-page">
      <section className="dislike-container">
        <header className="dislike-header">
          <button
            type="button"
            className="dislike-back-button"
            onClick={() => navigate("/mypage")}
            aria-label="마이페이지로 돌아가기"
          >
            ‹
          </button>

          <div>
            <h1>비선호 행동 관리</h1>

            <p>추천에서 제외하고 싶은 행동을 선택해주세요.</p>
          </div>
        </header>

        {error && <div className="dislike-error">{error}</div>}

        {actions.length === 0 ? (
          <div className="dislike-empty">등록된 행동이 없습니다.</div>
        ) : (
          <section className="dislike-card">
            {actions.map((action, index) => (
              <div key={action.actionId}>
                <button
                  type="button"
                  className={
                    action.disliked ? "dislike-item selected" : "dislike-item"
                  }
                  disabled={updatingId === action.actionId}
                  onClick={() => handleToggle(action)}
                >
                  <div className="dislike-item-content">
                    <div className="dislike-item-title">{action.name}</div>

                    <div className="dislike-item-meta">
                      <span>{action.category}</span>

                      <span>·</span>

                      <span>{action.durationMinutes}분</span>
                    </div>
                  </div>

                  <div className="dislike-check">
                    {updatingId === action.actionId
                      ? "..."
                      : action.disliked
                        ? "✓"
                        : ""}
                  </div>
                </button>

                {index < actions.length - 1 && (
                  <div className="dislike-divider" />
                )}
              </div>
            ))}
          </section>
        )}

        <p className="dislike-note">
          선택한 행동은 이후 추천에서 제외할 수 있어요.
        </p>
      </section>

      {toast && <div className="dislike-toast">{toast}</div>}
    </main>
  );
}
