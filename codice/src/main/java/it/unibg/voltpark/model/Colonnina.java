package it.unibg.voltpark.model;

import it.unibg.voltpark.state.ColonninaContext;
import it.unibg.voltpark.state.StatoColonnina;
import it.unibg.voltpark.state.StatoLibera;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Colonnina implements ColonninaContext {
    private static final Logger logger = LogManager.getLogger(Colonnina.class);
    public static final double POTENZA_MASSIMA_NOMINALE_KW = 22.0;
    public static final double MINUTI_GRAZIA_DEFAULT = 15.0;

    private final String idColonnina;
    private StatoColonnina statoCorrente;
    private final List<GridEventListener> listeners = new ArrayList<>();

    private double potenzaAllocataKw;
    private double capacitaBatteriaKwh;
    private double socPercentuale;
    private double kwhTotaliErogati;
    private ProfiloRicarica profiloRicarica;
    private LocalDateTime timestampAvvioRicarica;
    private int minutiPostCarica;
    private int minutiOverstay;

    public Colonnina(String idColonnina) {
        this.idColonnina = idColonnina;
        this.statoCorrente = new StatoLibera();
        resetParametriSessione();
    }

    public void addGridEventListener(GridEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void configuraEAvviaSessione(double capacitaBatteriaKwh, double socIniziale, ProfiloRicarica profilo) {
        if (capacitaBatteriaKwh <= 0 || socIniziale < 0 || socIniziale >= 100) {
            throw new IllegalArgumentException("Parametri batteria non validi.");
        }
        this.capacitaBatteriaKwh = capacitaBatteriaKwh;
        this.socPercentuale = socIniziale;
        this.kwhTotaliErogati = 0.0;
        this.profiloRicarica = profilo;
        this.timestampAvvioRicarica = LocalDateTime.now();
        this.minutiPostCarica = 0;
        this.minutiOverstay = 0;
        this.statoCorrente.avviaRicarica(this);
    }

    public void avanzaSimulazione(int minuti) {
        this.statoCorrente.avanzaTempo(this, minuti, MINUTI_GRAZIA_DEFAULT);
    }

    public void chiudiSessione() {
        this.statoCorrente.terminaSessione(this);
        resetParametriSessione();
    }

    private void resetParametriSessione() {
        this.potenzaAllocataKw = 0.0;
        this.capacitaBatteriaKwh = 50.0;
        this.socPercentuale = 0.0;
        this.kwhTotaliErogati = 0.0;
        this.profiloRicarica = ProfiloRicarica.ECO;
        this.timestampAvvioRicarica = null;
        this.minutiPostCarica = 0;
        this.minutiOverstay = 0;
    }

    @Override
    public void incrementaEnergiaESoc(double kwhErogati) {
        if (kwhErogati <= 0) return;
        double kwhMancantiAl100 = capacitaBatteriaKwh * ((100.0 - socPercentuale) / 100.0);
        double kwhEffettivi = Math.min(kwhErogati, kwhMancantiAl100);
        this.kwhTotaliErogati += kwhEffettivi;
        this.socPercentuale = Math.min(100.0, this.socPercentuale + (kwhEffettivi / capacitaBatteriaKwh) * 100.0);
    }

    @Override
    public void notificaCambiamentoRete() {
        logger.info("Colonnina {} notifica cambiamento stato rete: {}", idColonnina, statoCorrente.getNomeStato());
        for (GridEventListener listener : listeners) {
            listener.onGridStateChanged(idColonnina, statoCorrente.getNomeStato());
        }
    }

    @Override
    public String getIdColonnina() { return idColonnina; }
    @Override
    public double getPotenzaAllocataKw() { return potenzaAllocataKw; }
    @Override
    public void setPotenzaAllocataKw(double kw) { this.potenzaAllocataKw = Math.max(0.0, Math.min(kw, POTENZA_MASSIMA_NOMINALE_KW)); }
    @Override
    public double getSocPercentuale() { return socPercentuale; }
    @Override
    public int getMinutiPostCarica() { return minutiPostCarica; }
    @Override
    public void incrementaMinutiPostCarica(int minuti) { this.minutiPostCarica += minuti; }
    @Override
    public int getMinutiOverstay() { return minutiOverstay; }
    @Override
    public void incrementaMinutiOverstay(int minuti) { this.minutiOverstay += minuti; }

    @Override
    public void setStatoCorrente(StatoColonnina nuovoStato) {
        logger.info("Transizione Colonnina {}: {} -> {}", idColonnina,
                this.statoCorrente.getNomeStato(), nuovoStato.getNomeStato());
        this.statoCorrente = nuovoStato;
    }

    public StatoColonnina getStatoCorrente() { return statoCorrente; }
    public double getKwhTotaliErogati() { return kwhTotaliErogati; }
    public ProfiloRicarica getProfiloRicarica() { return profiloRicarica; }
    public LocalDateTime getTimestampAvvioRicarica() { return timestampAvvioRicarica; }

    public boolean richiedePotenza() {
        String nome = statoCorrente.getNomeStato();
        return "IN_EROGAZIONE".equals(nome) || "SOSPESA_PER_SOVRACCARICO".equals(nome);
    }
}