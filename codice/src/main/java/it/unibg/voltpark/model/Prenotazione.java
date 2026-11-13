package it.unibg.voltpark.model;

import java.time.LocalDateTime;

public record Prenotazione(
        String targaVeicolo,
        String codiceStallo,
        LocalDateTime istanteCreazione,
        LocalDateTime istanteScadenza) {

    public boolean isScaduta(LocalDateTime orarioCorrente) {
        return orarioCorrente.isAfter(istanteScadenza);
    }
}