package it.unibg.voltpark.model;

public interface GridEventListener {
    void onGridStateChanged(String idColonnina, String motivo);
}