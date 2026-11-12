package it.unibg.voltpark.state;

public class StatoSospesaSovraccarico implements StatoColonnina {

    @Override
    public void prenota(ColonninaContext ctx) {
        throw new IllegalStateException("Colonnina occupata.");
    }

    @Override
    public void avviaRicarica(ColonninaContext ctx) {
        throw new IllegalStateException("Sessione gia' presente ma sospesa.");
    }

    @Override
    public void sospendiPerSovraccarico(ColonninaContext ctx) {
        ctx.setPotenzaAllocataKw(0.0);
    }

    @Override
    public void riprendiErogazione(ColonninaContext ctx, double kwAssegnati) {
        ctx.setPotenzaAllocataKw(kwAssegnati);
        ctx.setStatoCorrente(new StatoInErogazione());
    }

    @Override
    public void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti) {
        // Potenza 0 kW: il SoC non avanza finche' non torna potenza disponibile
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
        return "SOSPESA_PER_SOVRACCARICO";
    }
}