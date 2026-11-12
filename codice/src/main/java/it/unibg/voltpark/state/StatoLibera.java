package it.unibg.voltpark.state;

public class StatoLibera implements StatoColonnina {

    @Override
    public void prenota(ColonninaContext ctx) {
        ctx.setStatoCorrente(new StatoPrenotata());
    }

    @Override
    public void avviaRicarica(ColonninaContext ctx) {
        ctx.setStatoCorrente(new StatoInErogazione());
        ctx.notificaCambiamentoRete();
    }

    @Override
    public void sospendiPerSovraccarico(ColonninaContext ctx) {
        throw new IllegalStateException("Colonnina libera: impossibile sospendere.");
    }

    @Override
    public void riprendiErogazione(ColonninaContext ctx, double kwAssegnati) {
        throw new IllegalStateException("Colonnina libera: nessuna sessione attiva.");
    }

    @Override
    public void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti) {
        // Nessuna azione richiesta quando la colonnina e' libera
    }

    @Override
    public void terminaSessione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
    }

    @Override
    public void impostaManutenzione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoFuoriServizio());
    }

    @Override
    public String getNomeStato() {
        return "LIBERA";
    }
}