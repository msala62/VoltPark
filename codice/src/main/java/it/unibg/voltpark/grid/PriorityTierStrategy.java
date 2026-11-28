package it.unibg.voltpark.grid;

import it.unibg.voltpark.model.Colonnina;
import it.unibg.voltpark.model.ProfiloRicarica;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategia Strict Priority Tier (FAST / ECO):
 * le colonnine con profilo FAST (priorita' 2) vengono alimentate per prime fino al cap
 * nominale di 22 kW; l'eventuale potenza residua viene ripartita equamente tra le
 * colonnine ECO (priorita' 1). In entrambi i tier nessuna colonnina eroga mai potenza
 * inferiore alla soglia minima tecnica Pmin: le eccedenti ricevono 0.0 kW (sospese).
 */
public class PriorityTierStrategy implements LoadBalancingStrategy {

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

        List<Colonnina> tierFast = colonnineRichiedenti.stream()
                .filter(c -> c.getProfiloRicarica() == ProfiloRicarica.FAST)
                .sorted(ORDINE_DETERMINISTICO)
                .toList();
        List<Colonnina> tierEco = colonnineRichiedenti.stream()
                .filter(c -> c.getProfiloRicarica() != ProfiloRicarica.FAST)
                .sorted(ORDINE_DETERMINISTICO)
                .toList();

        Map<Colonnina, Double> allocazioni = new LinkedHashMap<>();
        double potenzaResidua = Math.max(0.0, potenzaMassimaKw);

        // Fase 1: allocazione prioritaria al tier FAST
        if (!tierFast.isEmpty()) {
            int maxFastServibili = (int) Math.floor((potenzaResidua + EPSILON) / sogliaMinimaKw);
            int fastServite = Math.min(tierFast.size(), maxFastServibili);
            double quotaFast = fastServite > 0
                    ? Math.min(Colonnina.POTENZA_MASSIMA_NOMINALE_KW, potenzaResidua / fastServite)
                    : 0.0;

            for (int i = 0; i < tierFast.size(); i++) {
                double assegnata = i < fastServite ? quotaFast : 0.0;
                allocazioni.put(tierFast.get(i), assegnata);
            }
            potenzaResidua = Math.max(0.0, potenzaResidua - (fastServite * quotaFast));
        }

        // Fase 2: allocazione della potenza residua al tier ECO
        if (!tierEco.isEmpty()) {
            int maxEcoServibili = (int) Math.floor((potenzaResidua + EPSILON) / sogliaMinimaKw);
            int ecoServite = Math.min(tierEco.size(), maxEcoServibili);
            double quotaEco = ecoServite > 0
                    ? Math.min(Colonnina.POTENZA_MASSIMA_NOMINALE_KW, potenzaResidua / ecoServite)
                    : 0.0;

            for (int i = 0; i < tierEco.size(); i++) {
                double assegnata = i < ecoServite ? quotaEco : 0.0;
                allocazioni.put(tierEco.get(i), assegnata);
            }
        }

        return allocazioni;
    }

    @Override
    public String getNomeStrategia() {
        return "PRIORITY_TIER";
    }
}
