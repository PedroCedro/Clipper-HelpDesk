import { useState } from "react";
import Console from "./pages/Console";
import CurationWorkspace from "./pages/CurationWorkspace";

// O app É o console do agente (superfície desktop). Portal do
// solicitante e intake por WhatsApp são superfícies separadas, de
// outra fase — não entram aqui.
export default function App() {
  const [view, setView] = useState<"tickets" | "curation">("tickets");
  return view === "tickets" ? (
    <Console onOpenCuration={() => setView("curation")} />
  ) : (
    <CurationWorkspace onOpenTickets={() => setView("tickets")} />
  );
}
