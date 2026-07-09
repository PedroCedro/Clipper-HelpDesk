import { useState } from "react";

type NewTicketFormProps = {
  onCreate: (title: string, description: string) => Promise<void>;
  onCancel: () => void;
};

// Formulário mínimo DE PROPÓSITO: existe pra alimentar a demo do console
// sem curl. O intake real (portal do solicitante, WhatsApp) é outra
// superfície e outra fase — tudo desemboca no mesmo POST /api/tickets.
export default function NewTicketForm({ onCreate, onCancel }: NewTicketFormProps) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    if (!title.trim() || saving) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await onCreate(title.trim(), description.trim());
    } catch (err) {
      // O erro é tratado AQUI (não no Console): quem mostra o formulário
      // mostra a falha dele — sem catch, a rejeição subia muda e o usuário
      // ficava sem saber por que o ticket não abriu.
      setError(err instanceof Error ? err.message : "Falha inesperada ao abrir o ticket.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="new-ticket">
      <input
        placeholder="Título do chamado (ex.: Cupom fiscal não aparece na 1443)"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        autoFocus
      />
      <textarea
        placeholder="Descrição: o que acontece, em qual rotina, desde quando…"
        rows={3}
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      {error ? <p className="new-ticket-error">{error}</p> : null}
      <div className="new-ticket-actions">
        <button className="btn-ghost" onClick={onCancel} disabled={saving}>
          Cancelar
        </button>
        <button
          className="btn-primary"
          onClick={handleSubmit}
          disabled={saving || !title.trim()}
        >
          {saving ? "Abrindo…" : "Abrir ticket"}
        </button>
      </div>
    </div>
  );
}
