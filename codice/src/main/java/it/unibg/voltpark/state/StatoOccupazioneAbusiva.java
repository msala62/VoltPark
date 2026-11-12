package it.unibg.voltpark.state;

public class StatoOccupazioneAbusiva implements StatoColonnina {

    @Override
    public void prenota(ColonninaContext ctx) {
        throw new IllegalStateException("Stallo in occupazione abusiva.");
    }

    @Override
    public void avviaRicarica(ColonninaContext ctx) {
        throw new IllegalStateException("Carica gia' completata, liberare lo stallo.");
    }

    @Override
    public void sospendiPerSovraccarico(ColonninaContext ctx) {
        // Gia' a 0 kW
    }

    @Override
    public void riprendiErogazione(ColonninaContext ctx, double kwAssegnati) {
        ctx.setPotenzaAllocataKw(0.0);
    }

    @Override
    public void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti) {
        ctx.incrementaMinutiPostCarica(minuti);
        ctx.incrementaMinutiOverstay(minuti);
    }

    @Override
    public void terminaSessione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoLibera());
    }

    @Override
    public void impostaManutenzione(ColonninaContext ctx) {
        ctx.setStatoCorrente(new StatoFuoriServizio());
    }

    @Override
    public String getNomeStato() {
        return "OCCUPAZIONE_ABUSIVA";
    }
}