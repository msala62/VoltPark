package it.unibg.voltpark.state;

public class StatoCaricaCompletata implements StatoColonnina {

    @Override
    public void prenota(ColonninaContext ctx) {
        throw new IllegalStateException("Colonnina ancora occupata dal veicolo a fine carica.");
    }

    @Override
    public void avviaRicarica(ColonninaContext ctx) {
        throw new IllegalStateException("Batteria gia' al 100%.");
    }

    @Override
    public void sospendiPerSovraccarico(ColonninaContext ctx) {
        // Gia' a 0 kW
    }

    @Override
    public void riprendiErogazione(ColonninaContext ctx, double kwAssegnati) {
        // Gia' al 100%, non assorbe potenza
        ctx.setPotenzaAllocataKw(0.0);
    }

    @Override
    public void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti) {
        ctx.incrementaMinutiPostCarica(minuti);
        if (ctx.getMinutiPostCarica() > sogliaGraziaMinuti) {
            int eccedenza = (int) (ctx.getMinutiPostCarica() - sogliaGraziaMinuti);
            ctx.incrementaMinutiOverstay(eccedenza);
            ctx.setStatoCorrente(new StatoOccupazioneAbusiva());
        }
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
        return "CARICA_COMPLETATA";
    }
}