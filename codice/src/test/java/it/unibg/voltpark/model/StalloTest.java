package it.unibg.voltpark.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test di unita' della classe astratta {@link Stallo}: costruzione,
 * prenotazione con scadenza, occupazione, rilascio e conteggio della sosta.
 */
@DisplayName("Stallo")
class StalloTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 9, 0);
    private static final int VALIDITA_PRENOTAZIONE_MIN = 30;
    private static final String TARGA = "AB123CD";
    private static final String ALTRA_TARGA = "ZZ999ZZ";

    /** Sottoclasse minima: serve solo a istanziare la classe astratta. */
    private static final class StalloDiProva extends Stallo {
        StalloDiProva(String codice) {
            super(codice);
        }

        @Override
        public boolean supportaRicaricaEV() {
            return false;
        }

        @Override
        public double getTariffaOrariaSosta() {
            return 1.50;
        }
    }

    private Stallo stallo;

    @BeforeEach
    void setUp() {
        stallo = new StalloDiProva("STD-01");
    }

    /** Porta lo stallo nello stato desiderato solo tramite transizioni di dominio valide. */
    private void portaInStato(StatoStallo target) {
        switch (target) {
            case LIBERO -> { /* stato iniziale */ }
            case PRENOTATO -> stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);
            case OCCUPATO -> stallo.occupa(TARGA);
            case FUORI_SERVIZIO -> stallo.mettiFuoriServizio();
        }
    }

    @Nested
    @DisplayName("costruzione")
    class Costruzione {

        @Test
        @DisplayName("un nuovo stallo e' LIBERO, senza occupante, prenotazione o sosta")
        void statoIniziale() {
            assertAll(
                    () -> assertEquals("STD-01", stallo.getCodice()),
                    () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                    () -> assertTrue(stallo.getTargaOccupante().isEmpty()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()),
                    () -> assertEquals(0, stallo.getMinutiSostaAccumulati()));
        }

        @ParameterizedTest(name = "codice [{0}] rifiutato")
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void codiceNonValido(String codice) {
            assertThrows(IllegalArgumentException.class, () -> new StalloDiProva(codice));
        }
    }

    @Nested
    @DisplayName("prenotazione")
    class Prenotazioni {

        @Test
        @DisplayName("da LIBERO passa a PRENOTATO e registra la prenotazione")
        void prenotaStalloLibero() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            Prenotazione p = stallo.getPrenotazioneAttiva().orElseThrow();
            assertAll(
                    () -> assertEquals(StatoStallo.PRENOTATO, stallo.getStato()),
                    () -> assertEquals(TARGA, p.targaVeicolo()),
                    () -> assertEquals("STD-01", p.codiceStallo()),
                    () -> assertEquals(T0, p.istanteCreazione()),
                    () -> assertEquals(T0.plusMinutes(30), p.istanteScadenza()));
        }

        @ParameterizedTest(name = "rifiutata se lo stallo e' {0}")
        @EnumSource(value = StatoStallo.class, names = "LIBERO", mode = EnumSource.Mode.EXCLUDE)
        void prenotaStalloNonLibero(StatoStallo stato) {
            portaInStato(stato);

            assertThrows(IllegalStateException.class,
                    () -> stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN));
            assertEquals(stato, stallo.getStato());
        }

        @ParameterizedTest(name = "targa [{0}] rifiutata")
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void prenotaConTargaNonValida(String targaNonValida) {
            assertThrows(IllegalArgumentException.class,
                    () -> stallo.prenota(targaNonValida, T0, VALIDITA_PRENOTAZIONE_MIN));
            assertAll(
                    () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()));
        }

        @Test
        @DisplayName("rifiutata se l'istante di prenotazione e' null")
        void prenotaConIstanteNullo() {
            assertThrows(IllegalArgumentException.class,
                    () -> stallo.prenota(TARGA, null, VALIDITA_PRENOTAZIONE_MIN));
            assertEquals(StatoStallo.LIBERO, stallo.getStato());
        }

        @ParameterizedTest(name = "validita' di {0} minuti rifiutata")
        @ValueSource(ints = { 0, -10 })
        void prenotaConValiditaNonPositiva(int minutiValidita) {
            assertThrows(IllegalArgumentException.class,
                    () -> stallo.prenota(TARGA, T0, minutiValidita));
            assertEquals(StatoStallo.LIBERO, stallo.getStato());
        }

        @Test
        @DisplayName("prima della scadenza resta PRENOTATO")
        void nonScadutaPrimaDelTermine() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            assertFalse(stallo.verificaEScadutaPrenotazione(T0.plusMinutes(29)));
            assertEquals(StatoStallo.PRENOTATO, stallo.getStato());
        }

        @Test
        @DisplayName("esattamente allo scadere non e' ancora scaduta")
        void nonScadutaAlLimite() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            assertFalse(stallo.verificaEScadutaPrenotazione(T0.plusMinutes(30)));
            assertEquals(StatoStallo.PRENOTATO, stallo.getStato());
        }

        @Test
        @DisplayName("dopo la scadenza lo stallo torna LIBERO")
        void scadutaTornaLibero() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            assertTrue(stallo.verificaEScadutaPrenotazione(T0.plusMinutes(31)));
            assertAll(
                    () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()));
        }

        @Test
        @DisplayName("senza prenotazione la verifica non cambia nulla")
        void verificaSenzaPrenotazione() {
            assertFalse(stallo.verificaEScadutaPrenotazione(T0));
            assertEquals(StatoStallo.LIBERO, stallo.getStato());
        }

        @Test
        @DisplayName("uno stallo OCCUPATO non viene liberato dalla verifica di scadenza")
        void verificaSuStalloOccupato() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);
            stallo.occupa(TARGA);

            assertFalse(stallo.verificaEScadutaPrenotazione(T0.plusHours(2)));
            assertEquals(StatoStallo.OCCUPATO, stallo.getStato());
        }
    }

    @Nested
    @DisplayName("occupazione")
    class Occupazione {

        @Test
        @DisplayName("da LIBERO passa a OCCUPATO con la targa indicata")
        void occupaStalloLibero() {
            stallo.occupa(TARGA);

            assertAll(
                    () -> assertEquals(StatoStallo.OCCUPATO, stallo.getStato()),
                    () -> assertEquals(TARGA, stallo.getTargaOccupante().orElseThrow()),
                    () -> assertEquals(0, stallo.getMinutiSostaAccumulati()));
        }

        @Test
        @DisplayName("la targa che ha prenotato puo' occupare (confronto senza maiuscole/minuscole)")
        void occupaConPrenotazionePropria() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            stallo.occupa(TARGA.toLowerCase());

            assertAll(
                    () -> assertEquals(StatoStallo.OCCUPATO, stallo.getStato()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()));
        }

        @Test
        @DisplayName("una targa diversa da quella prenotata viene rifiutata")
        void occupaConPrenotazioneAltrui() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            assertThrows(IllegalStateException.class, () -> stallo.occupa(ALTRA_TARGA));
            assertAll(
                    () -> assertEquals(StatoStallo.PRENOTATO, stallo.getStato()),
                    () -> assertTrue(stallo.getTargaOccupante().isEmpty()));
        }

        @ParameterizedTest(name = "rifiutata se lo stallo e' {0}")
        @EnumSource(value = StatoStallo.class, names = { "OCCUPATO", "FUORI_SERVIZIO" })
        void occupaStalloNonDisponibile(StatoStallo stato) {
            portaInStato(stato);

            assertThrows(IllegalStateException.class, () -> stallo.occupa(TARGA));
            assertEquals(stato, stallo.getStato());
        }

        @ParameterizedTest(name = "targa [{0}] rifiutata su stallo LIBERO")
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void occupaConTargaNonValidaDaLibero(String targaNonValida) {
            assertThrows(IllegalArgumentException.class, () -> stallo.occupa(targaNonValida));
            assertAll(
                    () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                    () -> assertTrue(stallo.getTargaOccupante().isEmpty()));
        }

        @ParameterizedTest(name = "targa [{0}] rifiutata su stallo PRENOTATO")
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void occupaConTargaNonValidaDaPrenotato(String targaNonValida) {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            assertThrows(IllegalArgumentException.class, () -> stallo.occupa(targaNonValida));
            assertAll(
                    () -> assertEquals(StatoStallo.PRENOTATO, stallo.getStato()),
                    () -> assertTrue(stallo.getTargaOccupante().isEmpty()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isPresent()));
        }
    }

    @Nested
    @DisplayName("sosta e rilascio")
    class SostaERilascio {

        @Test
        @DisplayName("i minuti si accumulano solo mentre lo stallo e' OCCUPATO")
        void accumuloMinuti() {
            stallo.avanzaTempoSosta(10);
            assertEquals(0, stallo.getMinutiSostaAccumulati());

            stallo.occupa(TARGA);
            stallo.avanzaTempoSosta(10);
            stallo.avanzaTempoSosta(25);

            assertEquals(35, stallo.getMinutiSostaAccumulati());
        }

        @ParameterizedTest(name = "avanzamento di {0} minuti ignorato")
        @ValueSource(ints = { 0, -5 })
        void avanzamentoNonPositivoIgnorato(int minuti) {
            stallo.occupa(TARGA);
            stallo.avanzaTempoSosta(minuti);

            assertEquals(0, stallo.getMinutiSostaAccumulati());
        }

        @Test
        @DisplayName("libera() azzera occupante, sosta e riporta a LIBERO")
        void liberaStalloOccupato() {
            stallo.occupa(TARGA);
            stallo.avanzaTempoSosta(40);

            stallo.libera();

            assertAll(
                    () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                    () -> assertTrue(stallo.getTargaOccupante().isEmpty()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()),
                    () -> assertEquals(0, stallo.getMinutiSostaAccumulati()));
        }

        @Test
        @DisplayName("libera() annulla anche una prenotazione attiva")
        void liberaStalloPrenotato() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            stallo.libera();

            assertAll(
                    () -> assertEquals(StatoStallo.LIBERO, stallo.getStato()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()));
        }

        @Test
        @DisplayName("libera() rifiutata se lo stallo e' FUORI_SERVIZIO")
        void liberaStalloFuoriServizio() {
            stallo.mettiFuoriServizio();

            assertThrows(IllegalStateException.class, () -> stallo.libera());
            assertEquals(StatoStallo.FUORI_SERVIZIO, stallo.getStato());
        }

        @Test
        @DisplayName("dopo il rilascio lo stallo puo' essere rioccupato da un'altra targa")
        void rioccupazioneDopoRilascio() {
            stallo.occupa(TARGA);
            stallo.libera();

            stallo.occupa(ALTRA_TARGA);

            assertSame(StatoStallo.OCCUPATO, stallo.getStato());
            assertEquals(ALTRA_TARGA, stallo.getTargaOccupante().orElseThrow());
        }
    }

    @Nested
    @DisplayName("manutenzione")
    class Manutenzione {

        @Test
        @DisplayName("mettiFuoriServizio() porta uno stallo LIBERO in FUORI_SERVIZIO")
        void mettiFuoriServizioDaLibero() {
            stallo.mettiFuoriServizio();

            assertEquals(StatoStallo.FUORI_SERVIZIO, stallo.getStato());
        }

        @Test
        @DisplayName("mettiFuoriServizio() annulla l'eventuale prenotazione attiva")
        void mettiFuoriServizioDaPrenotato() {
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);

            stallo.mettiFuoriServizio();

            assertAll(
                    () -> assertEquals(StatoStallo.FUORI_SERVIZIO, stallo.getStato()),
                    () -> assertTrue(stallo.getPrenotazioneAttiva().isEmpty()));
        }

        @Test
        @DisplayName("mettiFuoriServizio() su stallo gia' FUORI_SERVIZIO e' idempotente")
        void mettiFuoriServizioIdempotente() {
            stallo.mettiFuoriServizio();
            stallo.mettiFuoriServizio();

            assertEquals(StatoStallo.FUORI_SERVIZIO, stallo.getStato());
        }

        @Test
        @DisplayName("mettiFuoriServizio() rifiutata se lo stallo e' OCCUPATO")
        void mettiFuoriServizioDaOccupatoRifiutato() {
            stallo.occupa(TARGA);

            assertThrows(IllegalStateException.class, () -> stallo.mettiFuoriServizio());
            assertAll(
                    () -> assertEquals(StatoStallo.OCCUPATO, stallo.getStato()),
                    () -> assertEquals(TARGA, stallo.getTargaOccupante().orElseThrow()));
        }

        @Test
        @DisplayName("rimettiInServizio() riporta in LIBERO uno stallo FUORI_SERVIZIO")
        void rimettiInServizioDaFuoriServizio() {
            stallo.mettiFuoriServizio();

            stallo.rimettiInServizio();

            assertEquals(StatoStallo.LIBERO, stallo.getStato());
            stallo.prenota(TARGA, T0, VALIDITA_PRENOTAZIONE_MIN);
            assertEquals(StatoStallo.PRENOTATO, stallo.getStato());
        }

        @ParameterizedTest(name = "rimettiInServizio() rifiutata se lo stallo e' {0}")
        @EnumSource(value = StatoStallo.class, names = "FUORI_SERVIZIO", mode = EnumSource.Mode.EXCLUDE)
        void rimettiInServizioDaStatoNonFuoriServizio(StatoStallo stato) {
            portaInStato(stato);

            assertThrows(IllegalStateException.class, () -> stallo.rimettiInServizio());
            assertEquals(stato, stallo.getStato());
        }
    }
}
