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
 * Test di unita' per {@link EqualSplitStrategy}.
 */
@DisplayName("EqualSplitStrategy")
class EqualSplitStrategyTest {

    private static final double DELTA = 1e-6;
    private final EqualSplitStrategy strategy = new EqualSplitStrategy();

    private static Colonnina creaColonninaAttiva(String id, ProfiloRicarica profilo) {
        Colonnina c = new Colonnina(id);
        c.configuraEAvviaSessione(50.0, 20.0, profilo);
        return c;
    }

    @Test
    @DisplayName("nome strategia coerente e lista vuota/null restituisce mappa vuota")
    void nomeEListaVuota() {
        assertAll(
                () -> assertEquals("EQUAL_SPLIT", strategy.getNomeStrategia()),
                () -> assertTrue(strategy.calcolaAllocazioni(List.of(), 60.0, 3.7).isEmpty()),
                () -> assertTrue(strategy.calcolaAllocazioni(null, 60.0, 3.7).isEmpty()));
    }

    @ParameterizedTest(name = "soglia minima {0} rifiutata")
    @ValueSource(doubles = { 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY })
    void sogliaMinimaNonValida(double pMin) {
        assertThrows(IllegalArgumentException.class,
                () -> strategy.calcolaAllocazioni(List.of(), 60.0, pMin));
    }

    @Test
    @DisplayName("rispetta il cap nominale di 22 kW quando Pmax / N eccede 22 kW")
    void capNominale22Kw() {
        Colonnina c1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Colonnina c2 = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(c1, c2), 60.0, 3.7);

        assertAll(
                () -> assertEquals(22.0, result.get(c1), DELTA),
                () -> assertEquals(22.0, result.get(c2), DELTA));
    }

    @Test
    @DisplayName("divide equamente la potenza quando Pmax e' inferiore alla somma nominale")
    void ripartizioneEquaSottoPmax() {
        Colonnina c1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Colonnina c2 = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);
        Colonnina c3 = creaColonninaAttiva("EV-03", ProfiloRicarica.ECO);
        Colonnina c4 = creaColonninaAttiva("EV-04", ProfiloRicarica.FAST);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(c1, c2, c3, c4), 60.0, 3.7);
        double somma = result.values().stream().mapToDouble(Double::doubleValue).sum();

        assertAll(
                () -> assertEquals(15.0, result.get(c1), DELTA),
                () -> assertEquals(15.0, result.get(c2), DELTA),
                () -> assertEquals(15.0, result.get(c3), DELTA),
                () -> assertEquals(15.0, result.get(c4), DELTA),
                () -> assertEquals(60.0, somma, DELTA));
    }

    @Test
    @DisplayName("in sovraccarico (Pmax/N < Pmin) alimenta K = floor(Pmax/Pmin) colonnine e sospende le restanti")
    void sheddingDeterministicoSottoSoglia() {
        Colonnina c1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Colonnina c2 = creaColonninaAttiva("EV-02", ProfiloRicarica.ECO);
        Colonnina c3 = creaColonninaAttiva("EV-03", ProfiloRicarica.ECO);
        Colonnina c4 = creaColonninaAttiva("EV-04", ProfiloRicarica.ECO);

        // Pmax = 10.0, Pmin = 3.7 -> K = floor(10.0 / 3.7) = 2 colonnine a 5.0 kW, 2 sospese a 0.0 kW
        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(c1, c2, c3, c4), 10.0, 3.7);
        long attive = result.values().stream().filter(v -> v >= 3.7).count();
        long sospese = result.values().stream().filter(v -> v == 0.0).count();
        double somma = result.values().stream().mapToDouble(Double::doubleValue).sum();

        assertAll(
                () -> assertEquals(2, attive),
                () -> assertEquals(2, sospese),
                () -> assertEquals(10.0, somma, DELTA));
    }

    @Test
    @DisplayName("gestisce multiplo esatto di Pmin (3 x 3.7 = 11.1 kW) senza sospensioni spurie")
    void multiploEsattoSogliaMinima() {
        Colonnina c1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Colonnina c2 = creaColonninaAttiva("EV-02", ProfiloRicarica.ECO);
        Colonnina c3 = creaColonninaAttiva("EV-03", ProfiloRicarica.ECO);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(c1, c2, c3), 11.1, 3.7);

        assertAll(
                () -> assertEquals(3.7, result.get(c1), DELTA),
                () -> assertEquals(3.7, result.get(c2), DELTA),
                () -> assertEquals(3.7, result.get(c3), DELTA));
    }

    @Test
    @DisplayName("se Pmax < Pmin sospende tutte le colonnine assegnando 0.0 kW")
    void budgetInsufficientePerUnaColonnina() {
        Colonnina c1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Colonnina c2 = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);

        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(c1, c2), 2.0, 3.7);

        assertAll(
                () -> assertEquals(0.0, result.get(c1), DELTA),
                () -> assertEquals(0.0, result.get(c2), DELTA));
    }
}
