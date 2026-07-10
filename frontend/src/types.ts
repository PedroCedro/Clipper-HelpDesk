// Tipos espelhando os contratos da API. Ficam num arquivo só enquanto são
// poucos — se crescerem, cada domínio (ticket, diagnóstico) ganha o seu.

export type ApiHealth = {
  status: string;
  service: string;
};

export type Ticket = {
  id: number;
  title: string;
  description: string;
  status: string | null;
};

// Os três estados do gate — espelham o enum Grounding.State do backend.
// Contrato: estado novo lá obriga o TypeScript a apontar os mapas
// incompletos aqui.
export type GroundingState = "ANCORADO" | "APOIADO" | "SEM_BASE";

export type Grounding = {
  state: GroundingState;
  articleTitle: string | null;
  articleUrl: string | null;
  model: string | null;
};

export type DiagnosticResult = {
  probableCause: string;
  nextSteps: string;
  confidence: number;
  // String legível ("ancorado: ...") — humano/log. A UI decide pelo
  // grounding estruturado, nunca parseando esta string.
  source: string;
  grounding: Grounding;
};

// Estado de UI de um diagnóstico em andamento/concluído por ticket.
export type DiagnoseState = {
  loading: boolean;
  result?: DiagnosticResult;
  error?: string;
};
