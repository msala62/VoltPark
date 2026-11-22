package it.unibg.voltpark.grid;

import it.unibg.voltpark.model.Colonnina;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategia di bilanciamento Equal Split: divide equamente la potenza disponibile
 * tra le colonnine richiedenti fino al cap nominale (22 kW). Se la quota equa scende
 * sotto la soglia minima tecnica, alimenta il massimo numero K = floor(Pmax / Pmin)
 * di colonnine in ordine deterministico di arrivo e sospende le restanti (0.0 kW).
 */
public class EqualSplitStrategy implements LoadBalancingStrategy {

    private static final double EPSILON = 1e-9;
    private static final Comparator<Colonnina> ORDINE_DETERMINISTICO = Comparator
            .comparing(Colonnina::getTimestampAvvioRicarica, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Colonnina::getIdColonnina);

    @Override
    public Map<Colonnina, Double> calcolaAllocazioni(
            List<Colonnina> colonnineRichiedenti,
            double potenzaMassimaKw,
            double sogliaMinimaKw) {
        if (sogliaMinimaKw <= 0.0 || Double.isNaN(sogliaMinimaKw) || Double.isInfinite(sogliaMinimaKw)) {
            throw new IllegalArgumentException("La soglia minima deve essere strettamente positiva.");
        }
        if (colonnineRichiedenti == null || colonnineRichiedenti.isEmpty()) {
            return Map.of();
        }

        List<Colonnina> ordinate = colonnineRichiedenti.stream()
                .sorted(ORDINE_DETERMINISTICO)
                .toList();

        double budget = Math.max(0.0, potenzaMassimaKw);
        int maxServibili = (int) Math.floor((budget + EPSILON) / sogliaMinimaKw);
        int numeroServite = Math.min(ordinate.size(), maxServibili);

        Map<Colonnina, Double> allocazioni = new LinkedHashMap<>();
        if (numeroServite <= 0) {
            for (Colonnina c : ordinate) {
                allocazioni.put(c, 0.0);
            }
            return allocazioni;
        }

        double quota = Math.min(Colonnina.POTENZA_MASSIMA_NOMINALE_KW, budget / numeroServite);
        for (int i = 0; i < ordinate.size(); i++) {
            allocazioni.put(ordinate.get(i), i < numeroServite ? quota : 0.0);
        }
        return allocazioni;
    }

    @Override
    public String getNomeStrategia() {
        return "EQUAL_SPLIT";
    }
}
