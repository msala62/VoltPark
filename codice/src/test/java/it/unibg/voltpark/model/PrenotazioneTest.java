package it.unibg.voltpark.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test di unita' del record {@link Prenotazione}. */
@DisplayName("Prenotazione")
class PrenotazioneTest {

    private static final LocalDateTime CREAZIONE = LocalDateTime.of(2026, 11, 20, 9, 0);
    private static final LocalDateTime SCADENZA = CREAZIONE.plusMinutes(30);

    private final Prenotazione prenotazione =
            new Prenotazione("AB123CD", "EV-01", CREAZIONE, SCADENZA);

    @Test
    @DisplayName("non e' scaduta prima dell'istante di scadenza")
    void nonScadutaPrima() {
        assertFalse(prenotazione.isScaduta(SCADENZA.minusSeconds(1)));
    }

    @Test
    @DisplayName("non e' scaduta esattamente all'istante di scadenza")
    void nonScadutaAlLimite() {
        assertFalse(prenotazione.isScaduta(SCADENZA));
    }

    @Test
    @DisplayName("e' scaduta dopo l'istante di scadenza")
    void scadutaDopo() {
        assertTrue(prenotazione.isScaduta(SCADENZA.plusSeconds(1)));
    }

    @Test
    @DisplayName("due prenotazioni con gli stessi dati sono uguali (semantica del record)")
    void uguaglianzaRecord() {
        Prenotazione copia = new Prenotazione("AB123CD", "EV-01", CREAZIONE, SCADENZA);
        Prenotazione diversa = new Prenotazione("AB123CD", "EV-02", CREAZIONE, SCADENZA);

        assertEquals(prenotazione, copia);
        assertEquals(prenotazione.hashCode(), copia.hashCode());
        assertNotEquals(prenotazione, diversa);
    }
}
