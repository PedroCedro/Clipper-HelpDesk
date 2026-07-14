import { useState } from "react";
import type { RawSearchItem, RawSearchPage } from "../types";

type Props = {
  linkedIds: Set<number>;
  disabled: boolean;
  onAdd: (documentId: number, suggestedReason: string) => void;
};

export default function CatalogSearch({ linkedIds, disabled, onAdd }: Props) {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<RawSearchItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);

  async function search() {
    if (query.trim().length < 2) return;
    setLoading(true);
    setSearched(true);
    setError(null);
    try {
      const response = await fetch(`/api/raw-documents/search?q=${encodeURIComponent(query.trim())}&size=10`);
      if (!response.ok) throw new Error(`Busca indisponível (HTTP ${response.status}).`);
      setItems(((await response.json()) as RawSearchPage).items);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao pesquisar fontes.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="curation-card">
      <h3>Pesquisar fontes oficiais</h3>
      <div className="catalog-search">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onKeyDown={(event) => event.key === "Enter" && void search()}
          placeholder="Rotina, erro ou assunto…"
          aria-label="Pesquisar fontes oficiais"
        />
        <button className="btn-ghost" onClick={() => void search()} disabled={loading || query.trim().length < 2}>
          {loading ? "Buscando…" : "Buscar"}
        </button>
      </div>
      {error ? <p className="curation-error">{error}</p> : null}
      {!error && searched && !loading && items.length === 0 ? (
        <p className="catalog-empty">Nenhuma fonte encontrada. Tente uma rotina, código de erro ou termo mais específico.</p>
      ) : null}
      {!searched ? <p className="catalog-hint">Pesquise no catálogo bruto para encontrar evidências oficiais.</p> : null}
      <div className="catalog-results">
        {items.map((item) => {
          const linked = linkedIds.has(item.id);
          return (
            <article className="catalog-result" key={item.id}>
              <div>
                <strong>{item.title}</strong>
                <p>{item.snippet}</p>
                <span>Fonte oficial{item.module ? ` · ${item.module}` : ""}</span>
              </div>
              <button
                className="btn-ghost"
                disabled={disabled || linked}
                onClick={() => onAdd(item.id, `Fonte associada após busca por “${query.trim()}”`)}
              >
                {linked ? "Associada" : "Associar"}
              </button>
            </article>
          );
        })}
      </div>
    </section>
  );
}
