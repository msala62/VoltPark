package it.unibg.voltpark.state;

public class StatoInErogazione implements StatoColonnina {

    @Override
    public void prenota(ColonninaContext ctx) {
        throw new IllegalStateException("Colonnina occupata in erogazione.");
    }

    @Override
    public void avviaRicarica(ColonninaContext ctx) {
        throw new IllegalStateException("Ricarica gia' in corso.");
    }

    @Override
    public void sospendiPerSovraccarico(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoSospesaSovraccarico());
    }

    @Override
    public void riprendiErogazione(ColonninaContext ctx, double kwAssegnati) {
        ctx.setPotenzaAllocataKw(kwAssegnati);
    }

    @Override
    public void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti) {
        double ore = minuti / 60.0;
        double kwhProdotti = ctx.getPotenzaAllocataKw() * ore;
        ctx.incrementaEnergiaESoc(kwhProdotti);

        if (ctx.getSocPercentuale() >= 100.0) {
            ctx.setPotenzaAllocataKw(0.0);
            ctx.setStatoCorrente(new StatoCaricaCompletata());
            ctx.notificaCambiamentoRete();
        }
    }

    @Override
    public void terminaSessione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoLibera());
        ctx.notificaCambiamentoRete();
    }

    @Override
    public void impostaManutenzione(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
        ctx.setStatoCorrente(new StatoFuoriServizio());
        ctx.notificaCambiamentoRete();
    }

    @Override
    public String getNomeStato() {
        return "IN_EROGAZIONE";
    }
}