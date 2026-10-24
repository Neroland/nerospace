# Configurazione

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Configuration.md).

La configurazione di Nerospace è volutamente ridotta. La mod tiene nel codice i valori di bilanciamento di base. Modpack e server li regolano con **sei moltiplicatori**. Le altre chiavi sono semplici interruttori e limiti.

> **Gestita da [Neroland Core](Neroland-Core.md).** Il file di configurazione è `config/nerospace.properties`. È di proprietà di Core. Ricaricalo in tempo reale con `/neroland config reload` (non serve riavviare). La maggior parte delle chiavi è **decisa dal server**: quando entri in un server, il tuo gioco usa i valori del server. L'unica eccezione è `telemetryEnabled`. È una scelta personale su ogni client, e un server non può mai cambiarla. I file esistenti vengono migrati sul posto.
>
> **Esistono solo le chiavi di questa pagina.** Pagine della wiki e versioni più vecchie elencavano molte più chiavi (area di ossigeno, atmosfera, effetti visivi del client, chiavi del terraformatore e delle meteore). Quelle chiavi non ci sono più. I valori che impostavano ora sono fissi nel codice.

## I sei moltiplicatori

Tutti e sei hanno valore predefinito `1.0`. Ognuno può essere impostato da **0.1 a 10.0**. Ogni valore scalato resta almeno 1, così un'impostazione estrema non ferma mai una macchina né rompe la progressione.

| Chiave | Cosa scala | Valori di base (a 1.0) |
| --- | --- | --- |
| `energyRateMultiplier` | Gli FE/t prodotti dai generatori: Generatore a combustione, Generatore passivo e Pannelli solari. Scala anche le riserve dei pannelli solari. **Non** cambia il Serbatoio di carburante né la riserva del Macinatore. | Combustione 20 FE/t · Passivo 8 FE/t · Solare 20 / 100 / 400 FE/t (livello 1 / 2 / 3) |
| `oxygenDrainMultiplier` | Quanto velocemente si consuma l'aria quando non sei in una zona sicura. Più alto = più duro. | Polmoni nudi 30 aria per controllo · tuta completa 3 aria per controllo (un controllo = 10 tick) · ×4 in un pericolo non schermato |
| `oxygenCapacityMultiplier` | Quanta aria contengono il giocatore e le tute. | Senza tuta 300 · qualsiasi tuta completa 900 |
| `fuelCostMultiplier` | Costi di carburante ed energia: carburante per lancio di razzo (mai più del serbatoio), ossigeno per unità d'aria in camera stagna e costi energetici delle macchine. Anche il carburante cargo. | Lancio: 2.500 mB verso un'altra dimensione, oppure 500 mB + 60 mB ogni 100 blocchi nella stessa dimensione · camera stagna 2 mB per aria · Macinatore 20 FE/t · Generatore di O₂ 20 FE per mB |
| `machineSpeedMultiplier` | Quanto velocemente lavorano le macchine. Più alto = più veloce (tempo di lavoro più breve). | Macinatore 200 tick per oggetto |
| `gravityMultiplier` | Una scala globale sulla [gravità](Planet-Gravity.md) di ogni mondo: giocatori, creature, oggetti, proiettili e meteore in caduta. Sotto 1 = più fluttuante, sopra 1 = più pesante. | Stazione ~0.1× · Glacira ~0.4× · Greenxertz ~0.6× · Cindara ~0.8× · Overworld e terreno terraformato 1.0× |

Esempi: `oxygenDrainMultiplier = 2.0` fa durare l'aria la metà. `machineSpeedMultiplier = 2.0` fa finire al macinatore un oggetto in 100 tick. `energyRateMultiplier = 0.5` dimezza la produzione dei generatori.

A `10.0`, il limite di ingresso di una macchina (per esempio i 1.000 FE/t del Generatore di O₂) può diventare il vero limite prima della velocità moltiplicata. È voluto.

## Valori di base fissi

Questi numeri sono fissi nel codice. Sono elencati qui per aiutarti a pianificare le costruzioni.

| Cosa | Valore |
| --- | --- |
| Tubo universale (ogni livello) | contiene 8.000 · sposta fino a 1.000 FE/t o 1.000 mB/t |
| Tubo universale (oggetti) | 8 tick per blocco · preleva 16 oggetti ogni 4 tick |
| Generatore di ossigeno | produce 4 mB/t · accetta fino a 1.000 FE/t · riserva da 50.000 FE · serbatoio da 8.000 mB |
| Camera stagna | 90 aria per controllo (ogni 10 tick) · raggio di 4 blocchi |
| Serbatoi di carburante dei razzi | 3.000 / 6.000 / 12.000 / 24.000 mB (livello 1 / 2 / 3 / 4) |
| Terraformatore | fino a 48 colonne per tick · accetta fino a 2.000 FE/t |
| Meteore | circa una ogni 9.000 s · preavviso di 30 s · a 200–500 blocchi · fino a 4 siti |

## Altre chiavi

### Telemetria

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `telemetryEnabled` | `true` | Segnalazioni anonime di arresto anomalo relative solo a Nerospace (Sentry, server UE, ripulite secondo POPIA/GDPR: vedi `PRIVACY.md`). Imposta `false` per disattivarle. Si applica al ricaricamento della configurazione. Ogni giocatore decide per il proprio client. |

### Terraformatore

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `terraformerForceLoadEnabled` | `false` | Un [Terraformatore](Terraformer.md) in funzione tiene caricata una piccola finestra di chunk, così continua a lavorare mentre sei lontano. Disattivato per impostazione predefinita perché costa memoria e velocità del server. |

### NeroLink

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `linkModuleEnabled` | `true` | Condivide i dati di Nerospace con l'app complementare NeroLink: i tuoi razzi, stazioni, pianeti, supporto vitale e avanzamento della Guida stellare (mai quelli di un altro giocatore). Imposta `false` e l'app mostrerà Nerospace come assente. Decide il server. |

### Razzi cargo

Impostazioni per le [Piattaforme cargo](Cargo-Pad.md) e i voli cargo. Le decide tutte il server.

| Chiave | Predefinito | Intervallo | Significato |
| --- | --- | --- | --- |
| `cargoPadSlots` | `18` | 1–27 | Slot della stiva utilizzabili su una Piattaforma cargo: il carico che trasporta un razzo cargo. |
| `cargoFuelCapacity` | `16000` | 1.000–64.000 | Carburante per razzi (mB) contenuto nella riserva alimentata da tubi di una Piattaforma cargo. |
| `cargoTravelTimeMultiplier` | `1.0` | 0.1–10 | Scala la durata dei voli cargo (più alto = più lento). |
| `cargoHoldRetrySeconds` | `30` | 5–600 | Ogni quanto un volo in attesa riprova ad atterrare. |
| `cargoHoldTimeoutMinutes` | `30` | 1–1.440 | Per quanto un volo resta in attesa prima che il carico venga lasciato cadere come Cassa cargo a destinazione (mai cancellato). |
| `cargoFlightRetentionDays` | `7` | 1–90 | Giorni di gioco per cui si conserva il registro di un volo concluso. |
| `cargoMaxFlightsPerOwner` | `16` | 1–128 | Voli attivi che un giocatore può avere contemporaneamente. |
| `cargoMaxPads` | `256` | 8–4.096 | Numero massimo di Piattaforme cargo registrabili. |

`fuelCostMultiplier` scala anche il carburante cargo.

### Economia (facoltativa: richiede NeroEconomy)

Ricompense e tariffe in crediti. Hanno effetto solo quando è installata la mod **NeroEconomy**. Ogni importo ha valore predefinito `0`, che disattiva quella funzione. Le decide tutte il server.

| Chiave | Predefinito | Intervallo | Significato |
| --- | --- | --- | --- |
| `economyFirstVisitReward` | `0` | 0–1.000.000 | Crediti pagati una volta, la prima volta che un giocatore raggiunge ciascun pianeta. Pagati dalla Riserva di NeroEconomy e limitati dai suoi tetti di pagamento giornalieri. Una ricompensa rifiutata non viene ritentata. |
| `economyLaunchFee` | `0` | 0–1.000.000 | Crediti addebitati per ogni lancio con equipaggio o cargo, dopo che tutti gli altri controlli di lancio sono superati. Un giocatore che non può pagare non può lanciare (una Piattaforma cargo mostra *Impossibile pagare la tariffa di volo*). |
| `economyDockingFee` | `0` | 0–1.000.000 | Crediti addebitati per ogni volo verso una stazione orbitale, in aggiunta alla tariffa di lancio. Se poi la tariffa di lancio viene rifiutata, la tariffa di attracco viene restituita. |

Le tariffe vanno alla Riserva di NeroEconomy. Senza NeroEconomy queste chiavi non fanno nulla.

### Cava

| Chiave | Predefinito | Intervallo | Significato |
| --- | --- | --- | --- |
| `quarryMaxSide` | `64` | 4–64 | Lato più lungo (in blocchi) dell'area dei segnaposto di una cava. |
| `quarryFrameDecayTicks` | `600` | 20–24.000 | Ritardo base prima che un blocco del telaio rimasto si sgretoli e lasci cadere il suo involucro. Ogni blocco aspetta `base + random(base × 7)` tick, quindi con il valore predefinito l'anello si disfa in circa 30 s – 4 min. |

### Villaggi alieni

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `alienRaidsEnabled` | `true` | Le creature ostili assaltano di notte i villaggi alieni rivendicati. Imposta `false` per disattivare le incursioni. Si applica al ricaricamento della configurazione. |

### Chat

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `welcomeMessageEnabled` | `true` | Invia il messaggio di benvenuto di Nerospace su due righe (l'invito al Libro della Guida stellare e il link per i commenti) quando un giocatore entra. Imposta `false` per entrare in silenzio. Si applica al ricaricamento della configurazione. Decide il server. |

## Chiavi rimosse (per gli autori di modpack)

Queste chiavi non esistono più. Se sono ancora nel tuo file, non fanno nulla.

- Assorbite nei moltiplicatori o fisse nel codice: `atmosphereDamage`, `oxygenMax`, `oxygenDrainPerTick`,
  `oxygenSuitDrain`, `oxygenSuitT2Max`, `oxygenAirlockRadius`, `oxygenAirlockRefillPerCheck`,
  `oxygenAirlockMbPerAir`, `terraformEnergyPerBlock`, `terraformWorkIntervalTicks`,
  `energyPipeCapacity`, `energyPipeThroughput`, `fluidPipeCapacity`, `fluidPipeThroughput`,
  `gasPipeCapacity`, `gasPipeThroughput`, `itemPipeTicksPerBlock`, `itemPipeExtractAmount`,
  `itemPipeExtractPeriod`, `batteryCapacity`, `fluidTankCapacity`, `gasTankCapacity`,
  `combustionGeneratorFePerTick`, `passiveGeneratorFePerTick`.
- Atmosfera e area di ossigeno: `atmosphereDamageEnabled`, `atmosphereSafeRadius`,
  `oxygenBubbleRadius`, `oxygenMaxConcentration`, `oxygenBreathableThreshold`,
  `oxygenDiffusionRate`, `oxygenDecayPerStep`, `oxygenSimIntervalTicks`,
  `oxygenMaxActiveCellsPerSource`, `oxygenLeakRange`, `oxygenEvaporateSeconds`, `oxygenSyncRadius`.
- Effetti visivi del client: `oxygenVisualQuality`, `oxygenParticleIntensity`, `oxygenHazeIntensity`,
  `oxygenBoundaryIntensity`, `oxygenDebugLog`.
- Terraformatore: `terraformPlantsEnabled`, `terraformWaterEnabled`, `terraformWaterMaxDepth`,
  `terraformDriftEnabled`, `terraformDriftPerSecond`, `terraformFaunaEnabled`,
  `terraformResourcesEnabled`, `terraformResourceOres`, `terraformMaxColumnsPerTick`,
  `terraformForceLoadChunks`, `terraformMaxForcedChunks`, `terraformPlantChance`,
  `terraformResourceChance`.
- Meteore: `meteorNaturalSpawn`, `meteorAvgIntervalSeconds`, `meteorWarningSeconds`,
  `meteorMinDistance`, `meteorMaxDistance`, `meteorCraterRadius`, `meteorMaxActiveSites`,
  `meteorLootBonusRolls`, `meteorDebugLog`.
- Residui del modello: `logDirtBlock`, `magicNumber`, `magicNumberIntroduction`, `items`,
  `oxygenSealedRoomMax`.
