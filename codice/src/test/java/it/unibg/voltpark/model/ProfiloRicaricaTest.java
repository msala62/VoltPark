package it.unibg.voltpark.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Test di unita' dell'enum {@link ProfiloRicarica}: priorita' e tariffe da specifica. */
@DisplayName("ProfiloRicarica")
class ProfiloRicaricaTest {

    private static final double DELTA = 1e-9;

    @ParameterizedTest(name = "{0}: priorita' {1}, {2} EUR/kWh")
    @CsvSource({ "ECO, 1, 0.45", "FAST, 2, 0.65" })
    void valoriDaSpecifica(ProfiloRicarica profilo, int priorita, double costoKwh) {
        assertEquals(priorita, profilo.getLivelloPriorita());
        assertEquals(costoKwh, profilo.getCostoPerKwh(), DELTA);
    }

    @Test
    @DisplayName("FAST ha priorita' piu' alta di ECO nel bilanciamento")
    void fastPrioritarioSuEco() {
        assertTrue(ProfiloRicarica.FAST.getLivelloPriorita()
                > ProfiloRicarica.ECO.getLivelloPriorita());
    }

    @Test
    @DisplayName("esistono esattamente due profili")
    void dueProfili() {
        assertEquals(2, ProfiloRicarica.values().length);
    }
}
