import type { DiagnoseState, GroundingState, Ticket } from "../types";

type ClipperPanelProps = {
  ticket: Ticket;
  diag: DiagnoseState | undefined;
  onDiagnose: (id: number) => void;
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

// Painel "Diagnóstico do Clipper": o coração do console. Renderiza o
// resultado pelo grounding ESTRUTURADO (nunca parseando a string source).
export default function ClipperPanel({ ticket, diag, onDiagnose }: ClipperPanelProps) {
  const result = diag?.result;

  return (
    <div className="clipper">
      <div className="clipper-head">
        <span className="clipper-mark">C</span>
        <h3>Diagnóstico do Clipper</h3>
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

            {/* Ações desabilitadas de propósito: os endpoints não existem
                ainda (rodada B3). Botão que não faz nada seria mentira de
                UI — desabilitado diz "existe, mas ainda não". */}
            <div className="clipper-actions">
              <button className="btn-primary" disabled title="Em breve">
                Aplicar como resposta
              </button>
              <button className="btn-ghost" disabled title="Em breve">
                Escalar para humano
              </button>
              <button className="btn-ghost" disabled title="Em breve">
                Marcar diagnóstico incorreto
              </button>
            </div>

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
