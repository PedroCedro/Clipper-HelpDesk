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

  // Cache de diagnósticos POR SESSÃO: trocar de ticket não perde o
  // resultado, mas recarregar a página perde — persistir é a rodada B1.
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

  // Busca client-side (título, descrição ou nº): com dezenas de tickets
  // é de graça; endpoint de busca só quando o volume pedir.
  const filteredTickets = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return tickets;
    return tickets.filter(
      (t) =>
        t.title.toLowerCase().includes(term) ||
        t.description.toLowerCase().includes(term) ||
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
    // demo é abrir e diagnosticar em seguida.
    setTickets((prev) => [created, ...prev]);
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
