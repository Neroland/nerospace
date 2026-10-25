# Piattaforma cargo

La **Piattaforma cargo** è il punto finale di una linea di rifornimento automatizzata: una piastra da rampa di lancio con una stiva da 27 slot, una riserva di carburante alimentata da tubi e un selettore di rotta. Costruiscine una dove le merci partono e una dove devono arrivare, metti un [Razzo cargo](Cargo-Rocket.md) sulla prima, e il carico vola tra le due senza nessuno a nessuna delle piattaforme.

## Ricetta

Lingotti di Nerosteel attorno a uno [Scafo cargo](Cargo-Rocket.md) in alto, due [Rampe di lancio per razzi](Rocket-Launch-Pad.md) ai lati di un Blocco di Nerosium al centro, Nerosteel lungo il fondo. Si colloca sulla scala dei materiali di Greenxertz, dopo il razzo di livello 2.

## Costruire la piattaforma

Una Piattaforma cargo **è** un blocco di rampa di lancio. Costruisci attorno a essa la consueta rampa **3×3** completa (la Piattaforma cargo può essere una qualsiasi cella del quadrato): un razzo cargo richiede una struttura di livello 2, lo stesso requisito di un razzo con equipaggio di livello 2. Fai clic destro da accovacciato a mani vuote sulla piattaforma per leggere il livello che forma.

- Un **[Serbatoio di carburante](Fuel-Tank.md)** adiacente pompa nel razzo cargo attraccato esattamente come fa per i razzi con equipaggio. In alternativa convoglia il carburante per razzi direttamente nella Piattaforma cargo: la sua riserva di carburante (16.000 mB per impostazione predefinita) accetta solo carburante per razzi e lo pompa nel razzo a 80 mB/tick.
- I **[Tubi universali](Universal-Pipe.md)** si collegano a ogni faccia. La stiva usa la configurazione dei lati di Neroland Core (il preset STORAGE: ogni faccia è in ingresso/uscita), quindi i tubi immettono il carico all'origine ed estraggono il carico consegnato a destinazione; i [filtri per tubi](Pipe-Filters-and-Upgrades.md) base e avanzati funzionano senza modifiche. Usa il [Configuratore](Configurator.md) per rendere una faccia solo ingresso o solo uscita.
- La stiva ha 27 slot; l'impostazione del server `cargoPadSlots` (predefinita 18) decide quanti sono utilizzabili.
  Gli slot bloccati sono disegnati più scuri nell'interfaccia.

## Proprietà

Piazzare una Piattaforma cargo ti rende il suo proprietario. Solo il proprietario (o chiunque, finché una piattaforma non ha proprietario) può cambiarne destinazione, programmazione e opzioni; chiunque può comunque inserire oggetti tramite l'interfaccia o i tubi. Gli altri giocatori possono inviare carichi **verso** la tua piattaforma solo se la contrassegni come **Pubblica** nell'interfaccia. Un'etichetta rinomina la piattaforma; il nome è ciò che gli altri giocatori vedono nel loro elenco di destinazioni.

Anche le [stazioni orbitali](Station-Charter.md) fondate contano come destinazioni: un volo verso una stazione atterra su una Piattaforma cargo piazzata entro sei blocchi dalla piattaforma di atterraggio della stazione.

## L'interfaccia

Fai clic destro sulla piattaforma (o sul suo razzo) a mani vuote.

| Controllo                  | Significato                                                                                                                                                                                                                     |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `<` `>` `x`                | Scorri le piattaforme verso cui puoi spedire (le tue, quelle condivise con te, quelle pubbliche, le tue stazioni), o azzera la destinazione.                                                 |
| Programmazione             | **Manuale** (solo il pulsante Lancia), **lancia quando piena** (ogni slot utilizzabile contiene qualcosa), oppure **ogni N min** (`-`/`+` cambiano N). |
| Indicatori riserva / razzo | Fuel in the pad's buffer and in the docked rocket.                                                                                                                                                              |
| Riga del viaggio           | Il preventivo per ciò che c'è ora nella stiva: carburante che il razzo deve contenere e durata del volo.                                                                                        |
| Ritorno a vuoto            | Dopo lo scarico, il razzo torna a vuoto a questa piattaforma invece di restare a destinazione. Il carburante del ritorno viene addebitato al lancio.                                            |
| Pubblica / Privata         | Se altri giocatori possono inviare carichi qui.                                                                                                                                                                 |
| Launch                     | Si illumina quando è attraccato un razzo cargo rifornito, è impostata una destinazione e la stiva non è vuota.                                                                                                  |
| Riga di stato              | L'esito dell'ultimo tentativo di lancio, poi lo stato dell'ultimo volo (in transito con conto alla rovescia, in attesa, consegnato, imballato).                                              |

## Volo, arrivo e attesa

Durata del viaggio e carburante derivano dal registro dei pianeti (vedi l'[API pubblica](Public-API.md) per la formula): un volo dura almeno un minuto, di più tra dimensioni diverse e tra mondi pesanti; il carburante cresce con il carico e con la gravità dell'origine. Durante il transito non viene mostrato nulla: il volo è un timer lato server che **sopravvive a un riavvio del server**.

All'arrivo il razzo si materializza sulla piattaforma di destinazione e scarica una pila ogni due tick così i tubi stanno al passo. Se la destinazione non è caricata, il volo aspetta: Nerospace non carica mai un chunk per consegnare.
Se la piattaforma di destinazione non c'è più, vi è attraccato un altro razzo o la sua stiva è piena, il volo **resta in attesa** e riprova ogni 30 secondi (`cargoHoldRetrySeconds`); dopo 30 minuti di attesa (`cargoHoldTimeoutMinutes`) il carico rimanente viene lasciato cadere come oggetto **Cassa cargo** nella posizione di destinazione. Il carico non viene mai cancellato. Fai clic destro su una cassa per disimballarla.

L'uscita del comparatore segue il livello di riempimento della stiva.

In un mondo creativo, `/nerospace gallery` costruisce una linea di trasporto merci già pronta (due piattaforme con rotta, un razzo rifornito, una stiva carica) a nord della fila dei razzi: apri la piattaforma A e premi Lancia per vedere una consegna.

## Privacy

La piattaforma memorizza l'UUID del suo proprietario, ed eventualmente gli UUID dei giocatori con cui il proprietario l'ha condivisa, solo nel salvataggio del mondo. `/neroland data eraseme` rimuove le tue rotte, scollega le tue piattaforme (che restano come contenuto condiviso del mondo), ti toglie da ogni elenco di accesso e ti stacca dai tuoi voli: il carico viene comunque consegnato o imballato. Niente di tutto ciò viene registrato nei log o inviato da qualche parte. Vedi `PRIVACY.md`.

---

Vedi anche: [Razzo cargo](Cargo-Rocket.md), [Rampa di lancio per razzi](Rocket-Launch-Pad.md), [Serbatoio di carburante](Fuel-Tank.md), [Tubo universale](Universal-Pipe.md), [Configurazione](Configuration.md).
