import CatalogSearch from "./CatalogSearch";
import type { CurationCaseDetail as Detail } from "../types";

type Props = {
  detail: Detail | null;
  loading: boolean;
  actionError: string | null;
  onAdd: (documentId: number, suggestedReason: string) => void;
  onRemove: (documentId: number) => void;
  onDiscard: () => void;
};

export default function CurationDetail({
  detail,
  loading,
  actionError,
  onAdd,
  onRemove,
  onDiscard,
}: Props) {
  if (loading) return <div className="detail-empty">Carregando caso…</div>;
  if (!detail) return <div className="detail-empty">Selecione um caso para investigar.</div>;
  const disabled = detail.status === "DESCARTADO";
  const linkedIds = new Set(detail.candidates.map((candidate) => candidate.documentId));

  return (
    <div className="curation-detail">
      <header className="curation-detail-head">
        <div>
          <span className="d-eyebrow">Caso C-{detail.id}</span>
          <h2>{detail.reason}</h2>
          <p>{detail.originType === "MANUAL" ? "Origem manual" : detail.originReference}</p>
        </div>
        <button className="btn-danger" disabled={disabled} onClick={onDiscard}>
          {disabled ? "Descartado" : "Descartar caso"}
        </button>
      </header>

      {actionError ? <p className="curation-error">{actionError}</p> : null}

      <div className="curation-metrics">
        <span><strong>{detail.candidateCount}</strong> fontes ativas</span>
        <span><strong>{detail.transitions.length}</strong> mudanças de etapa</span>
        <span><strong>{detail.candidateEvents.length}</strong> eventos auditados</span>
      </div>

      <section className="curation-card">
        <h3>Fontes candidatas</h3>
        {detail.candidates.length === 0 ? <p className="muted">Nenhuma fonte associada.</p> : null}
        {detail.candidates.map((candidate) => (
          <div className="candidate-row" key={candidate.id}>
            <div>
              <strong>{candidate.title}</strong>
              <span>{candidate.sourceLabel}{candidate.module ? ` · ${candidate.module}` : ""}</span>
              <span>{candidate.reason} · por {candidate.author}</span>
            </div>
            <div className="candidate-actions">
              {candidate.sourceUrl ? <a href={candidate.sourceUrl} target="_blank" rel="noreferrer">Abrir fonte</a> : null}
              <button className="btn-link-danger" disabled={disabled} onClick={() => onRemove(candidate.documentId)}>Remover</button>
            </div>
          </div>
        ))}
      </section>

      <CatalogSearch linkedIds={linkedIds} disabled={disabled} onAdd={onAdd} />

      <section className="curation-card">
        <h3>Histórico do caso</h3>
        <div className="timeline">
          {detail.transitions.map((transition) => (
            <div className="timeline-item" key={transition.id}>
              <span className="timeline-dot" />
              <div>
                <strong>{transition.fromStatus ?? "Criação"} → {transition.toStatus}</strong>
                <p>{transition.reason}</p>
                <small>{transition.actor} · {new Date(transition.createdAt).toLocaleString("pt-BR")}</small>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
