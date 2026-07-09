import type { Ticket } from "../types";

type TicketQueueProps = {
  tickets: Ticket[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  isLoading: boolean;
  error: string | null;
};

// Mapa status → rótulo + tom. Status desconhecido cai no neutro em vez
// de quebrar — o backend pode ganhar status novos antes do front.
const statusBadge: Record<string, { label: string; className: string }> = {
  NOVO: { label: "Novo", className: "b-novo" },
  ABERTO: { label: "Aberto", className: "b-novo" },
  EM_ANALISE: { label: "Em análise", className: "b-analise" },
  RESOLVIDO: { label: "Resolvido", className: "b-resolvido" },
};

// Fila master-detail: cada linha é um <button> (semântica de clique e
// foco por teclado de graça). A coluna vazia de 4px é a vaga da barra
// de prioridade — entra na F2, quando o Ticket tiver priority.
export default function TicketQueue({
  tickets,
  selectedId,
  onSelect,
  isLoading,
  error,
}: TicketQueueProps) {
  if (error) {
    return (
      <section className="queue" aria-label="Fila de tickets">
        <p className="queue-error">{error}</p>
      </section>
    );
  }

  return (
    <section className="queue" aria-label="Fila de tickets">
      {isLoading ? (
        <p className="queue-empty">Buscando tickets no backend…</p>
      ) : tickets.length === 0 ? (
        <p className="queue-empty">
          Nenhum ticket na fila. Crie o primeiro em “＋ Novo ticket”.
        </p>
      ) : (
        tickets.map((ticket) => {
          const badge = statusBadge[ticket.status] ?? {
            label: ticket.status,
            className: "b-neutro",
          };
          return (
            <button
              key={ticket.id}
              className={`queue-row${ticket.id === selectedId ? " sel" : ""}`}
              onClick={() => onSelect(ticket.id)}
            >
              <span />
              <span className="tid">#{ticket.id}</span>
              <span className="q-main">
                <span className="q-title">{ticket.title}</span>
                <span className="q-sub">{ticket.description}</span>
              </span>
              <span className="q-right">
                <span className={`badge ${badge.className}`}>
                  <span className="dot" /> {badge.label}
                </span>
              </span>
            </button>
          );
        })
      )}
    </section>
  );
}
