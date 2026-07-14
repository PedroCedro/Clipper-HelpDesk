import { useState } from "react";
import Console from "./pages/Console";
import CurationWorkspace from "./pages/CurationWorkspace";
import type { Ticket } from "./types";

// O app É o console do agente (superfície desktop). Portal do
// solicitante e intake por WhatsApp são superfícies separadas, de
// outra fase — não entram aqui.
export default function App() {
  const [view, setView] = useState<"tickets" | "curation">("tickets");
  const [pendingTicket, setPendingTicket] = useState<Ticket | null>(null);
  const [ticketCount, setTicketCount] = useState(0);
  return view === "tickets" ? (
    <Console
      onOpenCuration={() => setView("curation")}
      onSendToCuration={(ticket) => { setPendingTicket(ticket); setView("curation"); }}
      onTicketCountChange={setTicketCount}
    />
  ) : (
    <CurationWorkspace
      pendingTicket={pendingTicket}
      ticketCount={ticketCount}
      onPendingTicketHandled={() => setPendingTicket(null)}
      onOpenTickets={() => setView("tickets")}
    />
  );
}
