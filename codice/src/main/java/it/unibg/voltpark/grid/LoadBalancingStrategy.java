package it.unibg.voltpark.grid;

import it.unibg.voltpark.model.Colonnina;

import java.util.List;
import java.util.Map;

/**
 * Interfaccia dello Strategy Pattern per gli algoritmi di Smart Grid Load Balancing.
 * Le implementazioni sono stateless e calcolano esclusivamente la mappa delle potenze
 * da assegnare; l'applicazione delle transizioni di stato e' demandata a {@link LoadBalancer}.
 */
public interface LoadBalancingStrategy {

    /**
     * Calcola la potenza (in kW) da allocare a ciascuna colonnina richiedente.
     * Ogni valore restituito e' 0.0 (colonnina da sospendere per sovraccarico) oppure
     * compreso nell'intervallo [sogliaMinimaKw, Colonnina.POTENZA_MASSIMA_NOMINALE_KW].
     * La somma delle potenze allocate non supera mai potenzaMassimaKw.
     *
     * @param colonnineRichiedenti elenco delle colonnine che richiedono potenza
     * @param potenzaMassimaKw     tetto massimo complessivo della rete (kW)
     * @param sogliaMinimaKw       soglia minima tecnica garantita per colonnina attiva (kW)
     * @return mappa colonnina -> potenza allocata in kW
     */
    Map<Colonnina, Double> calcolaAllocazioni(
            List<Colonnina> colonnineRichiedenti,
            double potenzaMassimaKw,
            double sogliaMinimaKw);

    /** Restituisce l'identificativo leggibile della strategia. */
    String getNomeStrategia();
}
