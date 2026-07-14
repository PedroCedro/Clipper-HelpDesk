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
  // Contexto do chamado: tudo pode vir nulo (ticket antigo no banco,
  // intake mínimo via API). A UI omite o que não existe — nunca quebra.
  priority: string | null;
  requester: string | null;
  routine: string | null;
  createdAt: string | null;
  // A resposta aplicada ao solicitante — nula até a ação "aplicar como
  // resposta" acontecer.
  response: string | null;
  // Resumo do último diagnóstico, composto na borda pelo backend
  // (GET /tickets). Nulo = nunca diagnosticado, e a fila mostra a
  // linha sem tag de IA — que é a verdade.
  diagnosis: DiagnosisSummary | null;
};

// Só o que a tag de IA da fila precisa (estado do gate + confiança);
// o diagnóstico completo é buscado por ticket, sob demanda.
export type DiagnosisSummary = {
  state: GroundingState;
  confidence: number;
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

// As ações do painel (rodada B3 do backend). O Console é o dono das
// chamadas e do estado dos tickets; o painel só dispara e espera — as
// Promises rejeitam com Error legível pra UI exibir.
export type TicketActions = {
  onApplyResponse: (id: number, text: string) => Promise<void>;
  onEscalate: (id: number) => Promise<void>;
  onFlagIncorrect: (id: number, reason: string | null) => Promise<void>;
};

export type CurationStatus =
  | "ABERTO"
  | "COM_CANDIDATOS"
  | "RASCUNHO"
  | "EM_REVISAO"
  | "PUBLICADO"
  | "DESCARTADO";

export type CurationCaseSummary = {
  id: number;
  originType: "TICKET" | "FEEDBACK" | "MANUAL";
  originReference: string | null;
  status: CurationStatus;
  reason: string;
  author: string;
  candidateCount: number;
  createdAt: string;
  updatedAt: string;
};

export type CurationCandidate = {
  id: number;
  documentId: number;
  title: string;
  module: string | null;
  sourceLabel: string;
  sourceUrl: string | null;
  author: string;
  reason: string;
  createdAt: string;
};

export type CurationTransition = {
  id: number;
  fromStatus: CurationStatus | null;
  toStatus: CurationStatus;
  actor: string;
  reason: string;
  createdAt: string;
};

export type CurationCandidateEvent = {
  id: number;
  documentId: number;
  eventType: "ADDED" | "REMOVED";
  actor: string;
  reason: string;
  createdAt: string;
};

export type CurationCaseDetail = CurationCaseSummary & {
  candidates: CurationCandidate[];
  transitions: CurationTransition[];
  candidateEvents: CurationCandidateEvent[];
};

export type RawSearchItem = {
  id: number;
  externalId: string;
  title: string;
  module: string | null;
  score: number;
  matchReasons: string[];
  snippet: string;
  sourceUrl: string | null;
  sourceUpdatedAt: string | null;
};

export type RawSearchPage = {
  items: RawSearchItem[];
  page: number;
  size: number;
  total: number;
};
