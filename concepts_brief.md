# VoltPark — Concepts Brief & Brainstorming RFC

* **Author:** Salazar34 (Matteo Sala, Niccolò Angotti, Davide Donini)
* **Date:** 2026-11-10

## Overview

VoltPark è un sistema software per la gestione automatizzata di uno smart parking da 10 stalli (4 Standard, 6 EV) dotato di bilanciamento dinamico della potenza elettrica (*Smart Grid Load Balancing*). L'obiettivo ingegneristico primario è prevenire il sovraccarico dell'impianto ($P \le P_{\max}$), garantire che nessuna colonnina operi sotto la soglia tecnica minima ($P \ge P_{\min}$), rilevare e sanzionare l'occupazione abusiva post-carica (*Overstay*) e calcolare in modo trasparente la tariffazione combinata.

## Analysis

Dall'analisi dei documenti di progetto ([Project_Plan.md](file:///home/shadow01/personal/VoltPark/documenti/Project_Plan.md) e [RequirementsSpecification.md](file:///home/shadow01/personal/VoltPark/documenti/RequirementsSpecification.md)) emergono quattro punti critici che richiedono allineamento prima di avviare l'implementazione:

1. **Riorganizzazione delle Directory di Build:**
   Il filesystem attuale contiene il file `pom.xml` direttamente in `codice/`. Per rispettare la specifica di progetto e consentire l'introduzione di `VP_web` mantenendo l'importabilità pulita in Eclipse, i sorgenti di backend devono risiedere in `codice/VP_core_db/`.

2. **Astrazione del Tempo (Clock Deterministico):**
   I requisiti impongono controlli temporali stringenti (scadenza prenotazioni a 30 min, tick energetici $\Delta t$, grace period di 15 min per overstay). L'architettura deve prevedere un `SimulationClock` per consentire simulazioni temporali riproducibili e unit test deterministici con JUnit 5 senza attendere tempo reale.

3. **Casi Limite nel Load Balancing:**
   - In caso di *Equal Split* con saturazione ($P_{\max}/N < P_{\min}$), deve essere definita una politica deterministica di sospensione parziale (es. sospensione in ordine FCFS inverso) per non portare tutte le colonnine a 0 kW.
   - Nella strategia *Priority Tier*, va chiarito se la saturazione da sole sessioni *Fast* degradi a Equal Split tra le sole sessioni Fast o segua un ordine di arrivo.

4. **Persistenza su H2 Embedded:**
   La configurazione Maven include `com.h2database:h2` senza ORM (Hibernate o JPA). L'approccio raccomandato è un layer DAO/Repository basato su JDBC standard e connection pool leggero o gestione diretta delle connessioni tramite file di database locale relativo (senza percorsi assoluti).

## Goals vs. Non-Goals

* **In-Scope Goals:**
  - Separazione pulita dei package senza cicli (`it.unibg.voltpark.model`, `.balancing`, `.events`, `.billing`, `.persistence`).
  - Implementazione dei 3 Design Pattern GoF: State (Colonnina), Strategy (LoadBalancing e Billing), Observer (Monitor di rete).
  - Gestione completa dei 10 stalli e delle sessioni EV con transizioni di stato e calcolo overstay.
  - Test di unità JUnit 5 con copertura $\ge 80\%$.
  - Integrazione con database H2 embedded.

* **Out-of-Scope Non-Goals:**
  - Schermate di login/autenticazione complesse (posticipate a requisito opzionale `RF-OPT-01`).
  - Simulazione di impianti fotovoltaici e accumuli ausiliari.
  - Gateway di pagamento esterni o protocolli fisici hardware (OCPP).

## Alternatives Weighed

* **Hibernate / Spring Data JPA vs. JDBC Puro in `VP_core_db`:**
  - *Scelta:* JDBC puro con pattern DAO/Repository.
  - *Motivazione tecnica:* Riduce l'impronta delle dipendenze, azzera i conflitti di versioning con Eclipse/Maven nel backend puro e semplifica il rispetto dei vincoli di accoppiamento.

* **Thread.sleep() vs. Clock Deterministico:**
  - *Scelta:* `SimulationClock` configurabile.
  - *Motivazione tecnica:* Consente test istantanei a copertura del 100% per gli scenari di overstay e scadenze prenotazioni.

## Key Decisions & Staging for Next Steps

1. Ristrutturazione della cartella `codice/` creando la sottocartella `VP_core_db/`.
2. Definizione del package layout e delle interfacce fondamentali.
3. Implementazione del modello di dominio e degli algoritmi di bilanciamento con relativo State e Strategy pattern.
4. Predisposizione dei test di unità JUnit 5.
