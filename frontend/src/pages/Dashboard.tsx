import { useEffect, useState } from "react";
import ClipperWidget from "../components/ClipperWidget";

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

export default function Dashboard() {
  const [health, setHealth] = useState<ApiHealth | null>(null);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadDashboard() {
      setIsLoading(true);
      setError(null);

      try {
        const [healthResponse, ticketsResponse] = await Promise.all([
          fetch("/api/health"),
          fetch("/api/tickets"),
        ]);

        if (!healthResponse.ok || !ticketsResponse.ok) {
          throw new Error("Nao foi possivel consultar o backend local.");
        }

        const [healthData, ticketData] = await Promise.all([
          healthResponse.json() as Promise<ApiHealth>,
          ticketsResponse.json() as Promise<Ticket[]>,
        ]);

        if (!isMounted) {
          return;
        }

        setHealth(healthData);
        setTickets(ticketData);
      } catch (loadError) {
        if (!isMounted) {
          return;
        }

        const message =
          loadError instanceof Error
            ? loadError.message
            : "Falha inesperada ao carregar dados.";

        setError(message);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadDashboard();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <main className="dashboard">
      <section className="hero">
        <p className="eyebrow">Clipper Helpdesk</p>
        <h1>Diagnóstico automatizado antes da atuação do suporte humano.</h1>
        <p className="lead">
          Novos tickets chegam primeiro aqui. O Clipper analisa o contexto,
          aplica regras de diagnóstico e prepara o caso para a equipe de
          atendimento.
        </p>
      </section>
      <ClipperWidget
        error={error}
        health={health}
        isLoading={isLoading}
        tickets={tickets}
      />
    </main>
  );
}
