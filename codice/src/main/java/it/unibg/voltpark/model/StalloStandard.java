package it.unibg.voltpark.model;

public class StalloStandard extends Stallo {
    private static final double TARIFFA_ORARIA_STANDARD = 1.50;

    public StalloStandard(String codice) {
        super(codice);
    }

    @Override
    public boolean supportaRicaricaEV() {
        return false;
    }

    @Override
    public double getTariffaOrariaSosta() {
        return TARIFFA_ORARIA_STANDARD;
    }
}