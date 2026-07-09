import { useState } from "react";

type ApiHealth = {
  status: string;
  service: string;
};

type Ticket = {
  id: number;
  title: string;
  description: string;
  status: string;
};

// Os três estados do gate de grounding — espelham o enum Grounding.State do
// backend. É contrato: se o backend ganhar um estado novo, o TypeScript
// aponta os switches/mapas incompletos aqui.
type GroundingState = "ANCORADO" | "APOIADO" | "SEM_BASE";

type Grounding = {
  state: GroundingState;
  articleTitle: string | null;
  articleUrl: string | null;
  model: string | null;
};

type DiagnosticResult = {
  probableCause: string;
  nextSteps: string;
  confidence: number;
  // String legível ("ancorado: ...") — humano/log. A UI decide pelo
  // grounding estruturado, nunca parseando esta string.
  source: string;
  grounding: Grounding;
};

type DiagnoseState = {
  loading: boolean;
  result?: DiagnosticResult;
  error?: string;
};

type ClipperWidgetProps = {
  health: ApiHealth | null;
  tickets: Ticket[];
  isLoading: boolean;
  error: string | null;
};

const statusLabel: Record<string, string> = {
  NOVO: "Novo",
  ABERTO: "Aberto",
  EM_ANALISE: "Em análise",
  RESOLVIDO: "Resolvido",
};

// O selo do gate: rótulo + tom visual por estado. Rótulos neutros de
// propósito ("fonte oficial", nunca nome de marca de terceiros na UI).
const gateBadge: Record<GroundingState, { label: string; className: string }> = {
  ANCORADO: { label: "Ancorado · fonte oficial", className: "is-anchored" },
  APOIADO: { label: "Apoiado · fonte oficial + IA", className: "is-supported" },
  SEM_BASE: { label: "Sem base · somente IA", className: "is-ungrounded" },
};

export default function ClipperWidget({
  health,
  tickets,
  isLoading,
  error,
}: ClipperWidgetProps) {
  const isActive = health?.status === "ATIVO";
  const [diagnoses, setDiagnoses] = useState<Record<number, DiagnoseState>>({});

  async function handleDiagnose(id: number) {
    setDiagnoses((prev) => ({ ...prev, [id]: { loading: true } }));
    try {
      const response = await fetch(`/api/tickets/${id}/diagnose`, {
        method: "POST",
      });
      if (!response.ok) {
        throw new Error(`O Clipper não conseguiu diagnosticar (HTTP ${response.status}).`);
      }
      const data = (await response.json()) as { diagnosis: DiagnosticResult };
      setDiagnoses((prev) => ({
        ...prev,
        [id]: { loading: false, result: data.diagnosis },
      }));
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Falha inesperada no diagnóstico.";
      setDiagnoses((prev) => ({ ...prev, [id]: { loading: false, error: message } }));
    }
  }

  return (
    <section className="widget">
      <div className="widget-header">
        <div>
          <p className="widget-kicker">Central do Clipper</p>
          <h2>Triagem automatizada em tempo real</h2>
        </div>
        <div className={`widget-pill ${isActive ? "is-active" : "is-idle"}`}>
          <span className="dot" />
          <span>{isActive ? "API ativa" : "API indisponível"}</span>
        </div>
      </div>

      <p className="widget-copy">
        O painel abaixo mostra o estado da API e os tickets que já estão
        prontos para a primeira leitura automatizada.
      </p>

      <div className="widget-grid">
        <article className="metric-card">
          <span className="metric-label">Serviço</span>
          <strong>{health?.service ?? "Aguardando resposta"}</strong>
        </article>
        <article className="metric-card">
          <span className="metric-label">Tickets recebidos</span>
          <strong>{isLoading ? "..." : tickets.length}</strong>
        </article>
      </div>

      {error ? <p className="widget-error">{error}</p> : null}

      <div className="ticket-list">
        <div className="ticket-list-header">
          <h3>Fila inicial</h3>
          <span>{isLoading ? "Carregando" : `${tickets.length} itens`}</span>
        </div>

        {isLoading ? (
          <p className="ticket-empty">Buscando tickets no backend...</p>
        ) : tickets.length === 0 ? (
          <p className="ticket-empty">Nenhum ticket disponível no momento.</p>
        ) : (
          tickets.map((ticket) => {
            const diag = diagnoses[ticket.id];
            return (
              <article className="ticket-card" key={ticket.id}>
                <div className="ticket-card-header">
                  <h4>{ticket.title}</h4>
                  <span className="ticket-badge">
                    {statusLabel[ticket.status] ?? ticket.status}
                  </span>
                </div>
                <p>{ticket.description}</p>

                <button
                  className="diagnose-button"
                  onClick={() => handleDiagnose(ticket.id)}
                  disabled={diag?.loading}
                >
                  {diag?.loading
                    ? "Diagnosticando..."
                    : "Diagnosticar com o Clipper"}
                </button>

                {diag?.error ? (
                  <p className="diagnose-error">{diag.error}</p>
                ) : null}

                {diag?.result ? (
                  <div className="diagnose-result">
                    {/* Selo do gate + confiança: a primeira coisa que o técnico
                        lê é DE ONDE veio a resposta e quanto ela vale. */}
                    <div className="diagnose-gate">
                      <span
                        className={`gate-badge ${gateBadge[diag.result.grounding.state].className}`}
                      >
                        {gateBadge[diag.result.grounding.state].label}
                      </span>
                      <span className="gate-confidence">
                        {Math.round(diag.result.confidence * 100)}%
                      </span>
                    </div>
                    <div className="confidence-bar" aria-hidden="true">
                      <div
                        className="confidence-fill"
                        style={{ width: `${Math.round(diag.result.confidence * 100)}%` }}
                      />
                    </div>

                    <p className="diagnose-line">
                      <strong>Causa provável:</strong> {diag.result.probableCause}
                    </p>
                    {/* pre-line: o conteúdo curado vem com quebras de linha
                        (Sintoma / Causa / Passos) e elas devem aparecer. */}
                    <p className="diagnose-line diagnose-steps">
                      <strong>Próximos passos:</strong> {diag.result.nextSteps}
                    </p>

                    <p className="diagnose-meta">
                      {diag.result.grounding.articleTitle ? (
                        <>
                          Base: {diag.result.grounding.articleTitle}
                          {diag.result.grounding.articleUrl ? (
                            <>
                              {" · "}
                              <a
                                className="gate-source-link"
                                href={diag.result.grounding.articleUrl}
                                target="_blank"
                                rel="noreferrer"
                              >
                                ver fonte oficial
                              </a>
                            </>
                          ) : null}
                        </>
                      ) : (
                        "Sem artigo de base para este chamado"
                      )}
                      {diag.result.grounding.model
                        ? ` · via ${diag.result.grounding.model}`
                        : null}
                    </p>
                  </div>
                ) : null}
              </article>
            );
          })
        )}
      </div>
    </section>
  );
}
