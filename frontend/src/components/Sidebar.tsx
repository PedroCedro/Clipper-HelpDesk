// Sidebar do console: marca + navegação + usuário. Só a "Fila de
// atendimento" tem tela por trás hoje; o resto aparece apagado (classe
// .soon) de propósito — mostrar o mapa do produto sem fingir navegação.
type SidebarProps = {
  ticketCount: number;
  curationCount?: number;
  active: "tickets" | "curation";
  onOpenTickets?: () => void;
  onOpenCuration?: () => void;
};

export default function Sidebar({
  ticketCount,
  curationCount = 0,
  active,
  onOpenTickets,
  onOpenCuration,
}: SidebarProps) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">
          {/* A marca: ticket + pulso (mesma do favicon). Inline pra herdar
              o currentColor do tema. */}
          <svg
            viewBox="0 0 48 48"
            fill="none"
            stroke="currentColor"
            strokeWidth="4.4"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <rect x="8" y="11" width="32" height="26" rx="7" />
            <polyline points="14 25 18.5 25 21.5 19.5 25.5 30 28 25 34 25" strokeWidth="3.6" />
          </svg>
        </div>
        <div>
          <div className="brand-name">Clipper</div>
          <div className="brand-sub">Helpdesk</div>
        </div>
      </div>

      <nav className="nav">
        <div className="nav-label">Atendimento</div>
        <button className={`nav-item ${active === "tickets" ? "active" : ""}`} onClick={onOpenTickets}>
          <span className="ico">▤</span> Fila de atendimento
          <span className="count">{ticketCount}</span>
        </button>
        <div className="nav-item soon" title="Em breve">
          <span className="ico">◔</span> Meus tickets
        </div>
        <div className="nav-item soon" title="Em breve">
          <span className="ico">✓</span> Resolvidos
        </div>

        <div className="nav-label">Clipper</div>
        <button className={`nav-item ${active === "curation" ? "active" : ""}`} onClick={onOpenCuration}>
          <span className="ico">◇</span> Curadoria
          <span className="count">{curationCount}</span>
        </button>
        <div className="nav-item soon" title="Em breve">
          <span className="ico">◈</span> Base de conhecimento
        </div>
        <div className="nav-item soon" title="Em breve">
          <span className="ico">⚙</span> Configurações
        </div>
      </nav>

      <div className="user">
        <div className="avatar">PC</div>
        <div>
          <div className="user-name">Pedro Cedro</div>
          <div className="user-role">Suporte · N2</div>
        </div>
      </div>
    </aside>
  );
}
