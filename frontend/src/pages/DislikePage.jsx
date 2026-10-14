import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getDislikeActions, removeDislikeAction } from "../api/authApi";

import "./DislikePage.css";

import { formatDuration } from "../utils/formatDuration";

export default function DislikePage() {
  const navigate = useNavigate();

  const [toast, setToast] = useState("");
  const [actions, setActions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState("");

  /*
   * =========================
   * Toast
   * =========================
   */
  const showToast = (message) => {
    setToast(message);

    setTimeout(() => {
      setToast("");
    }, 2000);
  };

  /*
   * =========================
   * 비선호 행동 조회
   * =========================
   */
  useEffect(() => {
    let cancelled = false;

    async function loadActions() {
      try {
        setLoading(true);
        setError("");

        const data = await getDislikeActions();

        if (cancelled) {
          return;
        }

        /*
         * 실제 비선호 행동만 표시
         */
        const dislikedActions = (data ?? []).filter(
          (action) => action.disliked !== false,
        );

        setActions(dislikedActions);
      } catch (error) {
        console.error("비선호 행동 조회 실패:", error);

        if (!cancelled) {
          setError(error.message || "비선호 행동 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadActions();

    return () => {
      cancelled = true;
    };
  }, []);

  /*
   * =========================
   * 비선호 행동 삭제
   * =========================
   */
  const handleRemove = async (action) => {
    if (deletingId != null) {
      return;
    }

    const actionName = action.actionName ?? action.name ?? "이 행동";

    /*
     * 삭제 확인
     */
    const confirmed = window.confirm(
      `"${actionName}"을(를) 비선호 행동에서 정말 삭제하시겠습니까?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      setDeletingId(action.actionId);

      setError("");

      /*
       * DB DELETE
       */
      await removeDislikeAction(action.actionId);

      /*
       * 삭제 성공 시
       * 화면에서도 즉시 제거
       */
      setActions((prev) =>
        prev.filter((item) => item.actionId !== action.actionId),
      );

      showToast("비선호 행동에서 삭제되었습니다.");
    } catch (error) {
      console.error("비선호 행동 삭제 실패:", error);

      setError(error.message || "비선호 행동 삭제에 실패했습니다.");
    } finally {
      setDeletingId(null);
    }
  };

  /*
   * =========================
   * Loading
   * =========================
   */
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
        {/* =========================
            Header
        ========================= */}
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

            <p>다시 추천받아도 괜찮은 행동은 목록에서 삭제해주세요.</p>
          </div>
        </header>

        {/* Error */}
        {error && <div className="dislike-error">{error}</div>}

        {/* =========================
            Empty / List
        ========================= */}
        {actions.length === 0 ? (
          <div className="dislike-empty">등록된 비선호 행동이 없습니다.</div>
        ) : (
          <section className="dislike-card">
            {actions.map((action, index) => {
              /*
               * API DTO에 따라
               * actionName / name 대응
               */
              const actionName = action.actionName ?? action.name ?? "행동";

              /*
               * 중요
               *
               * 카테고리 이모지를 사용하지 않고
               * recommendations 테이블에서
               * 내려온 추천 당시 emoji를 사용한다.
               */
              const emoji = action.emoji?.trim() || "✨";

              return (
                <div key={action.actionId}>
                  <div className="dislike-item">
                    {/* 추천 행동 이모지 */}
                    <div className="dislike-item-emoji">{emoji}</div>

                    {/* 행동 정보 */}
                    <div className="dislike-item-content">
                      <div className="dislike-item-title">{actionName}</div>

                      <div className="dislike-item-meta">
                        {action.category && <span>{action.category}</span>}

                        {action.category && <span>·</span>}

                        <span>{formatDuration(action)}</span>
                      </div>
                    </div>

                    {/* 삭제 버튼 */}
                    <button
                      type="button"
                      className="dislike-delete-button"
                      disabled={deletingId === action.actionId}
                      onClick={() => handleRemove(action)}
                      aria-label={`${actionName} 비선호 삭제`}
                    >
                      {deletingId === action.actionId ? "..." : "×"}
                    </button>
                  </div>

                  {index < actions.length - 1 && (
                    <div className="dislike-divider" />
                  )}
                </div>
              );
            })}
          </section>
        )}

        <p className="dislike-note">
          삭제한 행동은 이후 추천에서 다시 나타날 수 있어요.
        </p>
      </section>

      {/* Toast */}
      {toast && <div className="dislike-toast">{toast}</div>}
    </main>
  );
}
