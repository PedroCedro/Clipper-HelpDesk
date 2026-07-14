import { useCallback, useEffect, useMemo, useState } from "react";
import Sidebar from "../components/Sidebar";
import CurationQueue from "../components/CurationQueue";
import CurationDetail from "../components/CurationDetail";
import type { CurationCaseDetail, CurationCaseSummary, CurationStatus, Ticket } from "../types";

const OPERATOR_KEY = "clipper.curation.operator";

type AuditAction = {
  title: string;
  confirmLabel: string;
  run: (reason: string) => Promise<void>;
};

type Props = {
  pendingTicket: Ticket | null;
  ticketCount: number;
  onPendingTicketHandled: () => void;
  onOpenTickets: () => void;
};

export default function CurationWorkspace({ pendingTicket, ticketCount, onPendingTicketHandled, onOpenTickets }: Props) {
  const [cases, setCases] = useState<CurationCaseSummary[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<CurationCaseDetail | null>(null);
  const [status, setStatus] = useState<CurationStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [operator, setOperator] = useState(() => localStorage.getItem(OPERATOR_KEY) ?? "");
  const [auditAction, setAuditAction] = useState<AuditAction | null>(null);
  const [auditReason, setAuditReason] = useState("");
  const [auditBusy, setAuditBusy] = useState(false);
  const [ticketReason, setTicketReason] = useState("");

  useEffect(() => {
    if (operator.trim()) localStorage.setItem(OPERATOR_KEY, operator.trim());
    else localStorage.removeItem(OPERATOR_KEY);
  }, [operator]);

  const loadCases = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const suffix = status ? `?status=${status}` : "";
      const response = await fetch(`/api/curation-cases${suffix}`);
      if (!response.ok) throw new Error(`Não foi possível carregar a fila (HTTP ${response.status}).`);
      const data = (await response.json()) as CurationCaseSummary[];
      setCases(data);
      setSelectedId((current) => data.some((item) => item.id === current) ? current : data[0]?.id ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar a curadoria.");
    } finally {
      setLoading(false);
    }
  }, [status]);

  const loadDetail = useCallback(async (id: number) => {
    setDetailLoading(true);
    try {
      const response = await fetch(`/api/curation-cases/${id}`);
      if (!response.ok) throw new Error(`Não foi possível abrir o caso (HTTP ${response.status}).`);
      setDetail((await response.json()) as CurationCaseDetail);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Falha ao abrir o caso.");
      setDetail(null);
    } finally {
      setDetailLoading(false);
    }
  }, []);

  useEffect(() => { void loadCases(); }, [loadCases]);
  useEffect(() => { if (selectedId !== null) void loadDetail(selectedId); else setDetail(null); }, [selectedId, loadDetail]);

  async function mutate(url: string, init: RequestInit) {
    setActionError(null);
    if (!operator.trim()) throw new Error("Informe o operador responsável antes de realizar a ação.");
    const response = await fetch(url, {
      ...init,
      headers: { "Content-Type": "application/json", "X-Actor": operator.trim(), ...init.headers },
    });
    if (!response.ok) throw new Error(`A operação não pôde ser concluída (HTTP ${response.status}).`);
    if (selectedId !== null) await loadDetail(selectedId);
    await loadCases();
  }

  function requestAudit(action: AuditAction, suggestedReason = "") {
    if (!operator.trim()) {
      setActionError("Informe o operador responsável antes de realizar a ação.");
      return;
    }
    setAuditReason(suggestedReason);
    setAuditAction(action);
  }

  async function confirmAudit() {
    if (!auditAction || !auditReason.trim()) return;
    setAuditBusy(true);
    try {
      await auditAction.run(auditReason.trim());
      setAuditAction(null);
      setAuditReason("");
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Falha ao registrar a ação.");
    } finally {
      setAuditBusy(false);
    }
  }

  async function sendPendingTicket() {
    if (!pendingTicket || !operator.trim() || !ticketReason.trim()) return;
    setAuditBusy(true);
    setActionError(null);
    try {
      const response = await fetch(`/api/tickets/${pendingTicket.id}/curation-case`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Actor": operator.trim() },
        body: JSON.stringify({ reason: ticketReason.trim() }),
      });
      if (!response.ok) throw new Error(`Não foi possível enviar o ticket (HTTP ${response.status}).`);
      const result = (await response.json()) as { curationCase: CurationCaseSummary; created: boolean };
      onPendingTicketHandled();
      setTicketReason("");
      await loadCases();
      setSelectedId(result.curationCase.id);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Falha ao enviar ticket para curadoria.");
    } finally {
      setAuditBusy(false);
    }
  }

  const visibleCount = useMemo(() => cases.filter((item) => item.status !== "DESCARTADO").length, [cases]);

  return (
    <div className="app">
      <Sidebar ticketCount={ticketCount} curationCount={visibleCount} active="curation" onOpenTickets={onOpenTickets} />
      <main className="workspace">
        <header className="curation-topbar">
          <div><span className="d-eyebrow">Clipper</span><h1>Curadoria de conhecimento</h1></div>
          <label className="operator-field">
            Operador <span>declaratório</span>
            <input
              value={operator}
              onChange={(event) => setOperator(event.target.value)}
              placeholder="Seu nome"
              aria-label="Operador responsável"
            />
          </label>
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value as CurationStatus | "")}>
              <option value="">Todos</option><option value="ABERTO">Abertos</option>
              <option value="COM_CANDIDATOS">Com candidatos</option><option value="DESCARTADO">Descartados</option>
            </select>
          </label>
        </header>
        <div className="curation-panes">
          <aside className="curation-queue">
            <div className="queue-heading"><strong>Fila de investigação</strong><span>{cases.length} casos</span></div>
            <CurationQueue cases={cases} selectedId={selectedId} loading={loading} error={error} onSelect={setSelectedId} />
          </aside>
          <CurationDetail
            detail={detail}
            loading={detailLoading}
            actionError={actionError}
            onAdd={(documentId, suggestedReason) => {
              requestAudit({
                title: "Justificar associação da fonte",
                confirmLabel: "Associar fonte",
                run: (reason) => mutate(`/api/curation-cases/${selectedId}/candidates`, {
                  method: "POST", body: JSON.stringify({ documentId, reason }),
                }),
              }, suggestedReason);
            }}
            onRemove={(documentId) => {
              requestAudit({
                title: "Justificar remoção da fonte",
                confirmLabel: "Remover fonte",
                run: (reason) => mutate(
                  `/api/curation-cases/${selectedId}/candidates/${documentId}?reason=${encodeURIComponent(reason)}`,
                  { method: "DELETE" },
                ),
              });
            }}
            onDiscard={() => {
              requestAudit({
                title: "Justificar descarte do caso",
                confirmLabel: "Descartar caso",
                run: (reason) => mutate(`/api/curation-cases/${selectedId}/transitions`, {
                  method: "POST", body: JSON.stringify({ targetStatus: "DESCARTADO", reason }),
                }),
              });
            }}
          />
        </div>
        {pendingTicket ? (
          <div className="audit-backdrop" role="presentation">
            <section className="audit-dialog" role="dialog" aria-modal="true" aria-labelledby="ticket-curation-title">
              <span className="d-eyebrow">Ticket #{pendingTicket.id}</span>
              <h2 id="ticket-curation-title">Enviar para curadoria</h2>
              <p>{pendingTicket.title}</p>
              <label>
                Motivo da curadoria
                <textarea
                  autoFocus
                  value={ticketReason}
                  onChange={(event) => setTicketReason(event.target.value)}
                  rows={4}
                  placeholder="Explique por que este caso pode gerar conhecimento reutilizável…"
                />
              </label>
              {!operator.trim() ? <p className="inline-warning">Preencha o operador no topo para continuar.</p> : null}
              <div className="audit-actions">
                <button className="btn-ghost" disabled={auditBusy} onClick={onPendingTicketHandled}>Cancelar</button>
                <button
                  className="btn-primary"
                  disabled={auditBusy || !operator.trim() || !ticketReason.trim()}
                  onClick={() => void sendPendingTicket()}
                >
                  {auditBusy ? "Enviando…" : "Criar caso de curadoria"}
                </button>
              </div>
            </section>
          </div>
        ) : null}
        {auditAction ? (
          <div className="audit-backdrop" role="presentation">
            <section className="audit-dialog" role="dialog" aria-modal="true" aria-labelledby="audit-title">
              <span className="d-eyebrow">Registro de auditoria</span>
              <h2 id="audit-title">{auditAction.title}</h2>
              <p>O motivo ficará no histórico do caso e ajudará a melhorar a curadoria.</p>
              <label>
                Motivo
                <textarea
                  autoFocus
                  value={auditReason}
                  onChange={(event) => setAuditReason(event.target.value)}
                  rows={4}
                  placeholder="Explique a decisão…"
                />
              </label>
              <div className="audit-actions">
                <button className="btn-ghost" disabled={auditBusy} onClick={() => setAuditAction(null)}>Cancelar</button>
                <button className="btn-primary" disabled={auditBusy || !auditReason.trim()} onClick={() => void confirmAudit()}>
                  {auditBusy ? "Registrando…" : auditAction.confirmLabel}
                </button>
              </div>
            </section>
          </div>
        ) : null}
      </main>
    </div>
  );
}
