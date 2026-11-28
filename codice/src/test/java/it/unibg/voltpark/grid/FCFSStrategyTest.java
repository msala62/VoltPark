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
 * Test di unita' per {@link FCFSStrategy}.
 */
@DisplayName("FCFSStrategy")
class FCFSStrategyTest {

    private static final double DELTA = 1e-6;
    private final FCFSStrategy strategy = new FCFSStrategy();

    private static Colonnina creaColonninaAttiva(String id, ProfiloRicarica profilo) {
        Colonnina c = new Colonnina(id);
        c.configuraEAvviaSessione(50.0, 20.0, profilo);
        return c;
    }

    @Test
    @DisplayName("nome strategia coerente e lista vuota/null restituisce mappa vuota")
    void nomeEListaVuota() {
        assertAll(
                () -> assertEquals("FCFS", strategy.getNomeStrategia()),
                () -> assertTrue(strategy.calcolaAllocazioni(List.of(), 60.0, 3.7).isEmpty()),
                () -> assertTrue(strategy.calcolaAllocazioni(null, 60.0, 3.7).isEmpty()));
    }

    @ParameterizedTest(name = "soglia minima {0} rifiutata")
    @ValueSource(doubles = { 0.0, -0.5, Double.NaN, Double.POSITIVE_INFINITY })
    void sogliaMinimaNonValida(double pMin) {
        assertThrows(IllegalArgumentException.class,
                () -> strategy.calcolaAllocazioni(List.of(), 60.0, pMin));
    }

    @Test
    @DisplayName("assegna fino a 22 kW in ordine di arrivo e sospende le colonnine oltre budget")
    void ordineCronologicoDiArrivo() throws InterruptedException {
        // EV-06 arriva PRIMA di EV-01: verifica che prevalga il timestamp sull'ID
        Colonnina prima = creaColonninaAttiva("EV-06", ProfiloRicarica.ECO);
        Thread.sleep(5);
        Colonnina seconda = creaColonninaAttiva("EV-02", ProfiloRicarica.FAST);
        Thread.sleep(5);
        Colonnina terza = creaColonninaAttiva("EV-03", ProfiloRicarica.ECO);
        Thread.sleep(5);
        Colonnina quarta = creaColonninaAttiva("EV-01", ProfiloRicarica.FAST);

        // Passate in ordine mescolato: FCFS deve servire prima -> seconda -> terza -> quarta
        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(
                List.of(quarta, terza, seconda, prima), 60.0, 3.7);

        assertAll(
                () -> assertEquals(22.0, result.get(prima), DELTA),
                () -> assertEquals(22.0, result.get(seconda), DELTA),
                () -> assertEquals(16.0, result.get(terza), DELTA),
                () -> assertEquals(0.0, result.get(quarta), DELTA));
    }

    @Test
    @DisplayName("non eroga mai potenza sub-soglia se il residuo e' inferiore a Pmin")
    void residuoSottoSogliaSospendeColonnina() throws InterruptedException {
        Colonnina c1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Thread.sleep(5);
        Colonnina c2 = creaColonninaAttiva("EV-02", ProfiloRicarica.ECO);
        Thread.sleep(5);
        Colonnina c3 = creaColonninaAttiva("EV-03", ProfiloRicarica.ECO);

        // Pmax = 47.0 kW: c1=22, c2=22, residuo=3.0 < 3.7 -> c3 sospesa (0.0 kW)
        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(c1, c2, c3), 47.0, 3.7);

        assertAll(
                () -> assertEquals(22.0, result.get(c1), DELTA),
                () -> assertEquals(22.0, result.get(c2), DELTA),
                () -> assertEquals(0.0, result.get(c3), DELTA));
    }

    @Test
    @DisplayName("assegna esattamente Pmin quando il residuo coincide con Pmin (tolleranza floating-point)")
    void residuoEsattamenteUgualeAPmin() throws InterruptedException {
        Colonnina c1 = creaColonninaAttiva("EV-01", ProfiloRicarica.ECO);
        Thread.sleep(5);
        Colonnina c2 = creaColonninaAttiva("EV-02", ProfiloRicarica.ECO);

        // 22.0 + 3.7 = 25.7 kW
        Map<Colonnina, Double> result = strategy.calcolaAllocazioni(List.of(c1, c2), 25.7, 3.7);

        assertAll(
                () -> assertEquals(22.0, result.get(c1), DELTA),
                () -> assertEquals(3.7, result.get(c2), DELTA));
    }
}
