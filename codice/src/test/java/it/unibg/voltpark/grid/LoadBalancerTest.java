package it.unibg.voltpark.grid;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import it.unibg.voltpark.model.Colonnina;
import it.unibg.voltpark.model.ProfiloRicarica;

/**
 * Test di unita' e integrazione per {@link LoadBalancer}: validazione parametri Pmax/Pmin,
 * pattern Observer con {@link Colonnina}, cambio strategia a runtime e rispetto invarianti.
 */
@DisplayName("LoadBalancer")
class LoadBalancerTest {

    private static final double DELTA = 1e-6;

    private LoadBalancer loadBalancer;

    @BeforeEach
    void setUp() {
        loadBalancer = new LoadBalancer();
    }

    @Nested
    @DisplayName("costruzione e validazione parametri")
    class ConfigurazioneEValidazione {

        @Test
        @DisplayName("costruttore di default imposta Pmax=60 kW, Pmin=3.7 kW ed EqualSplitStrategy")
        void valoriDefault() {
            assertAll(
                    () -> assertEquals(60.0, loadBalancer.getPotenzaMassimaKw(), DELTA),
                    () -> assertEquals(3.7, loadBalancer.getSogliaMinimaKw(), DELTA),
                    () -> assertEquals("EQUAL_SPLIT", loadBalancer.getStrategia().getNomeStrategia()),
                    () -> assertEquals(0.0, loadBalancer.getPotenzaTotaleAllocataKw(), DELTA),
                    () -> assertEquals(60.0, loadBalancer.getPotenzaDisponibileKw(), DELTA),
                    () -> assertTrue(loadBalancer.getColonnineRegistrate().isEmpty()));
        }

        @Test
        @DisplayName("rifiuta strategia null nel costruttore e nel setter")
        void strategiaNullRifiutata() {
            assertThrows(IllegalArgumentException.class,
                    () -> new LoadBalancer(null, 60.0, 3.7));
            assertThrows(IllegalArgumentException.class,
                    () -> loadBalancer.setStrategia(null));
        }

        @ParameterizedTest(name = "Pmin={0} kW fuori range [1.5, 11.0] rifiutata")
        @ValueSource(doubles = { 0.0, 1.49, 11.01, -3.0, Double.NaN, Double.POSITIVE_INFINITY })
        void sogliaMinimaFuoriRangeRifiutata(double pMinNonValida) {
            assertThrows(IllegalArgumentException.class,
                    () -> new LoadBalancer(new EqualSplitStrategy(), 60.0, pMinNonValida));
            assertThrows(IllegalArgumentException.class,
                    () -> loadBalancer.setSogliaMinimaKw(pMinNonValida));
        }

        @Test
        @DisplayName("rifiuta Pmax < Pmin e Pmin > Pmax")
        void vincoloIncrociatoPmaxPmin() {
            LoadBalancer lb = new LoadBalancer(new EqualSplitStrategy(), 8.0, 5.0);

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new LoadBalancer(new EqualSplitStrategy(), 3.0, 3.7)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> lb.setPotenzaMassimaKw(4.9)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> lb.setPotenzaMassimaKw(Double.NaN)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> lb.setSogliaMinimaKw(8.1)));
        }

        @Test
        @DisplayName("registraColonnina rifiuta null, evita duplicati ed espone lista non modificabile")
        void registrazioneColonnine() {
            Colonnina c1 = new Colonnina("EV-01");

            assertThrows(IllegalArgumentException.class, () -> loadBalancer.registraColonnina(null));

            loadBalancer.registraColonnina(c1);
            loadBalancer.registraColonnina(c1);

            assertEquals(1, loadBalancer.getColonnineRegistrate().size());
            assertThrows(UnsupportedOperationException.class,
                    () -> loadBalancer.getColonnineRegistrate().add(new Colonnina("EV-02")));
        }
    }

    @Nested
    @DisplayName("Observer e bilanciamento dinamico")
    class ObserverEBilanciamento {

        @Test
        @DisplayName("avvio ricariche innesca automaticamente il ribilanciamento EqualSplit via Observer")
        void avvioRicaricheTriggerAutomatico() {
            Colonnina c1 = new Colonnina("EV-01");
            Colonnina c2 = new Colonnina("EV-02");
            Colonnina c3 = new Colonnina("EV-03");
            loadBalancer.registraColonnina(c1);
            loadBalancer.registraColonnina(c2);
            loadBalancer.registraColonnina(c3);

            c1.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.ECO);
            assertEquals(22.0, c1.getPotenzaAllocataKw(), DELTA);

            c2.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.FAST);
            assertAll(
                    () -> assertEquals(22.0, c1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(22.0, c2.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(44.0, loadBalancer.getPotenzaTotaleAllocataKw(), DELTA),
                    () -> assertEquals(16.0, loadBalancer.getPotenzaDisponibileKw(), DELTA));

            c3.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.ECO);
            assertAll(
                    () -> assertEquals(20.0, c1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(20.0, c2.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(20.0, c3.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(60.0, loadBalancer.getPotenzaTotaleAllocataKw(), DELTA),
                    () -> assertEquals(3, loadBalancer.getNumeroColonnineInErogazione()),
                    () -> assertEquals(0, loadBalancer.getNumeroColonnineSospese()));
        }

        @Test
        @DisplayName("cambio strategia a runtime sospende gli ECO in PriorityTier e li riattiva in EqualSplit")
        void cambioStrategiaARuntime() {
            Colonnina f1 = new Colonnina("EV-01");
            Colonnina f2 = new Colonnina("EV-02");
            Colonnina f3 = new Colonnina("EV-03");
            Colonnina e1 = new Colonnina("EV-04");
            for (Colonnina c : new Colonnina[] { f1, f2, f3, e1 }) {
                loadBalancer.registraColonnina(c);
            }

            f1.configuraEAvviaSessione(50.0, 10.0, ProfiloRicarica.FAST);
            f2.configuraEAvviaSessione(50.0, 10.0, ProfiloRicarica.FAST);
            f3.configuraEAvviaSessione(50.0, 10.0, ProfiloRicarica.FAST);
            e1.configuraEAvviaSessione(50.0, 10.0, ProfiloRicarica.ECO);

            // EqualSplit: 4 x 15 kW = 60 kW
            assertEquals(15.0, e1.getPotenzaAllocataKw(), DELTA);

            // Switch a PriorityTier: 3 FAST a 20 kW, e1 sospesa a 0 kW
            loadBalancer.setStrategia(new PriorityTierStrategy());
            assertAll(
                    () -> assertEquals(20.0, f1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(20.0, f2.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(20.0, f3.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(0.0, e1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals("SOSPESA_PER_SOVRACCARICO", e1.getStatoCorrente().getNomeStato()),
                    () -> assertEquals(3, loadBalancer.getNumeroColonnineInErogazione()),
                    () -> assertEquals(1, loadBalancer.getNumeroColonnineSospese()));

            // Switch di ritorno a EqualSplit: e1 si riattiva automaticamente
            loadBalancer.setStrategia(new EqualSplitStrategy());
            assertAll(
                    () -> assertEquals("IN_EROGAZIONE", e1.getStatoCorrente().getNomeStato()),
                    () -> assertEquals(15.0, e1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(4, loadBalancer.getNumeroColonnineInErogazione()),
                    () -> assertEquals(0, loadBalancer.getNumeroColonnineSospese()));
        }

        @Test
        @DisplayName("al raggiungimento del 100% di SoC una colonnina sospesa viene riattivata automaticamente")
        void riattivazioneAutomaticaAlCompletamentoCarica() {
            loadBalancer.setStrategia(new PriorityTierStrategy());

            Colonnina f1 = new Colonnina("EV-01");
            Colonnina f2 = new Colonnina("EV-02");
            Colonnina f3 = new Colonnina("EV-03");
            Colonnina e1 = new Colonnina("EV-04");
            for (Colonnina c : new Colonnina[] { f1, f2, f3, e1 }) {
                loadBalancer.registraColonnina(c);
            }

            // f1 quasi carica (95% su 50 kWh -> servono 2.5 kWh)
            f1.configuraEAvviaSessione(50.0, 95.0, ProfiloRicarica.FAST);
            f2.configuraEAvviaSessione(50.0, 10.0, ProfiloRicarica.FAST);
            f3.configuraEAvviaSessione(50.0, 10.0, ProfiloRicarica.FAST);
            e1.configuraEAvviaSessione(50.0, 10.0, ProfiloRicarica.ECO);

            assertEquals("SOSPESA_PER_SOVRACCARICO", e1.getStatoCorrente().getNomeStato());

            // Avanzando 15 minuti a 20 kW -> 5 kWh prodotti -> f1 completa la carica e notifica la rete
            f1.avanzaSimulazione(15);

            assertAll(
                    () -> assertEquals("CARICA_COMPLETATA", f1.getStatoCorrente().getNomeStato()),
                    () -> assertEquals(0.0, f1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(22.0, f2.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(22.0, f3.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals("IN_EROGAZIONE", e1.getStatoCorrente().getNomeStato()),
                    () -> assertEquals(16.0, e1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals(60.0, loadBalancer.getPotenzaTotaleAllocataKw(), DELTA));
        }

        @Test
        @DisplayName("variazione di Pmax e Pmin a runtime causa sospensione sotto soglia e successiva ripresa")
        void modificaPmaxEPminARuntime() {
            Colonnina c1 = new Colonnina("EV-01");
            Colonnina c2 = new Colonnina("EV-02");
            Colonnina c3 = new Colonnina("EV-03");
            loadBalancer.registraColonnina(c1);
            loadBalancer.registraColonnina(c2);
            loadBalancer.registraColonnina(c3);

            c1.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.ECO);
            c2.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.ECO);
            c3.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.ECO);

            // Abbassiamo Pmax a 15 kW -> 5 kW ciascuna (>= 3.7)
            loadBalancer.setPotenzaMassimaKw(15.0);
            assertAll(
                    () -> assertEquals(3, loadBalancer.getNumeroColonnineInErogazione()),
                    () -> assertEquals(5.0, c1.getPotenzaAllocataKw(), DELTA));

            // Alziamo Pmin a 6.0 kW -> 15 / 3 = 5.0 < 6.0 -> solo K=2 servite a 7.5 kW, 1 sospesa!
            loadBalancer.setSogliaMinimaKw(6.0);
            assertAll(
                    () -> assertEquals(2, loadBalancer.getNumeroColonnineInErogazione()),
                    () -> assertEquals(1, loadBalancer.getNumeroColonnineSospese()),
                    () -> assertEquals(15.0, loadBalancer.getPotenzaTotaleAllocataKw(), DELTA),
                    () -> assertTrue(loadBalancer.getColonnineRegistrate().stream()
                            .allMatch(c -> c.getPotenzaAllocataKw() == 0.0 || c.getPotenzaAllocataKw() >= 6.0)));

            // Abbassiamo Pmin a 3.7 kW -> la colonnina sospesa riparte automaticamente
            loadBalancer.setSogliaMinimaKw(3.7);
            assertAll(
                    () -> assertEquals(3, loadBalancer.getNumeroColonnineInErogazione()),
                    () -> assertEquals(0, loadBalancer.getNumeroColonnineSospese()),
                    () -> assertEquals(5.0, c1.getPotenzaAllocataKw(), DELTA));
        }

        @Test
        @DisplayName("chiusura sessione o manutenzione durante erogazione libera potenza per le altre colonnine")
        void chiusuraEManutenzioneRibilanciano() {
            loadBalancer.setStrategia(new FCFSStrategy());
            loadBalancer.setPotenzaMassimaKw(25.0);

            Colonnina c1 = new Colonnina("EV-01");
            Colonnina c2 = new Colonnina("EV-02");
            loadBalancer.registraColonnina(c1);
            loadBalancer.registraColonnina(c2);

            c1.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.ECO);
            c2.configuraEAvviaSessione(50.0, 20.0, ProfiloRicarica.ECO);

            // FCFS su 25 kW con Pmin=3.7: c1=22 kW, residuo=3.0 < 3.7 -> c2 sospesa
            assertAll(
                    () -> assertEquals(22.0, c1.getPotenzaAllocataKw(), DELTA),
                    () -> assertEquals("SOSPESA_PER_SOVRACCARICO", c2.getStatoCorrente().getNomeStato()));

            // Messa in manutenzione di c1 libera tutta la rete -> c2 sale a 22 kW
            c1.getStatoCorrente().impostaManutenzione(c1);
            assertAll(
                    () -> assertEquals("FUORI_SERVIZIO", c1.getStatoCorrente().getNomeStato()),
                    () -> assertEquals("IN_EROGAZIONE", c2.getStatoCorrente().getNomeStato()),
                    () -> assertEquals(22.0, c2.getPotenzaAllocataKw(), DELTA));

            // Chiusura sessione di c2 porta il carico totale a 0.0 kW
            c2.chiudiSessione();
            assertAll(
                    () -> assertEquals(0.0, loadBalancer.getPotenzaTotaleAllocataKw(), DELTA),
                    () -> assertEquals(25.0, loadBalancer.getPotenzaDisponibileKw(), DELTA),
                    () -> assertEquals(0, loadBalancer.getNumeroColonnineInErogazione()));
        }
    }
}
