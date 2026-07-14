import type { CurationCaseSummary, CurationStatus } from "../types";

const statusLabel: Record<CurationStatus, string> = {
  ABERTO: "Aberto",
  COM_CANDIDATOS: "Com candidatos",
  RASCUNHO: "Rascunho",
  EM_REVISAO: "Em revisão",
  PUBLICADO: "Publicado",
  DESCARTADO: "Descartado",
};

type Props = {
  cases: CurationCaseSummary[];
  selectedId: number | null;
  loading: boolean;
  error: string | null;
  onSelect: (id: number) => void;
};

export default function CurationQueue({ cases, selectedId, loading, error, onSelect }: Props) {
  if (loading) return <div className="queue-empty">Carregando casos de curadoria…</div>;
  if (error) return <div className="queue-error">{error}</div>;
  if (cases.length === 0) return <div className="queue-empty">Nenhum caso neste filtro.</div>;

  return (
    <div className="curation-list">
      {cases.map((item) => (
        <button
          key={item.id}
          className={`curation-row ${item.id === selectedId ? "sel" : ""}`}
          onClick={() => onSelect(item.id)}
        >
          <span className="tid">C-{item.id}</span>
          <span className="q-main">
            <strong className="q-title">{item.reason}</strong>
            <span className="q-sub">
              {item.originType === "MANUAL" ? "Origem manual" : item.originReference}
            </span>
          </span>
          <span className="q-right">
            <span className={`badge c-${item.status.toLowerCase()}`}>{statusLabel[item.status]}</span>
            <span className="q-time">{item.candidateCount} fonte(s)</span>
          </span>
        </button>
      ))}
    </div>
  );
}
