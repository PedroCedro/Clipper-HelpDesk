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

type ClipperWidgetProps = {
  health: ApiHealth | null;
  tickets: Ticket[];
  isLoading: boolean;
  error: string | null;
};

const statusLabel: Record<string, string> = {
  NOVO: "Novo",
  ABERTO: "Aberto",
  EM_ANALISE: "Em analise",
  RESOLVIDO: "Resolvido",
};

export default function ClipperWidget({
  health,
  tickets,
  isLoading,
  error,
}: ClipperWidgetProps) {
  const isActive = health?.status === "ATIVO";

  return (
    <section className="widget">
      <div className="widget-header">
        <div>
          <p className="widget-kicker">Central do Clipper</p>
          <h2>Triagem automatizada em tempo real</h2>
        </div>
        <div className={`widget-pill ${isActive ? "is-active" : "is-idle"}`}>
          <span className="dot" />
          <span>{isActive ? "API ativa" : "API indisponivel"}</span>
        </div>
      </div>

      <p className="widget-copy">
        O painel abaixo mostra o estado da API e os tickets que ja estao
        prontos para a primeira leitura automatizada.
      </p>

      <div className="widget-grid">
        <article className="metric-card">
          <span className="metric-label">Servico</span>
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
          <p className="ticket-empty">Nenhum ticket disponivel no momento.</p>
        ) : (
          tickets.map((ticket) => (
            <article className="ticket-card" key={ticket.id}>
              <div className="ticket-card-header">
                <h4>{ticket.title}</h4>
                <span className="ticket-badge">
                  {statusLabel[ticket.status] ?? ticket.status}
                </span>
              </div>
              <p>{ticket.description}</p>
            </article>
          ))
        )}
      </div>
    </section>
  );
}
