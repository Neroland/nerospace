# API di integrazione pubblica

Nerospace espone alle altre mod un'unica superficie di integrazione supportata: il pacchetto Java `za.co.neroland.nerospace.api`. Tutto ciò che è fuori da quel pacchetto è interno e può cambiare senza preavviso.

Questa pagina è per gli **sviluppatori di mod**. Se sei un giocatore, niente di tutto ciò richiede configurazione: l'API resta inattiva finché un'altra mod non la usa.

## Stabilità

All'interno di una versione principale, tipi pubblici, metodi, componenti dei record e costanti enum esistenti non vengono mai rimossi, rinominati né cambiati di significato. Nelle versioni minori possono essere aggiunti nuovi membri. Usa l'API come dipendenza `compileOnly` e tieni i tipi dietro un tuo adattatore di compatibilità, così la tua mod funziona anche quando Nerospace è assente.

Nerospace pubblica tutte e sei le combinazioni di loader su GitHub Packages:

```
za.co.neroland.nerospace:nerospace-<loader>-<mc>:<version>
```

## Pianeti, stazioni e rotte

- `NerospacePlanets`: identità del pianeta (`PlanetId`), l'hook `currentPlanet(Entity)`, i `PlanetTraits` di sola lettura di ogni pianeta (gravità predefinita, flag privo d'aria, `Hazard`) e la gravità esatta per posizione.
- `NerospaceStations`: le destinazioni stazione come record `StationInfo` immutabili.
- `NerospaceRoutes`: il catalogo delle rotte a livello di dimensione: `RouteEndpoint` e `CargoRoute` orientate con livello minimo, costo del carburante e durata del transito.

## Piattaforme cargo, rotte e voli (`nerospace.api.route`)

L'API delle rotte a livello di piattaforma dietro le [Piattaforme cargo](Cargo-Pad.md). Ottienila con `RouteApi.instance()`; è lato server e composta solo da interfacce.

- `RouteApi.padsVisibleTo(server, uuid)`: le piattaforme verso cui quel giocatore può spedire (le sue, quelle condivise con lui, quelle pubbliche, le stazioni che gestisce). Mai un elenco dell'intero server.
- `RouteApi.quote(server, origin, destination, manifest, returnEmpty)`: un `RouteQuote`: tick di viaggio, carburante in mB (compreso il ritorno a vuoto se richiesto), flag tra dimensioni, massa del carico.
- `RouteApi.requestFlight(server, requester, FlightRequest)`: lancia un carico dal razzo cargo attraccato e rifornito sulla piattaforma di origine; un `FlightRequestResult` contiene il volo o il motivo esatto del rifiuto.
  Le nuove costanti `Denial` vengono solo aggiunte in fondo: la 1.4.0 ha aggiunto `CANNOT_PAY` (il richiedente non ha potuto pagare la tariffa facoltativa di lancio o di attracco del server). Associa i nomi che non conosci a un valore di ripiego.
- `RouteApi.flight` / `flightsOwnedBy` / `routesOwnedBy` / `cancel`: interrogazione e gestione.
- `RouteEvents.subscribe(listener)`: `onPadRegistered`, `onPadUnregistered`, `onFlightDeparted`, `onFlightArrived`, `onFlightHeld`, `onFlightDropped`.

Il modello dei costi, così modpack e altre mod possono ragionarci (`cargoTravelTimeMultiplier` e `fuelCostMultiplier` si applicano in aggiunta):

```text
travelTicks = 1200 + (crossDimension ? 2400 : 0) + 1800 × (gOrigin + gDestination) / 2, clamped 600..24000
massUnits   = ceil(items / 64)
fuelMb      = (1000 + 100 × massUnits) × max(0.1, gOrigin)      (+ the same for mass 0 from gDestination on return_empty)
```

`PadHandle`, `RouteHandle` e `FlightHandle` sono istantanee immutabili; la proprietà viene indicata solo come `ownedBy(UUID)` per un UUID che possiedi già.

## Interrogazioni sull'ambiente

`NerospaceEnvironment.at(ServerLevel, BlockPos)` restituisce un `EnvironmentSnapshot` immutabile:

| Campo            | Significato                                                                                                                           |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `loaded`         | `false` quando il chunk non è caricato: in quel caso ogni altro campo è un valore predefinito sicuro, non una lettura |
| `planet`         | il pianeta di Nerospace, oppure vuoto sulla Terra e in qualsiasi dimensione non di Nerospace                                          |
| `atmosphere`     | `VACUUM`, `PRESSURIZED`, `TERRAFORMING` o `BREATHABLE`                                                                                |
| `hazard`         | il pericolo ambientale del pianeta                                                                                                    |
| `gravity`        | fattore di gravità, `1.0` = normale terrestre                                                                                         |
| `oxygen`         | 0-15, l'area di ossigeno più eventuali contributi esterni                                                                             |
| `terraformStage` | 0-3                                                                                                                                   |
| `breathable`     | se un giocatore può respirare qui senza tuta                                                                                          |

Due comportamenti da conoscere:

- **Non forza mai il caricamento dei chunk.** Il controllo sui chunk non caricati viene eseguito prima di qualsiasi ricerca basata sui chunk, quindi una sonda in una posizione arbitraria non può far generare terreno al server.
- **La fase 1 significa già respirabile.** È il momento in cui il Terraformatore contrassegna il chunk, quindi `TERRAFORMING` descrive una regione in corso di trasformazione che non ci è ancora arrivata.

## Visite ai pianeti

`NerospaceVisits.hasVisited(player, planet)` e `visitedPlanets(server, uuid)` rispondono in base allo storico, comprese le visite fatte prima che la tua mod fosse installata. `PlanetVisitEvents.onVisit(listener)` scatta una volta, sul thread del server, la prima volta che un giocatore raggiunge un pianeta.

L'evento ti consegna un giocatore attivo, perché di solito i listener devono concedergli qualcosa. Se di conseguenza salvi qualcosa legato a lui, **quella copia spetta a te cancellarla**: l'hook di cancellazione di Nerospace raggiunge l'archivio di Nerospace, non il tuo. Registrati presso `PlayerDataErasure` di Neroland Core così una singola richiesta elimina comunque tutto.

## Contribuire con ossigeno

`NerospaceOxygen.contribute(level, source, center, radius, strength, durationTicks)` registra una sorgente di ossigeno limitata e con scadenza che Nerospace include nelle risposte su respirabilità e ambiente.
Limiti: raggio ≤ 64, intensità ≤ 15, durata ≤ un'ora. Contribuire di nuovo con lo stesso id sorgente sostituisce il valore precedente, quindi lo schema naturale «questa macchina ora produce N» è idempotente.
`remove(level, source)` lo ritira.

I contributi devono essere rinnovati prima della scadenza. È voluto: una macchina distrutta mentre il suo chunk non è caricato non può lasciare dietro di sé aria gratuita permanente.

## Sovrapposizioni di terraformazione

`NerospaceTerraforming` applica e annulla sovrapposizioni regionali reversibili. Una sovrapposizione cambia ciò che Nerospace _riporta_ su una regione senza riscrivere un solo blocco, ed è questo che rende l'annullamento istantaneo e senza perdite. La copertura è **orizzontale**: una regione va dal substrato roccioso al limite di costruzione come una colonna, così come la terraformazione fisica contrassegna un intero chunk anziché una bolla. Ogni regione registra la fase fisica del chunk sottostante, quindi rimuovere una sovrapposizione ripristina quella base invece di cancellare progressi reali.

**Ogni modifica viene rifiutata finché il server non installa una politica di rivendicazione:**

```java
NerospaceTerraforming.setClaimPolicy((actor, level, request) -> yourClaimMod.mayBuild(actor, request.center()));
```

È volutamente sicuro per impostazione predefinita: una modifica ambientale regionale non autenticata è uno strumento per il griefing.

## Privacy

Due superfici dell'API toccano dati dei giocatori, ed entrambe sono coperte dall'hook di cancellazione dei dati condiviso di Neroland Core:

- La **proprietà delle stazioni** non viene mai esposta come UUID grezzo, ma solo come booleano per giocatore `NerospaceStations.ownedBy`.
- Le **visite ai pianeti** memorizzano un UUID e un insieme di id di pianeti. Niente date e ore, niente coordinate, niente tracce di movimento. Una richiesta di cancellazione elimina la riga e riscrive subito il backup dei dati salvati.
- Le **rotte cargo** memorizzano l'UUID del proprietario di piattaforme, rotte e voli e gli UUID nell'elenco di accesso di una piattaforma. L'API non li restituisce mai; `padsVisibleTo` è limitato al giocatore che chiede. La cancellazione rimuove le rotte del giocatore, anonimizza le sue piattaforme e i suoi voli e lo toglie dagli elenchi di accesso: il carico già in volo viene comunque consegnato o imballato.

I contributi di ossigeno e le sovrapposizioni di terraformazione non memorizzano **alcun giocatore**. I loro id di sorgente e di regione li scegli tu, vengono salvati così come sono e **non devono mai codificare l'UUID o il nome di un giocatore**.
