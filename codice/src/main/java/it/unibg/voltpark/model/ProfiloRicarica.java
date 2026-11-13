package it.unibg.voltpark.model;

public enum ProfiloRicarica {
    ECO(1, 0.45),
    FAST(2, 0.65);

    private final int livelloPriorita;
    private final double costoPerKwh;

    ProfiloRicarica(int livelloPriorita, double costoPerKwh) {
        this.livelloPriorita = livelloPriorita;
        this.costoPerKwh = costoPerKwh;
    }

    public int getLivelloPriorita() {
        return livelloPriorita;
    }

    public double getCostoPerKwh() {
        return costoPerKwh;
    }
}