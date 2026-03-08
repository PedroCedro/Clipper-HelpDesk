import ClipperWidget from "../components/ClipperWidget";

export default function Dashboard() {
  return (
    <main className="dashboard">
      <section className="hero">
        <p className="eyebrow">Clipper Helpdesk</p>
        <h1>Diagnostico automatizado antes da atuacao do suporte humano.</h1>
        <p className="lead">
          Novos tickets chegam primeiro aqui. O Clipper analisa o contexto,
          aplica regras de diagnostico e prepara o caso para a equipe de
          atendimento.
        </p>
      </section>
      <ClipperWidget />
    </main>
  );
}
