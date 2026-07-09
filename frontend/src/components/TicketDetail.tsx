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

// Painel de detalhe do ticket selecionado. O meta-grid do mockup
// (solicitante/filial/rotina/responsável) entra na F2, quando o Ticket
// tiver esses campos — placeholder vazio aqui seria só ruído.
export default function TicketDetail({ ticket, diag, onDiagnose }: TicketDetailProps) {
  if (!ticket) {
    return (
      <section className="detail" aria-label="Detalhe do ticket">
        <p className="detail-empty">Selecione um ticket na fila pra ver o detalhe.</p>
      </section>
    );
  }

  const chip = statusChip[ticket.status] ?? { label: ticket.status, className: "b-neutro" };

  return (
    <section className="detail" aria-label="Detalhe do ticket">
      <div className="d-eyebrow">Ticket #{ticket.id}</div>
      <h1 className="d-title">{ticket.title}</h1>
      <div className="d-chips">
        <span className={`badge ${chip.className}`}>
          <span className="dot" /> {chip.label}
        </span>
      </div>

      <p className="section-label">Descrição do solicitante</p>
      <div className="desc">{ticket.description}</div>

      <ClipperPanel ticket={ticket} diag={diag} onDiagnose={onDiagnose} />
    </section>
  );
}
