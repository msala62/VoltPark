package it.unibg.voltpark.grid;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import it.unibg.voltpark.model.Colonnina;
import it.unibg.voltpark.model.ProfiloRicarica;

/**
 * Test di unita' per {@link PriorityTierStrategy}.
 */
@DisplayName("PriorityTierStrategy")
class PriorityTierStrategyTest {

    private static final double DELTA = 1e-6;
    private final PriorityTierStrategy strategy = new PriorityTierStrategy();

    private static Colonnina creaColonninaAttiva(String id, ProfiloRicarica profilo) {
        Colonnina c = new Colonnina(id);
        c.configuraEAvviaSessione(50.0, 20.0, profilo);
        return c;
    }

    @Test
    @DisplayName("nome strategia coerente e lista vuota/null restituisce mappa vuota")
    void nomeEListaVuota() {
        assertAll(
                () -> assertEquals("PRIORITY_TIER", strategy.getNomeStrategia()),
                () -> assertTrue(strategy.calcolaAllocazioni(List.of(), 60.0, 3.7).isEmpty()),
                () -> assertTrue(strategy.calcolaAllocazioni(null, 60.0, 3.7).isEmpty()));
    }

    @ParameterizedTest(name = "soglia minima {0} rifiutata")
    @ValueSource(doubles = { 0.0, -2.5, Double.NaN, Double.NEGATIVE_INFINITY })
    void sogliaMinimaNonValida(double pMin) {
        assertThrows(IllegalArgumentException.class,
                () -> strategy.calcolaAllocazioni(List.of(), 60.0, pMin));
    }

    @Test
    @DisplayName("2 FAST + 2 ECO su 60 kW: FAST al massimo (22 kW), ECO si dividono il residuo (8 kW)")
    void fastAlMassimoEResiduoAdEco() {
        Colonnina f1 = creaColonninaAttiva("EV-01", ProfiloRicarica.FAST);
        Colonnina f2 = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);
        Colonnina e1 = creaColonninaAttiva("EV-03", ProfiloRicarica.ECO);
        Colonnina e2 = creaColonninaAttiva("EV-04", ProfiloRicarica.ECO);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(f1, f2, e1, e2), 60.0, 3.7);
        double somma = result.values().stream().mapToDouble(Double::doubleValue).sum();

        assertAll(
                () -> assertEquals(22.0, result.get(f1), DELTA),
                () -> assertEquals(22.0, result.get(f2), DELTA),
                () -> assertEquals(8.0, result.get(e1), DELTA),
                () -> assertEquals(8.0, result.get(e2), DELTA),
                () -> assertEquals(60.0, somma, DELTA));
    }

    @Test
    @DisplayName("3 FAST + 2 ECO su 60 kW: i FAST assorbono tutto (20 kW cad.), gli ECO vengono sospesi")
    void fastSaturanoReteEcoSospesi() {
        Colonnina f1 = creaColonninaAttiva("EV-01", ProfiloRicarica.FAST);
        Colonnina f2 = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);
        Colonnina f3 = creaColonninaAttiva("EV-03", ProfiloRicarica.FAST);
        Colonnina e1 = creaColonninaAttiva("EV-04", ProfiloRicarica.ECO);
        Colonnina e2 = creaColonninaAttiva("EV-05", ProfiloRicarica.ECO);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(
                List.of(f1, f2, f3, e1, e2), 60.0, 3.7);

        assertAll(
                () -> assertEquals(20.0, result.get(f1), DELTA),
                () -> assertEquals(20.0, result.get(f2), DELTA),
                () -> assertEquals(20.0, result.get(f3), DELTA),
                () -> assertEquals(0.0, result.get(e1), DELTA),
                () -> assertEquals(0.0, result.get(e2), DELTA));
    }

    @Test
    @DisplayName("residuo parziale per ECO: alimenta solo K_eco = floor(Pres/Pmin) colonnine ECO")
    void residuoParzialeSheddingSoloSuEco() {
        Colonnina f1 = creaColonninaAttiva("EV-01", ProfiloRicarica.FAST);
        Colonnina f2 = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);
        Colonnina e1 = creaColonninaAttiva("EV-03", ProfiloRicarica.ECO);
        Colonnina e2 = creaColonninaAttiva("EV-04", ProfiloRicarica.ECO);
        Colonnina e3 = creaColonninaAttiva("EV-05", ProfiloRicarica.ECO);

        // Pmax = 50 kW: 2 FAST prendono 44 kW, residuo = 6.0 kW; con Pmin = 3.7 kW -> 1 ECO a 6.0 kW, 2 ECO a 0.0
        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(
                List.of(f1, f2, e1, e2, e3), 50.0, 3.7);
        long ecoAttive = List.of(e1, e2, e3).stream().filter(c -> result.get(c) >= 3.7).count();
        long ecoSospese = List.of(e1, e2, e3).stream().filter(c -> result.get(c) == 0.0).count();

        assertAll(
                () -> assertEquals(22.0, result.get(f1), DELTA),
                () -> assertEquals(22.0, result.get(f2), DELTA),
                () -> assertEquals(1, ecoAttive),
                () -> assertEquals(2, ecoSospese),
                () -> assertEquals(50.0, result.values().stream().mapToDouble(Double::doubleValue).sum(), DELTA));
    }

    @Test
    @DisplayName("sovraccarico anche nel tier FAST: applica shedding sui FAST e sospende tutti gli ECO")
    void sovraccaricoNelTierFast() {
        Colonnina f1 = creaColonninaAttiva("EV-01", ProfiloRicarica.FAST);
        Colonnina f2 = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);
        Colonnina f3 = creaColonninaAttiva("EV-03", ProfiloRicarica.FAST);
        Colonnina e1 = creaColonninaAttiva("EV-04", ProfiloRicarica.ECO);

        // Pmax = 10.0, Pmin = 3.7 -> 2 FAST a 5.0 kW, 1 FAST e 1 ECO a 0.0 kW
        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(f1, f2, f3, e1), 10.0, 3.7);
        long fastAttive = List.of(f1, f2, f3).stream().filter(c -> result.get(c) >= 3.7).count();

        assertAll(
                () -> assertEquals(2, fastAttive),
                () -> assertEquals(0.0, result.get(e1), DELTA),
                () -> assertEquals(10.0, result.values().stream().mapToDouble(Double::doubleValue).sum(), DELTA));
    }

    @Test
    @DisplayName("con sole colonnine ECO usa l'intero budget rispettando il cap di 22 kW")
    void soloColonnineEco() {
        Colonnina e1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Colonnina e2 = creaColonninaAttiva("EV-02", ProfiloRicarica.ECO);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(e1, e2), 60.0, 3.7);

        assertAll(
                () -> assertEquals(22.0, result.get(e1), DELTA),
                () -> assertEquals(22.0, result.get(e2), DELTA));
    }

    @Test
    @DisplayName("con Pmax < Pmin sia FAST sia ECO ricevono 0.0 kW")
    void budgetInferioreASogliaMinima() {
        Colonnina f1 = creaColonninaAttiva("EV-01", ProfiloRicarica.FAST);
        Colonnina e1 = creaColonninaAttiva("EV-02", ProfiloRicarica.ECO);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(f1, e1), 2.0, 3.7);

        assertAll(
                () -> assertEquals(0.0, result.get(f1), DELTA),
                () -> assertEquals(0.0, result.get(e1), DELTA));
    }
}
