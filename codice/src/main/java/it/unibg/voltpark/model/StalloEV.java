package it.unibg.voltpark.model;

import java.time.LocalDateTime;

public class StalloEV extends Stallo {
    private static final double TARIFFA_ORARIA_EV = 2.00;
    private final Colonnina colonnina;

    public StalloEV(String codice, Colonnina colonnina) {
        super(codice);
        if (colonnina == null) {
            throw new IllegalArgumentException("Uno StalloEV deve possedere una colonnina.");
        }
        this.colonnina = colonnina;
    }

    @Override
    public boolean supportaRicaricaEV() {
        return true;
    }

    @Override
    public double getTariffaOrariaSosta() {
        return TARIFFA_ORARIA_EV;
    }

    @Override
    public void prenota(String targa, LocalDateTime adesso, int minutiValidita) {
        super.prenota(targa, adesso, minutiValidita);
        colonnina.getStatoCorrente().prenota(colonnina);
    }

    @Override
    public void libera() {
        super.libera();
        colonnina.chiudiSessione();
    }

    @Override
    public void mettiFuoriServizio() {
        super.mettiFuoriServizio();
        colonnina.getStatoCorrente().impostaManutenzione(colonnina);
    }

    @Override
    public void rimettiInServizio() {
        super.rimettiInServizio();
        colonnina.chiudiSessione();
    }

    public Colonnina getColonnina() {
        return colonnina;
    }
}