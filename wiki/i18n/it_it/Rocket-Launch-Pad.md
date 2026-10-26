# Rampa di lancio per razzi

<!-- nerospace:render -->

<p align="right"><img src="images/rocket_launch_pad.png" alt="Rocket Launch Pad" width="150" align="right"></p>
<!-- /nerospace:render -->

La base su cui si schiera un razzo prima del lancio, e una zona di atterraggio sicura.

## Panoramica

Schiera un **razzo** (fai clic destro su una rampa di lancio con un oggetto razzo) e comparirà sulla rampa, pronto per il rifornimento e il volo. La rampa fa anche da piccola **zona sicura respirabile**, così chi arriva non viene subito punito dall'atmosfera priva d'aria.

## Obtaining

**Fabbricazione** (con forma):

```text
N N N
N B N
N N N
```

`N` = Lingotto di Nerosteel · `B` = [Blocco di Nerosium](Block-of-Nerosium.md)

## Come funziona

- **Schieramento:** fai clic destro su un qualsiasi blocco della rampa con un oggetto razzo per posizionarvi il razzo. La dimensione della rampa viene

  controllata **al lancio**. Se la rampa è troppo piccola per il livello del razzo, il lancio viene bloccato con un
  messaggio chiaro. Rompere blocchi della rampa sotto un razzo schierato può lasciarlo a terra.

- **Dimensione della rampa per livello** (una rampa più grande funziona sempre per un razzo più piccolo):
  - **Livello 1**: un blocco di rampa.

  - **Livello 2**: una rampa 3×3 completa e allineata.

  - **Livello 3**: una rampa 3×3 **circondata dalla [Parete della stazione](Station-Wall.md)** (il bordo di 16 blocchi del

    5×5 circostante, all'altezza della rampa), oppure un [Complesso di lancio pesante](Launch-Gantry.md).

  - **Livello 4**: solo il Complesso di lancio pesante.

- **Dati dei razzi per livello:**

  | Tier | Serbatoio di carburante   | Ossigeno a bordo          | Energia a bordo           |
  | ---- | ------------------------- | ------------------------- | ------------------------- |
  | 1    | 3.000 mB  | 2.000 mB  | 4.000 FE  |
  | 2    | 6.000 mB  | 4.000 mB  | 8.000 FE  |
  | 3    | 12.000 mB | 8.000 mB  | 16.000 FE |
  | 4    | 24.000 mB | 16.000 mB | 30.000 FE |

- Il **carburante per lancio** è lo stesso per ogni livello. Dipende dal viaggio:

  - un viaggio verso **un'altra dimensione** costa **2.500 mB** (500 di base + 2.000);
  - un salto **all'interno della stessa dimensione** costa **500 mB + 60 mB ogni 100 blocchi**.

  Entrambi scalano con `fuelCostMultiplier`. Vedi [Configurazione](Configuration.md).

- **Consiglio:** un razzo di livello 1 raggiunge la stazione con solo circa **500 mB** rimasti. Porta almeno

  **2 Taniche di carburante per razzi** così puoi tornare a casa.

- **Complesso di lancio pesante:** una **rampa 5×5** completa e allineata più almeno un modulo

  **[Torre di lancio](Launch-Gantry.md)** adiacente all'altezza della rampa. Fai clic destro sulla torre per salire
  sul razzo; un clic destro a mani vuote su un qualsiasi blocco della rampa fornisce un **rapporto sulla struttura** (dimensione del gruppo, quadrato
  più grande, moduli presenti, prossimo pezzo mancante). Vedi [Torre di lancio](Launch-Gantry.md).

- **Multiblocco:** le rampe piazzate una accanto all'altra formano un gruppo. Una **rampa 3×3** completa e allineata permette a un

  [Serbatoio di carburante](Fuel-Tank.md) di rifornire **4 volte più veloce**, e un complesso pesante **12 volte più veloce** (480 mB/t).

- **Proxy per l'automazione:** mentre un razzo si trova sul gruppo, ogni blocco della rampa espone lo

  **slot di immissione del carburante** del razzo come capability per gli oggetti: punta una tramoggia o un [Tubo universale](Universal-Pipe.md)
  (livello oggetti) verso un qualsiasi blocco della rampa per immettere Secchi/Taniche di carburante per razzi (ed estrarre i secchi
  vuoti). Un Serbatoio di carburante + tubi su una rampa 3×3 automatizzano completamente la preparazione al lancio.

- **Zona sicura:** entro pochi blocchi da una rampa puoi respirare, quindi non soffochi nell'istante in cui

  atterri. Costruisci una base con un [Generatore di ossigeno](Oxygen-Generator.md) per tutto ciò che va oltre l'area di atterraggio.

> **Vuoi costruirne una?** La [Guida alla costruzione della rampa di lancio](Launch-Pad-Build-Guide.md) contiene uno schema e
> un elenco dei materiali per ogni livello.

## Pad-to-pad travel

Launch pads double as the waypoints of rocket travel.

- **Registrare una rampa:** fai clic destro su una rampa con un'**etichetta** per renderla un nodo di viaggio con nome (il nome dell'etichetta la identifica, oppure «Pad N»). Le rampe registrate diventano destinazioni di atterraggio.
- **Scegliere dove atterrare:** nell'interfaccia del razzo scegli una destinazione (Casa / un pianeta / la Stazione orbitale), poi scorri il **selettore di rampa** per atterrare su una specifica rampa registrata lì, oppure _Più vicina_ per la scelta automatica. Puoi anche saltare **da rampa a rampa nella stessa dimensione**.
- **Carburante calcolato:** un lancio consuma un costo base più la **distanza** (per un salto nello stesso mondo) o un **sovrapprezzo fisso tra dimensioni**. Il costo è mostrato in tempo reale nella console; il lancio è bloccato finché il serbatoio non lo copre.
- **Ritorno a casa:** un razzo ricorda la rampa da cui è partito, quindi tornando atterri **esattamente su quella rampa**. Una **Capsula di atterraggio** viene costruita automaticamente solo quando la destinazione non ha davvero alcuna rampa di lancio.

## Dettagli

- ID: `nerospace:rocket_launch_pad` · Attrezzo: piccone, livello ferro · Drop: se stessa
