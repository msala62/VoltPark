package it.unibg.voltpark.state;

public interface ColonninaContext {
    String getIdColonnina();
    double getPotenzaAllocataKw();
    void setPotenzaAllocataKw(double kw);
    double getSocPercentuale();
    void incrementaEnergiaESoc(double kwhErogati);
    int getMinutiPostCarica();
    void incrementaMinutiPostCarica(int minuti);
    int getMinutiOverstay();
    void incrementaMinutiOverstay(int minuti);
    void setStatoCorrente(StatoColonnina nuovoStato);
    void notificaCambiamentoRete();
}