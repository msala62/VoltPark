package it.unibg.voltpark.grid;

import it.unibg.voltpark.model.Colonnina;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategia First Come First Served (FCFS): ordina le colonnine richiedenti per
 * istante di avvio ricarica crescente (con tie-break deterministico sull'ID) e
 * assegna a ciascuna fino al cap nominale di 22 kW finche' la potenza residua
 * rispetta la soglia minima tecnica Pmin. Le colonnine restanti vengono sospese (0.0 kW).
 */
public class FCFSStrategy implements LoadBalancingStrategy {

    private static final double EPSILON = 1e-9;
    private static final Comparator<Colonnina> ORDINE_ARRIVO = Comparator
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
                .sorted(ORDINE_ARRIVO)
                .toList();

        Map<Colonnina, Double> allocazioni = new LinkedHashMap<>();
        double potenzaResidua = Math.max(0.0, potenzaMassimaKw);

        for (Colonnina c : ordinate) {
            if (potenzaResidua + EPSILON >= sogliaMinimaKw) {
                double assegnata = Math.min(Colonnina.POTENZA_MASSIMA_NOMINALE_KW, potenzaResidua);
                allocazioni.put(c, assegnata);
                potenzaResidua = Math.max(0.0, potenzaResidua - assegnata);
            } else {
                allocazioni.put(c, 0.0);
            }
        }

        return allocazioni;
    }

    @Override
    public String getNomeStrategia() {
        return "FCFS";
    }
}
