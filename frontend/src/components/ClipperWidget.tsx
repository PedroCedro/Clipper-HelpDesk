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

type DiagnosticResult = {
  probableCause: string;
  nextSteps: string;
  confidence: number;
  source: string;
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
                    <p className="diagnose-line">
                      <strong>Causa provável:</strong> {diag.result.probableCause}
                    </p>
                    <p className="diagnose-line">
                      <strong>Próximos passos:</strong> {diag.result.nextSteps}
                    </p>
                    <p className="diagnose-meta">
                      Confiança: {Math.round(diag.result.confidence * 100)}% · Fonte:{" "}
                      {diag.result.source}
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
