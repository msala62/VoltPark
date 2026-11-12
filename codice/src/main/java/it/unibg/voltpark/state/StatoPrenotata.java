package it.unibg.voltpark.state;

public class StatoPrenotata implements StatoColonnina {

    @Override
    public void prenota(ColonninaContext ctx) {
        throw new IllegalStateException("La colonnina e' gia' prenotata.");
    }

    @Override
    public void avviaRicarica(ColonninaContext ctx) {
        ctx.setStatoCorrente(new StatoInErogazione());
        ctx.notificaCambiamentoRete();
    }

    @Override
    public void sospendiPerSovraccarico(ColonninaContext ctx) {
        throw new IllegalStateException("Sessione di ricarica non ancora avviata.");
    }

    @Override
    public void riprendiErogazione(ColonninaContext ctx, double kwAssegnati) {
        throw new IllegalStateException("Sessione di ricarica non ancora avviata.");
    }

    @Override
    public void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti) {
        // La scadenza della prenotazione e' gestita a livello di Stallo
    }

    @Override
    public void terminaSessione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoLibera());
    }

    @Override
    public void impostaManutenzione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoFuoriServizio());
    }

    @Override
    public String getNomeStato() {
        return "PRENOTATA";
    }
}