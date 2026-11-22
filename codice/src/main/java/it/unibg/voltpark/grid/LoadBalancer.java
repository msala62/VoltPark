package it.unibg.voltpark.grid;

import it.unibg.voltpark.model.Colonnina;
import it.unibg.voltpark.model.GridEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Motore centrale di Smart Grid Load Balancing.
 * Svolge il doppio ruolo di Observer ({@link GridEventListener}) verso le colonnine EV
 * e di Context dello Strategy Pattern ({@link LoadBalancingStrategy}), garantendo a
 * runtime i vincoli Pmax, Pmin in [1.5, 11.0] kW e l'assenza di erogazioni sub-soglia.
 */
public class LoadBalancer implements GridEventListener {

    private static final Logger logger = LogManager.getLogger(LoadBalancer.class);
    private static final double EPSILON = 1e-9;

    public static final double POTENZA_MASSIMA_DEFAULT_KW = 60.0;
    public static final double SOGLIA_MINIMA_DEFAULT_KW = 3.7;
    public static final double SOGLIA_MINIMA_MIN_KW = 1.5;
    public static final double SOGLIA_MINIMA_MAX_KW = 11.0;

    private final List<Colonnina> colonnine = new ArrayList<>();
    private LoadBalancingStrategy strategia;
    private double potenzaMassimaKw;
    private double sogliaMinimaKw;

    public LoadBalancer() {
        this(new EqualSplitStrategy(), POTENZA_MASSIMA_DEFAULT_KW, SOGLIA_MINIMA_DEFAULT_KW);
    }

    public LoadBalancer(LoadBalancingStrategy strategia, double potenzaMassimaKw, double sogliaMinimaKw) {
        if (strategia == null) {
            throw new IllegalArgumentException("La strategia di bilanciamento non puo' essere nulla.");
        }
        validaParametriRete(potenzaMassimaKw, sogliaMinimaKw);
        this.strategia = strategia;
        this.potenzaMassimaKw = potenzaMassimaKw;
        this.sogliaMinimaKw = sogliaMinimaKw;
    }

    public void registraColonnina(Colonnina colonnina) {
        if (colonnina == null) {
            throw new IllegalArgumentException("La colonnina da registrare non puo' essere nulla.");
        }
        if (!colonnine.contains(colonnina)) {
            colonnine.add(colonnina);
            colonnina.addGridEventListener(this);
            logger.info("Colonnina {} registrata nel LoadBalancer.", colonnina.getIdColonnina());
        }
    }

    @Override
    public void onGridStateChanged(String idColonnina, String motivo) {
        logger.info("Evento rete ricevuto da Colonnina {} (stato={}): avvio ribilanciamento.",
                idColonnina, motivo);
        ribilancia();
    }

    public void ribilancia() {
        List<Colonnina> richiedenti = colonnine.stream()
                .filter(Colonnina::richiedePotenza)
                .toList();

        if (richiedenti.isEmpty()) {
            logger.info("Ribilanciamento [{}]: nessuna colonnina richiedente potenza (0.0 / {} kW).",
                    strategia.getNomeStrategia(), potenzaMassimaKw);
            return;
        }

        Map<Colonnina, Double> allocazioni = strategia.calcolaAllocazioni(
                richiedenti, potenzaMassimaKw, sogliaMinimaKw);

        for (Colonnina colonnina : richiedenti) {
            double kw = allocazioni.getOrDefault(colonnina, 0.0);
            if (kw + EPSILON < sogliaMinimaKw) {
                colonnina.getStatoCorrente().sospendiPerSovraccarico(colonnina);
            } else {
                double kwEffettivi = Math.min(kw, Colonnina.POTENZA_MASSIMA_NOMINALE_KW);
                colonnina.getStatoCorrente().riprendiErogazione(colonnina, kwEffettivi);
            }
        }

        logger.info("Ribilanciamento [{}] completato: {} / {} kW allocati ({} attive, {} sospese, Pmin={} kW).",
                strategia.getNomeStrategia(),
                String.format(Locale.ROOT, "%.2f", getPotenzaTotaleAllocataKw()),
                potenzaMassimaKw,
                getNumeroColonnineInErogazione(),
                getNumeroColonnineSospese(),
                sogliaMinimaKw);
    }

    public void setStrategia(LoadBalancingStrategy nuovaStrategia) {
        if (nuovaStrategia == null) {
            throw new IllegalArgumentException("La strategia di bilanciamento non puo' essere nulla.");
        }
        logger.info("Cambio strategia LoadBalancer: {} -> {}",
                this.strategia.getNomeStrategia(), nuovaStrategia.getNomeStrategia());
        this.strategia = nuovaStrategia;
        ribilancia();
    }

    public void setPotenzaMassimaKw(double nuovaPotenzaMassimaKw) {
        validaParametriRete(nuovaPotenzaMassimaKw, this.sogliaMinimaKw);
        logger.info("Aggiornamento Pmax rete: {} kW -> {} kW",
                this.potenzaMassimaKw, nuovaPotenzaMassimaKw);
        this.potenzaMassimaKw = nuovaPotenzaMassimaKw;
        ribilancia();
    }

    public void setSogliaMinimaKw(double nuovaSogliaMinimaKw) {
        validaParametriRete(this.potenzaMassimaKw, nuovaSogliaMinimaKw);
        logger.info("Aggiornamento Pmin rete: {} kW -> {} kW",
                this.sogliaMinimaKw, nuovaSogliaMinimaKw);
        this.sogliaMinimaKw = nuovaSogliaMinimaKw;
        ribilancia();
    }

    private static void validaParametriRete(double pMax, double pMin) {
        if (Double.isNaN(pMin) || Double.isInfinite(pMin)
                || pMin < SOGLIA_MINIMA_MIN_KW || pMin > SOGLIA_MINIMA_MAX_KW) {
            throw new IllegalArgumentException(
                    "La soglia minima Pmin deve essere compresa tra "
                            + SOGLIA_MINIMA_MIN_KW + " e " + SOGLIA_MINIMA_MAX_KW + " kW.");
        }
        if (Double.isNaN(pMax) || Double.isInfinite(pMax) || pMax < pMin) {
            throw new IllegalArgumentException(
                    "La potenza massima Pmax deve essere maggiore o uguale a Pmin (" + pMin + " kW).");
        }
    }

    public double getPotenzaMassimaKw() {
        return potenzaMassimaKw;
    }

    public double getSogliaMinimaKw() {
        return sogliaMinimaKw;
    }

    public LoadBalancingStrategy getStrategia() {
        return strategia;
    }

    public List<Colonnina> getColonnineRegistrate() {
        return Collections.unmodifiableList(colonnine);
    }

    public double getPotenzaTotaleAllocataKw() {
        return colonnine.stream()
                .mapToDouble(Colonnina::getPotenzaAllocataKw)
                .sum();
    }

    public double getPotenzaDisponibileKw() {
        return Math.max(0.0, potenzaMassimaKw - getPotenzaTotaleAllocataKw());
    }

    public long getNumeroColonnineInErogazione() {
        return colonnine.stream()
                .filter(c -> "IN_EROGAZIONE".equals(c.getStatoCorrente().getNomeStato()))
                .count();
    }

    public long getNumeroColonnineSospese() {
        return colonnine.stream()
                .filter(c -> "SOSPESA_PER_SOVRACCARICO".equals(c.getStatoCorrente().getNomeStato()))
                .count();
    }
}
