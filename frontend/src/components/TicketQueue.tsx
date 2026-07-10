import type { GroundingState, Ticket } from "../types";

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

// Tag de IA da fila: versão curta do selo do gate (o rótulo completo
// vive no painel do Clipper). Mesmo contrato de lá: estado novo no
// backend obriga o TypeScript a apontar este mapa incompleto.
const aiTag: Record<GroundingState, { label: string; className: string }> = {
  ANCORADO: { label: "Ancorado", className: "b-ancorado" },
  APOIADO: { label: "Apoiado", className: "b-apoiado" },
  SEM_BASE: { label: "Sem base", className: "b-sem-base" },
};

// Barra de prioridade (a coluna de 4px): cor por urgência. Prioridade
// nula ou desconhecida fica invisível — ausência não é alarme.
const prioBar: Record<string, string> = {
  ALTA: "p-alta",
  MEDIA: "p-media",
  BAIXA: "p-baixa",
};

// "aberto há X": tempo relativo curto pra triagem visual. Calculado no
// render — precisão de minuto basta numa fila que recarrega ao navegar.
function openSince(createdAt: string): string {
  const mins = Math.floor((Date.now() - new Date(createdAt).getTime()) / 60_000);
  if (mins < 1) return "agora";
  if (mins < 60) return `há ${mins} min`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `há ${hours} h`;
  return `há ${Math.floor(hours / 24)} d`;
}

// Fila master-detail: cada linha é um <button> (semântica de clique e
// foco por teclado de graça).
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
          const badge = (ticket.status ? statusBadge[ticket.status] : undefined) ?? {
            label: ticket.status ?? "—",
            className: "b-neutro",
          };
          const tag = ticket.diagnosis ? aiTag[ticket.diagnosis.state] : null;
          return (
            <button
              key={ticket.id}
              className={`queue-row${ticket.id === selectedId ? " sel" : ""}`}
              onClick={() => onSelect(ticket.id)}
            >
              <span
                className={`prio ${(ticket.priority && prioBar[ticket.priority]) || ""}`}
              />
              <span className="tid">#{ticket.id}</span>
              <span className="q-main">
                <span className="q-title">{ticket.title}</span>
                <span className="q-sub">{ticket.description}</span>
              </span>
              <span className="q-right">
                <span className={`badge ${badge.className}`}>
                  <span className="dot" /> {badge.label}
                </span>
                {tag && ticket.diagnosis ? (
                  // A confiança fica no title (hover): na fila o que
                  // decide a triagem é o ESTADO; o número vive no painel.
                  <span
                    className={`badge ${tag.className}`}
                    title={`Confiança ${Math.round(ticket.diagnosis.confidence * 100)}%`}
                  >
                    <span className="dot" /> {tag.label}
                  </span>
                ) : null}
                {ticket.createdAt ? (
                  <span className="q-time">{openSince(ticket.createdAt)}</span>
                ) : null}
              </span>
            </button>
          );
        })
      )}
    </section>
  );
}
