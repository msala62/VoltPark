package it.unibg.voltpark.model;

import java.time.LocalDateTime;
import java.util.Optional;

public abstract class Stallo {
    private final String codice;
    private StatoStallo stato;
    private String targaOccupante;
    private int minutiSostaAccumulati;
    private Prenotazione prenotazioneAttiva;

    protected Stallo(String codice) {
        if (codice == null || codice.isBlank()) {
            throw new IllegalArgumentException("Il codice dello stallo non puo' essere vuoto.");
        }
        this.codice = codice;
        this.stato = StatoStallo.LIBERO;
        this.minutiSostaAccumulati = 0;
    }

    public abstract boolean supportaRicaricaEV();
    public abstract double getTariffaOrariaSosta();

    public void prenota(String targa, LocalDateTime adesso, int minutiValidita) {
        if (this.stato != StatoStallo.LIBERO) {
            throw new IllegalStateException("Lo stallo " + codice + " non e' libero.");
        }
        this.prenotazioneAttiva = new Prenotazione(
                targa, codice, adesso, adesso.plusMinutes(minutiValidita));
        this.stato = StatoStallo.PRENOTATO;
    }

    public boolean verificaEScadutaPrenotazione(LocalDateTime adesso) {
        if (this.stato == StatoStallo.PRENOTATO && prenotazioneAttiva != null
                && prenotazioneAttiva.isScaduta(adesso)) {
            this.prenotazioneAttiva = null;
            this.stato = StatoStallo.LIBERO;
            return true;
        }
        return false;
    }

    public void occupa(String targa) {
        if (this.stato == StatoStallo.PRENOTATO) {
            if (prenotazioneAttiva != null && !prenotazioneAttiva.targaVeicolo().equalsIgnoreCase(targa)) {
                throw new IllegalStateException("Stallo prenotato da un'altra targa.");
            }
            this.prenotazioneAttiva = null;
        } else if (this.stato != StatoStallo.LIBERO) {
            throw new IllegalStateException("Stallo " + codice + " non disponibile.");
        }
        this.targaOccupante = targa;
        this.minutiSostaAccumulati = 0;
        this.stato = StatoStallo.OCCUPATO;
    }

    public void libera() {
        this.targaOccupante = null;
        this.prenotazioneAttiva = null;
        this.minutiSostaAccumulati = 0;
        this.stato = StatoStallo.LIBERO;
    }

    public void avanzaTempoSosta(int minuti) {
        if (this.stato == StatoStallo.OCCUPATO && minuti > 0) {
            this.minutiSostaAccumulati += minuti;
        }
    }

    public String getCodice() { return codice; }
    public StatoStallo getStato() { return stato; }
    public void setStato(StatoStallo stato) { this.stato = stato; }
    public Optional<String> getTargaOccupante() { return Optional.ofNullable(targaOccupante); }
    public Optional<Prenotazione> getPrenotazioneAttiva() { return Optional.ofNullable(prenotazioneAttiva); }
    public int getMinutiSostaAccumulati() { return minutiSostaAccumulati; }
}