import { useEffect, useState } from "react";
import type { DiagnoseState, DiagnosticResult, GroundingState, Ticket, TicketActions } from "../types";

type ClipperPanelProps = {
  ticket: Ticket;
  diag: DiagnoseState | undefined;
  onDiagnose: (id: number) => void;
  actions: TicketActions;
};

// O selo do gate: rótulo + classe visual por estado. Rótulos neutros de
// propósito ("fonte oficial", nunca marca de terceiros na UI). O estado
// APOIADO não existia no mockup (nasceu no backend depois): azul/info é
// o meio-termo entre o verde do curado e o âmbar do sem lastro.
const gateChip: Record<GroundingState, { label: string; className: string }> = {
  ANCORADO: { label: "Ancorado · fonte oficial", className: "g-ancorado" },
  APOIADO: { label: "Apoiado · fonte oficial + IA", className: "g-apoiado" },
  SEM_BASE: { label: "Sem base · somente IA", className: "g-sem-base" },
};

// O texto que "aplicar como resposta" manda pro solicitante: o diagnóstico
// como o técnico viu, com os mesmos títulos do painel.
function composeReply(result: DiagnosticResult): string {
  return `Causa provável:\n${result.probableCause}\n\nPróximos passos:\n${result.nextSteps}`;
}

type ActionKind = "apply" | "escalate" | "feedback";

// Painel "Diagnóstico do Clipper": o coração do console. Renderiza o
// resultado pelo grounding ESTRUTURADO (nunca parseando a string source).
//
// O estado aqui é só o EFÊMERO de UI (qual ação está em voo, form de
// feedback aberto) — o dado que importa (status, response, diagnóstico)
// mora no Console. O TicketDetail remonta o painel por ticket
// (key={ticket.id}), então nada disso vaza de um chamado pro outro.
export default function ClipperPanel({ ticket, diag, onDiagnose, actions }: ClipperPanelProps) {
  const result = diag?.result;

  const [acting, setActing] = useState<ActionKind | null>(null);
  const [note, setNote] = useState<{ kind: "ok" | "erro"; text: string } | null>(null);
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [feedbackSent, setFeedbackSent] = useState(false);

  // O feedback pertence à RODADA de diagnóstico, não ao ticket (a B3
  // aceita um feedback por rodada). O key do TicketDetail zera o painel
  // ao trocar de chamado, mas rediagnosticar mantém o ticket — quem
  // sinaliza "rodada nova" é o `result` trocando de referência. Aí o
  // painel volta a aceitar feedback e limpa as notas da rodada anterior.
  useEffect(() => {
    setFeedbackSent(false);
    setFeedbackOpen(false);
    setReason("");
    setNote(null);
  }, [result]);

  const busy = acting !== null || diag?.loading === true;

  // Executor comum das três ações: trava os botões, traduz sucesso/erro
  // em nota visível e destrava — o padrão é um só pra não divergir.
  async function run(kind: ActionKind, action: () => Promise<void>, okText: string): Promise<boolean> {
    setActing(kind);
    setNote(null);
    try {
      await action();
      setNote({ kind: "ok", text: okText });
      return true;
    } catch (err) {
      setNote({ kind: "erro", text: err instanceof Error ? err.message : "Falha inesperada na ação." });
      return false;
    } finally {
      setActing(null);
    }
  }

  async function handleFeedback() {
    // reason vazio vira null: o backend trata a ausência como legítima —
    // feedback sem justificativa ainda alimenta a curadoria.
    const sent = await run(
      "feedback",
      () => actions.onFlagIncorrect(ticket.id, reason.trim() || null),
      "Feedback registrado — obrigado, isso alimenta a curadoria.",
    );
    if (sent) {
      setFeedbackSent(true);
      setFeedbackOpen(false);
    }
  }

  return (
    <div className="clipper">
      <div className="clipper-head">
        <span className="clipper-mark">C</span>
        <h3>Diagnóstico do Clipper</h3>
        {result ? (
          <button
            className="btn-ghost btn-small"
            onClick={() => onDiagnose(ticket.id)}
            disabled={busy}
          >
            {diag?.loading ? "Rediagnosticando…" : "Rediagnosticar"}
          </button>
        ) : null}
      </div>

      <div className="clipper-body">
        {!result ? (
          <div className="clipper-cta">
            <button
              className="btn-primary"
              onClick={() => onDiagnose(ticket.id)}
              disabled={diag?.loading}
            >
              {diag?.loading ? "Diagnosticando…" : "Diagnosticar com o Clipper"}
            </button>
            {diag?.error ? <p className="clipper-error">{diag.error}</p> : null}
          </div>
        ) : (
          <>
            <div className={`grounding ${gateChip[result.grounding.state].className}`}>
              <span className="src-chip">
                <span className="dot" /> {gateChip[result.grounding.state].label}
              </span>
              {result.grounding.articleTitle ? (
                result.grounding.articleUrl ? (
                  <a
                    className="src-link"
                    href={result.grounding.articleUrl}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {result.grounding.articleTitle}
                  </a>
                ) : (
                  <span className="src-link">{result.grounding.articleTitle}</span>
                )
              ) : null}
              {result.grounding.model ? (
                <span className="src-model">via {result.grounding.model}</span>
              ) : null}
              <span className="conf">
                <span className="conf-bar">
                  <span
                    className="conf-fill"
                    style={{ width: `${Math.round(result.confidence * 100)}%` }}
                  />
                </span>
                <span className="conf-num">{Math.round(result.confidence * 100)}%</span>
              </span>
            </div>

            <div className="diag-block">
              <h4>Causa provável</h4>
              <p>{result.probableCause}</p>
            </div>

            <div className="diag-block">
              <h4>Próximos passos</h4>
              {/* pre-line: conteúdo curado vem com quebras (Sintoma/Causa/Passos). */}
              <p className="pre">{result.nextSteps}</p>
            </div>

            <div className="clipper-actions">
              <button
                className="btn-primary"
                disabled={busy}
                onClick={() =>
                  run(
                    "apply",
                    () => actions.onApplyResponse(ticket.id, composeReply(result)),
                    "Resposta aplicada — chamado resolvido.",
                  )
                }
              >
                {acting === "apply" ? "Aplicando…" : "Aplicar como resposta"}
              </button>
              <button
                className="btn-ghost"
                disabled={busy}
                onClick={() =>
                  run(
                    "escalate",
                    () => actions.onEscalate(ticket.id),
                    "Chamado escalado para atendimento humano.",
                  )
                }
              >
                {acting === "escalate" ? "Escalando…" : "Escalar para humano"}
              </button>
              {feedbackSent ? (
                // Vira texto, não botão desabilitado: a ação já aconteceu,
                // não está indisponível.
                <span className="feedback-done">✓ Feedback registrado</span>
              ) : (
                <button
                  className="btn-ghost"
                  disabled={busy}
                  onClick={() => setFeedbackOpen((v) => !v)}
                >
                  Marcar diagnóstico incorreto
                </button>
              )}
            </div>

            {feedbackOpen && !feedbackSent ? (
              <div className="feedback-form">
                <textarea
                  rows={2}
                  placeholder="Por que está errado? (opcional — é o dado mais valioso pra curadoria)"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
                <div className="feedback-form-actions">
                  <button className="btn-primary" disabled={busy} onClick={handleFeedback}>
                    {acting === "feedback" ? "Enviando…" : "Enviar feedback"}
                  </button>
                  <button className="btn-ghost" disabled={busy} onClick={() => setFeedbackOpen(false)}>
                    Cancelar
                  </button>
                </div>
              </div>
            ) : null}

            {note ? <p className={`action-note ${note.kind}`}>{note.text}</p> : null}

            <div className="gate-note">
              <strong>Como o Clipper decide a fonte.</strong> Com artigo oficial que
              casa forte com o chamado, a resposta vem ancorada, sem IA. Com indício
              parcial, a IA responde apoiada no artigo. Sem base, ele avisa em vez de
              chutar — e a confiança tem teto.
              <div className="gate-legend">
                <span>
                  <span className="legend-ok">●</span> Ancorado — documentação oficial
                </span>
                <span>
                  <span className="legend-info">●</span> Apoiado — IA com fonte oficial
                </span>
                <span>
                  <span className="legend-warn">●</span> Sem base — hipótese técnica
                </span>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
