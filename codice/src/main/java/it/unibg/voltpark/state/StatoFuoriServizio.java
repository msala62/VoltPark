package it.unibg.voltpark.state;

public class StatoFuoriServizio implements StatoColonnina {

    @Override
    public void prenota(ColonninaContext ctx) {
        throw new IllegalStateException("Colonnina fuori servizio.");
    }

    @Override
    public void avviaRicarica(ColonninaContext ctx) {
        throw new IllegalStateException("Colonnina fuori servizio.");
    }

    @Override
    public void sospendiPerSovraccarico(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
    }

    @Override
    public void riprendiErogazione(ColonninaContext ctx, double kwAssegnati) {
        throw new IllegalStateException("Colonnina fuori servizio.");
    }

    @Override
    public void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti) {
        // Nessuna operazione
    }

    @Override
    public void terminaSessione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoLibera());
    }

    @Override
    public void impostaManutenzione(ColonninaContext ctx) {
        // Gia' in manutenzione
    }

    @Override
    public String getNomeStato() {
        return "FUORI_SERVIZIO";
    }
}