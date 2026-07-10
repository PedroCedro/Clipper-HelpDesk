import type { DiagnoseState, Ticket } from "../types";
import ClipperPanel from "./ClipperPanel";

type TicketDetailProps = {
  ticket: Ticket | null;
  diag: DiagnoseState | undefined;
  onDiagnose: (id: number) => void;
};

const statusChip: Record<string, { label: string; className: string }> = {
  NOVO: { label: "Novo", className: "b-novo" },
  ABERTO: { label: "Aberto", className: "b-novo" },
  EM_ANALISE: { label: "Em análise", className: "b-analise" },
  RESOLVIDO: { label: "Resolvido", className: "b-resolvido" },
};

// Prioridade legível no meta-grid; valor desconhecido passa cru — dado
// novo do backend aparece, mesmo sem tradução bonita.
const prioLabel: Record<string, string> = {
  ALTA: "Alta",
  MEDIA: "Média",
  BAIXA: "Baixa",
};

// Painel de detalhe do ticket selecionado. O meta-grid mostra SÓ os
// campos que existem — célula vazia ou "null" na tela seria só ruído
// (tickets antigos no banco não têm esses campos).
export default function TicketDetail({ ticket, diag, onDiagnose }: TicketDetailProps) {
  if (!ticket) {
    return (
      <section className="detail" aria-label="Detalhe do ticket">
        <p className="detail-empty">Selecione um ticket na fila pra ver o detalhe.</p>
      </section>
    );
  }

  const chip = (ticket.status ? statusChip[ticket.status] : undefined) ?? {
    label: ticket.status ?? "—",
    className: "b-neutro",
  };

  // Monta só os pares presentes; routine chega como "" no intake mínimo
  // e o filtro de falsy já descarta junto com os nulos.
  const meta = [
    ticket.requester ? { label: "Solicitante", value: ticket.requester } : null,
    ticket.routine ? { label: "Rotina", value: ticket.routine } : null,
    ticket.priority
      ? { label: "Prioridade", value: prioLabel[ticket.priority] ?? ticket.priority }
      : null,
    ticket.createdAt
      ? {
          label: "Aberto em",
          value: new Date(ticket.createdAt).toLocaleString("pt-BR", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          }),
        }
      : null,
  ].filter((item): item is { label: string; value: string } => item !== null);

  return (
    <section className="detail" aria-label="Detalhe do ticket">
      <div className="d-eyebrow">Ticket #{ticket.id}</div>
      <h1 className="d-title">{ticket.title}</h1>
      <div className="d-chips">
        <span className={`badge ${chip.className}`}>
          <span className="dot" /> {chip.label}
        </span>
      </div>

      {meta.length > 0 ? (
        <div className="meta-grid">
          {meta.map((item) => (
            <div className="meta-item" key={item.label}>
              <span className="meta-label">{item.label}</span>
              <span className="meta-value">{item.value}</span>
            </div>
          ))}
        </div>
      ) : null}

      <p className="section-label">Descrição do solicitante</p>
      <div className="desc">{ticket.description}</div>

      <ClipperPanel ticket={ticket} diag={diag} onDiagnose={onDiagnose} />
    </section>
  );
}
