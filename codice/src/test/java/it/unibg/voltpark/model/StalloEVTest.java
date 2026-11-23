package it.unibg.voltpark.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test di unita' per {@link StalloEV}: coerenza delle transizioni di stato tra
 * stallo e colonnina associata, inclusa la gestione di manutenzione.
 */
@DisplayName("StalloEV")
class StalloEVTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 9, 0);
    private static final String TARGA = "EV123AB";
    private static final String ALTRA_TARGA = "ZZ999ZZ";

    private Colonnina colonnina;
    private StalloEV stalloEV;

    @BeforeEach
    void setUp() {
        colonnina = new Colonnina("COL-01");
        stalloEV = new StalloEV("EV-01", colonnina);
    }

    @Test
    @DisplayName("costruttore rifiuta colonnina null e imposta proprieta' EV")
    void costruzione() {
        assertThrows(IllegalArgumentException.class, () -> new StalloEV("EV-02", null));
        assertAll(
                () -> assertTrue(stalloEV.supportaRicaricaEV()),
                () -> assertEquals(2.00, stalloEV.getTariffaOrariaSosta(), 1e-9),
                () -> assertEquals(colonnina, stalloEV.getColonnina()));
    }

    @Test
    @DisplayName("mettiFuoriServizio() sincronizza stallo e colonnina in FUORI_SERVIZIO")
    void mettiFuoriServizioSincronizzaColonnina() {
        stalloEV.mettiFuoriServizio();

        assertAll(
                () -> assertEquals(StatoStallo.FUORI_SERVIZIO, stalloEV.getStato()),
                () -> assertEquals("FUORI_SERVIZIO", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("mettiFuoriServizio() su stallo occupato fallisce senza alterare la colonnina")
    void mettiFuoriServizioSuOccupatoNonAlteraColonnina() {
        stalloEV.occupa(TARGA);
        colonnina.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.FAST);

        assertThrows(IllegalStateException.class, () -> stalloEV.mettiFuoriServizio());
        assertAll(
                () -> assertEquals(StatoStallo.OCCUPATO, stalloEV.getStato()),
                () -> assertEquals("IN_EROGAZIONE", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("libera() su StalloEV FUORI_SERVIZIO lancia IllegalStateException e non ripristina la colonnina")
    void liberaFuoriServizioNonRipristinaColonnina() {
        stalloEV.mettiFuoriServizio();

        assertThrows(IllegalStateException.class, () -> stalloEV.libera());
        assertAll(
                () -> assertEquals(StatoStallo.FUORI_SERVIZIO, stalloEV.getStato()),
                () -> assertEquals("FUORI_SERVIZIO", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("rimettiInServizio() riporta stallo a LIBERO e colonnina a LIBERA")
    void rimettiInServizioSincronizzaColonnina() {
        stalloEV.mettiFuoriServizio();

        stalloEV.rimettiInServizio();

        assertAll(
                () -> assertEquals(StatoStallo.LIBERO, stalloEV.getStato()),
                () -> assertEquals("LIBERA", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("prenota() con targa non valida fallisce senza prenotare la colonnina")
    void prenotaTargaNonValidaNonAlteraColonnina() {
        assertThrows(IllegalArgumentException.class, () -> stalloEV.prenota("  ", T0, 30));
        assertAll(
                () -> assertEquals(StatoStallo.LIBERO, stalloEV.getStato()),
                () -> assertEquals("LIBERA", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("prenota() e libera() su stallo attivo sincronizzano stallo e colonnina")
    void prenotaELiberaSincronizzanoColonnina() {
        stalloEV.prenota(TARGA, T0, 30);
        assertAll(
                () -> assertEquals(StatoStallo.PRENOTATO, stalloEV.getStato()),
                () -> assertEquals("PRENOTATA", colonnina.getStatoCorrente().getNomeStato()));

        stalloEV.libera();
        assertAll(
                () -> assertEquals(StatoStallo.LIBERO, stalloEV.getStato()),
                () -> assertEquals("LIBERA", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("la scadenza della prenotazione libera anche la colonnina")
    void scadenzaPrenotazioneLiberaColonnina() {
        stalloEV.prenota(TARGA, T0, 30);

        assertTrue(stalloEV.verificaEScadutaPrenotazione(T0.plusMinutes(31)));
        assertAll(
                () -> assertEquals(StatoStallo.LIBERO, stalloEV.getStato()),
                () -> assertEquals("LIBERA", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("dopo una prenotazione scaduta lo stallo si puo' prenotare di nuovo")
    void nuovaPrenotazioneDopoScadenza() {
        stalloEV.prenota(TARGA, T0, 30);
        stalloEV.verificaEScadutaPrenotazione(T0.plusMinutes(31));

        stalloEV.prenota(ALTRA_TARGA, T0.plusMinutes(32), 30);

        assertAll(
                () -> assertEquals(StatoStallo.PRENOTATO, stalloEV.getStato()),
                () -> assertEquals(ALTRA_TARGA,
                        stalloEV.getPrenotazioneAttiva().orElseThrow().targaVeicolo()),
                () -> assertEquals("PRENOTATA", colonnina.getStatoCorrente().getNomeStato()));
    }

    @Test
    @DisplayName("prima della scadenza la colonnina resta PRENOTATA")
    void prenotazioneNonScadutaNonToccaColonnina() {
        stalloEV.prenota(TARGA, T0, 30);

        assertFalse(stalloEV.verificaEScadutaPrenotazione(T0.plusMinutes(29)));
        assertEquals("PRENOTATA", colonnina.getStatoCorrente().getNomeStato());
    }

    @Test
    @DisplayName("se la colonnina rifiuta la prenotazione, lo stallo resta LIBERO")
    void prenotaRifiutataDallaColonninaNonAlteraStallo() {
        colonnina.getStatoCorrente().impostaManutenzione(colonnina);

        assertThrows(IllegalStateException.class, () -> stalloEV.prenota(TARGA, T0, 30));
        assertAll(
                () -> assertEquals(StatoStallo.LIBERO, stalloEV.getStato()),
                () -> assertTrue(stalloEV.getPrenotazioneAttiva().isEmpty()),
                () -> assertEquals("FUORI_SERVIZIO", colonnina.getStatoCorrente().getNomeStato()));
    }
}
