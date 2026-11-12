package it.unibg.voltpark.state;

public interface StatoColonnina {
    void prenota(ColonninaContext ctx);
    void avviaRicarica(ColonninaContext ctx);
    void sospendiPerSovraccarico(ColonninaContext ctx);
    void riprendiErogazione(ColonninaContext ctx, double kwAssegnati);
    void avanzaTempo(ColonninaContext ctx, int minuti, double sogliaGraziaMinuti);
    void terminaSessione(ColonninaContext ctx);
    void impostaManutenzione(ColonninaContext ctx);
    String getNomeStato();
}