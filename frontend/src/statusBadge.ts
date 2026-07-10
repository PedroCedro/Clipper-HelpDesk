// Mapa status → rótulo + tom, num lugar só: fila e detalhe mostravam o
// mesmo status com mapas duplicados — divergir era questão de tempo.
// Status desconhecido cai no neutro em vez de quebrar (o backend pode
// ganhar status novos antes do front) e status nulo (ticket antigo no
// banco) vira "—".
export type StatusBadge = { label: string; className: string };

const byStatus: Record<string, StatusBadge> = {
  NOVO: { label: "Novo", className: "b-novo" },
  ABERTO: { label: "Aberto", className: "b-novo" },
  EM_ANALISE: { label: "Em análise", className: "b-analise" },
  RESOLVIDO: { label: "Resolvido", className: "b-resolvido" },
  // Escalado pede atenção de gente — é o único status que ganha o par
  // crítico (os outros pares já têm dono: novo=info, análise=warn, ok=verde).
  ESCALADO: { label: "Escalado", className: "b-escalado" },
};

export function statusBadge(status: string | null): StatusBadge {
  return (
    (status ? byStatus[status] : undefined) ?? {
      label: status ?? "—",
      className: "b-neutro",
    }
  );
}
