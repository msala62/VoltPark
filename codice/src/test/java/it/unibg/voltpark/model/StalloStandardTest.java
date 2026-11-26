package it.unibg.voltpark.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test di unita' per {@link StalloStandard}: proprieta' specifiche dello stallo
 * senza colonnina e ciclo di vita completo ereditato da {@link Stallo}.
 */
@DisplayName("StalloStandard")
class StalloStandardTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 24, 9, 0);
    private static final int VALIDITA_PRENOTAZIONE_MIN = 30;
    private static final String TARGA = "AB123CD";
    private static final double DELTA = 1e-9;

    private StalloStandard stallo;

    @BeforeEach
    void setUp() {
        stallo = new StalloStandard("STD-01");
    }

    @Test
    @DisplayName("un nuovo stallo standard e' uno Stallo LIBERO senza ricarica EV, a 1,50 euro/h")
    void costruzione() {
        assertAll(
                () -> assertInstanceOf(Stallo.class, stallo),
                () -> assertEquals("STD-01", stallo.getCodice()),
                () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                () -> assertFalse(stallo.supportaRicaricaEV()),
                () -> assertEquals(1.50, stallo.getTariffaOrariaSosta(), DELTA));
    }

    @ParameterizedTest(name = "codice [{0}] rifiutato")
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t" })
    void codiceNonValido(String codice) {
        assertThrows(IllegalArgumentException.class, () -> new StalloStandard(codice));
    }

    @Test
    @DisplayName("la tariffa standard e' piu' bassa di quella di uno stallo EV")
    void tariffaInferioreAEv() {
        StalloEV stalloEV = new StalloEV("EV-01", new Colonnina("COL-01"));

        assertTrue(stallo.getTariffaOrariaSosta() < stalloEV.getTariffaOrariaSosta());
    }

    @Test
    @DisplayName("ciclo completo: prenota, occupa, accumula sosta e libera")
    void cicloCompleto() {
        stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);
        assertEquals(StatoStallo.PRENOTATO, stallo.getStato());

        stallo.occupa(TARGA);
        stallo.avanzaTempoSosta(90);
        assertAll(
                () -> assertEquals(StatoStallo.OCCUPATO, stallo.getStato()),
                () -> assertEquals(TARGA, stallo.getTargaOccupante().orElseThrow()),
                () -> assertEquals(90, stallo.getMinutiSostaAccumulati()));

        stallo.libera();
        assertAll(
                () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                () -> assertTrue(stallo.getTargaOccupante().isEmpty()),
                () -> assertEquals(0, stallo.getMinutiSostaAccumulati()));
    }

    @Test
    @DisplayName("fuori servizio e rimessa in servizio senza colonnina da sincronizzare")
    void manutenzione() {
        stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

        stallo.mettiFuoriServizio();
        assertAll(
                () -> assertEquals(StatoStallo.FUORI_SERVIZIO, stallo.getStato()),
                () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()));

        stallo.rimettiInServizio();
        assertEquals(StatoStallo.LIBERO, stallo.getStato());
    }

    @Test
    @DisplayName("la prenotazione scaduta riporta lo stallo a LIBERO")
    void scadenzaPrenotazione() {
        stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

        assertTrue(stallo.verificaEScadutaPrenotazione(T0.plusMinutes(VALIDITA_PRENOTAZIONE_MIN + 1)));
        assertEquals(StatoStallo.LIBERO, stallo.getStato());
    }
}
