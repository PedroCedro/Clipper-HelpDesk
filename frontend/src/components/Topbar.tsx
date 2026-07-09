import type { ThemeMode } from "../hooks/useTheme";

type TopbarProps = {
  ticketCount: number;
  search: string;
  onSearchChange: (value: string) => void;
  theme: ThemeMode;
  onThemeChange: (mode: ThemeMode) => void;
  onNewTicket: () => void;
};

// Barra superior: título/contagem, busca (filtra a fila em memória —
// com o volume atual não precisa de endpoint), seletor de tema e o
// botão de novo ticket.
export default function Topbar({
  ticketCount,
  search,
  onSearchChange,
  theme,
  onThemeChange,
  onNewTicket,
}: TopbarProps) {
  const modes: { mode: ThemeMode; label: string }[] = [
    { mode: "light", label: "☀ Claro" },
    { mode: "dark", label: "☾ Escuro" },
    { mode: "system", label: "◐ Sistema" },
  ];

  return (
    <header className="topbar">
      <span className="page-title">Fila de atendimento</span>
      <span className="page-count">{ticketCount} tickets</span>

      <label className="search">
        <span>⌕</span>
        <input
          placeholder="Buscar por nº, assunto, rotina ou código de erro…"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </label>

      <span className="spacer" />

      <div className="seg" role="group" aria-label="Tema">
        {modes.map(({ mode, label }) => (
          <button
            key={mode}
            className={theme === mode ? "on" : undefined}
            onClick={() => onThemeChange(mode)}
          >
            {label}
          </button>
        ))}
      </div>

      <button className="btn-primary" onClick={onNewTicket}>
        ＋ Novo ticket
      </button>
    </header>
  );
}
