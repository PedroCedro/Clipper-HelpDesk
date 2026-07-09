import Console from "./pages/Console";

// O app É o console do agente (superfície desktop). Portal do
// solicitante e intake por WhatsApp são superfícies separadas, de
// outra fase — não entram aqui.
export default function App() {
  return <Console />;
}
