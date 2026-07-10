import { useEffect, useMemo, useState } from "react";
import Sidebar from "../components/Sidebar";
import Topbar from "../components/Topbar";
import TicketQueue from "../components/TicketQueue";
import TicketDetail from "../components/TicketDetail";
import NewTicketForm from "../components/NewTicketForm";
import { useTheme } from "../hooks/useTheme";
import type { DiagnoseState, DiagnosticResult, Ticket } from "../types";

// Página do console: DONA de todo o estado (tickets, seleção, busca,
// diagnósticos). Os componentes abaixo dela são apresentação — mesma
// filosofia do backend: um orquestrador magro, peças pequenas.
export default function Console() {
  const [theme, setTheme] = useTheme();

  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [search, setSearch] = useState("");
  const [showNewTicket, setShowNewTicket] = useState(false);

  // Cache de diagnósticos por sessão. Desde a B1 o backend persiste o
  // resultado, então o cache é só pra não rebuscar a cada clique — a
  // fonte da verdade é GET /tickets/{id}/diagnosis.
  const [diagnoses, setDiagnoses] = useState<Record<number, DiagnoseState>>({});

  useEffect(() => {
    let isMounted = true;

    async function loadTickets() {
      try {
        const response = await fetch("/api/tickets");
        if (!response.ok) {
          throw new Error(`Não foi possível consultar o backend (HTTP ${response.status}).`);
        }
        const data = (await response.json()) as Ticket[];
        if (!isMounted) return;
        setTickets(data);
        // Auto-seleciona o primeiro pra tela não abrir vazia.
        if (data.length > 0) {
          setSelectedId((current) => current ?? data[0].id);
        }
      } catch (err) {
        if (!isMounted) return;
        setError(err instanceof Error ? err.message : "Falha inesperada ao carregar tickets.");
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    void loadTickets();
    return () => {
      isMounted = false;
    };
  }, []);

  // Ao abrir um ticket ainda não visto na sessão, busca o diagnóstico
  // SALVO (B1): 200 = tinha, 204 = nunca diagnosticado. Qualquer entrada
  // no cache (mesmo vazia) marca "já conferido" e corta o refetch — por
  // isso o erro também vira entrada, senão o efeito re-tentaria em loop.
  //
  // Sem cleanup/cancelamento de propósito: o resultado é gravado POR ID,
  // então mesmo que o técnico já tenha pulado pra outro ticket a resposta
  // continua sendo dado válido — descartá-la deixaria a entrada travada
  // em loading. Deps só de selectedId: reagir à mudança de `diagnoses`
  // (que o próprio efeito causa) cancelaria o fetch em voo.
  useEffect(() => {
    if (selectedId === null || diagnoses[selectedId] !== undefined) return;
    const id = selectedId;

    async function loadSavedDiagnosis() {
      setDiagnoses((prev) => ({ ...prev, [id]: { loading: true } }));
      try {
        const response = await fetch(`/api/tickets/${id}/diagnosis`);
        if (response.status === 204) {
          setDiagnoses((prev) => ({ ...prev, [id]: { loading: false } }));
          return;
        }
        if (!response.ok) {
          throw new Error(`Não foi possível consultar o diagnóstico salvo (HTTP ${response.status}).`);
        }
        const data = (await response.json()) as { diagnosis: DiagnosticResult };
        setDiagnoses((prev) => ({ ...prev, [id]: { loading: false, result: data.diagnosis } }));
      } catch (err) {
        const message = err instanceof Error ? err.message : "Falha ao consultar o diagnóstico salvo.";
        setDiagnoses((prev) => ({ ...prev, [id]: { loading: false, error: message } }));
      }
    }

    void loadSavedDiagnosis();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId]);

  // Busca client-side (título, descrição ou nº): com dezenas de tickets
  // é de graça; endpoint de busca só quando o volume pedir.
  const filteredTickets = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return tickets;
    return tickets.filter(
      (t) =>
        t.title.toLowerCase().includes(term) ||
        t.description.toLowerCase().includes(term) ||
        (t.requester ?? "").toLowerCase().includes(term) ||
        (t.routine ?? "").toLowerCase().includes(term) ||
        String(t.id).includes(term),
    );
  }, [tickets, search]);

  const selectedTicket = tickets.find((t) => t.id === selectedId) ?? null;

  async function handleCreate(title: string, description: string) {
    const response = await fetch("/api/tickets", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      // Status nasce NOVO na borda de intake — regra de negócio de
      // status é do backend; aqui só declaramos o ponto de partida.
      body: JSON.stringify({ title, description, status: "NOVO" }),
    });
    if (!response.ok) {
      throw new Error(`Não foi possível abrir o ticket (HTTP ${response.status}).`);
    }
    const created = (await response.json()) as Ticket;
    // Recém-criado entra no topo e já selecionado: o fluxo natural da
    // demo é abrir e diagnosticar em seguida. O POST devolve o ticket
    // cru (sem o campo diagnosis da listagem) — normaliza pra null.
    setTickets((prev) => [{ ...created, diagnosis: created.diagnosis ?? null }, ...prev]);
    setSelectedId(created.id);
    setShowNewTicket(false);
  }

  async function handleDiagnose(id: number) {
    setDiagnoses((prev) => ({ ...prev, [id]: { loading: true } }));
    try {
      const response = await fetch(`/api/tickets/${id}/diagnose`, { method: "POST" });
      if (!response.ok) {
        throw new Error(`O Clipper não conseguiu diagnosticar (HTTP ${response.status}).`);
      }
      const data = (await response.json()) as { diagnosis: DiagnosticResult };
      setDiagnoses((prev) => ({ ...prev, [id]: { loading: false, result: data.diagnosis } }));
      // A tag de IA da linha reflete o resultado novo na hora, sem
      // refetch da listagem — mesmo resumo que o backend comporia.
      setTickets((prev) =>
        prev.map((t) =>
          t.id === id
            ? {
                ...t,
                diagnosis: {
                  state: data.diagnosis.grounding.state,
                  confidence: data.diagnosis.confidence,
                },
              }
            : t,
        ),
      );
    } catch (err) {
      const message = err instanceof Error ? err.message : "Falha inesperada no diagnóstico.";
      setDiagnoses((prev) => ({ ...prev, [id]: { loading: false, error: message } }));
    }
  }

  return (
    <div className="app">
      <Sidebar ticketCount={tickets.length} />

      <div className="workspace">
        <Topbar
          ticketCount={filteredTickets.length}
          search={search}
          onSearchChange={setSearch}
          theme={theme}
          onThemeChange={setTheme}
          onNewTicket={() => setShowNewTicket((v) => !v)}
        />

        {showNewTicket ? (
          <NewTicketForm onCreate={handleCreate} onCancel={() => setShowNewTicket(false)} />
        ) : null}

        <div className="panes">
          <TicketQueue
            tickets={filteredTickets}
            selectedId={selectedId}
            onSelect={setSelectedId}
            isLoading={isLoading}
            error={error}
          />
          <TicketDetail
            ticket={selectedTicket}
            diag={selectedTicket ? diagnoses[selectedTicket.id] : undefined}
            onDiagnose={handleDiagnose}
          />
        </div>
      </div>
    </div>
  );
}
