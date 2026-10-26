# Oggetti

Un riferimento per gli oggetti di Nerospace che non sono blocchi. (I blocchi hanno le proprie pagine: vedi la barra laterale.)

## Materiali

| Oggetto                          | Note                                                                                                                                                                                                  |
| -------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Lingotto di Nerosium**         | Si estrae dal [Minerale di Nerosium](Nerosium-Ore.md); fondilo (fornace o forno fusorio) in un lingotto di Nerosium.                                               |
| **Nerosium grezzo**              | Metallo di base delle prime fasi; serve per attrezzi, il macinatore e il nucleo della rampa di lancio.                                                                                |
| **Polvere di Nerosium**          | Prodotto del [Macinatore di Nerosium](Nerosium-Grinder.md); si fonde in un lingotto (raddoppia la resa dei minerali).                                              |
| **Lingotto di Nerosteel**        | Si estrae dal [Minerale di Nerosteel](Nerosteel-Ore.md) su Greenxertz; fondilo in un lingotto di Nerosteel.                                                                           |
| **Nerosteel grezzo**             | Il metallo portante di metà partita: razzi, tuta, macchine, blocchi della stazione.                                                                                   |
| **Quarzo Xertz**                 | Gemma lasciata dal [Minerale di quarzo Xertz](Xertz-Quartz-Ore.md); usata nei potenziamenti e nel Monitor di terraformazione.                                                         |
| **Cindrite**                     | Gemma dal [Minerale di Cindrite](Cindrite-Ore.md) su Cindara; potenziamento di livello 3 del Terraformatore, pezzi di livello 2 + [Tuta termica](Oxygen-Suit.md), razzo di livello 4. |
| **Glacite**                      | Gemma dal [Minerale di Glacite](Glacite-Ore.md) su Glacira; pezzi della [Tuta criogenica](Oxygen-Suit.md) e ciclo dell'acqua del [Modulo di idratazione](Hydration-Module.md).        |
| **Frammento alieno**             | Bottino comune da un [Nucleo di meteora](Meteor-Core.md); futura materia prima per lo scanner.                                                                                        |
| **Rottami di tecnologia aliena** | Bottino non comune delle meteore; futura fabbricazione di potenziamenti.                                                                                                              |
| **Nucleo alieno**                | Bottino raro delle meteore; requisito di alto valore per scanner/potenziamenti.                                                                                                       |

## Attrezzi

- **Piccone di Nerosium**: estrazione di livello ferro, maggiore durabilità e un piccolo bonus d'attacco. Fabbricazione:

  3 lingotti di Nerosium sopra due bastoni (schema standard del piccone).

- **[Configuratore](Configurator.md)**: lo strumento della rete di tubi: modalità di I/O per faccia × per livello, con un

  pannello di configurazione completo con clic destro da accovacciato.

- **Filtro per tubi / Filtro per tubi avanzato / Potenziamento di velocità / Potenziamento di capacità**: vedi

  [Filtri per tubi e potenziamenti](Pipe-Filters-and-Upgrades.md).

## Tuta a ossigeno

Un set di armatura di Nerosteel in quattro pezzi (**Elmo, Corazza, Gambiere, Stivali**) che fa anche da supporto vitale personale. Indossare il **set completo** ti permette di respirare fuori dalla Terra lontano da un generatore o da una rampa; attinge da una riserva d'aria limitata che si ricarica in qualsiasi zona respirabile (quindi allunga molto la tua autonomia senza essere infinita). Protezione di classe diamante, si ripara con il Nerosteel.

La tuta si ricarica anche nelle **camere stagne**: stare entro pochi blocchi da un [Serbatoio di gas](Gas-Tank.md) (ora fornito da [Neroland Core](Neroland-Core.md)) o da un [Generatore di ossigeno](Oxygen-Generator.md) che contiene ossigeno ricarica la tuta da quella riserva (consumandone il gas). Una **Tuta a ossigeno di livello 2** (ogni pezzo = pezzo di livello 1 + 4 Cindrite) è un'armatura più resistente. **Non** contiene più aria: ogni tuta completa contiene 900 aria e si ricarica alla stessa velocità.

Due **varianti anti-pericolo** della tuta di livello 2 ti proteggono dagli estremi planetari (l'esposizione senza protezione **quadruplica** il consumo di ossigeno): la **Tuta termica** (pezzo L2 + 4 Cindrite) per il calore di Cindara e la **Tuta criogenica** (pezzo L2 + 4 Glacite) per il freddo di Glacira. Tutti e quattro i pezzi devono corrispondere perché la protezione si attivi. Vedi **[Tuta a ossigeno](Oxygen-Suit.md)** per la pagina completa.

## Razzi e carburante

- **Tanica di carburante per razzi**: un'unità di carburante impilabile. Fabbricazione (senza forma): polvere di blaze + carbone +

  lingotto di ferro → 2 taniche. Serve per rifornire razzi e macchine e come nucleo di fabbricazione. (Raggiungibile prima
  del tuo primo lancio: non servono materiali extraterrestri.)

- **[Raffineria di carburante](Fuel-Refinery.md)**: automatizza il carburante: carbone + polvere di blaze + energia di rete → carburante per razzi

  liquido trasportabile con i tubi, l'alternativa da logistica alla fabbricazione a mano delle taniche.

- **Secchio di carburante per razzi**: un secchio del fluido `rocket_fuel`; versalo in un razzo o in un [Serbatoio di carburante](Fuel-Tank.md).

- **Razzi (livello 1 / 2 / 3 / 4)**: schierali su una [Rampa di lancio per razzi](Rocket-Launch-Pad.md), riforniscili,

  sali a bordo, scegli una destinazione e lancia (le destinazioni sono cumulative):

  - **Livello 1** → la Stazione orbitale (e qualsiasi [stazione fondata](Station-Charter.md)).

  - **Livello 2** → + Greenxertz.

  - **Livello 3** → + Cindara. (Richiede una rampa 3×3 circondata dalla [Parete della stazione](Station-Wall.md) **oppure** un

    [Complesso di lancio pesante](Launch-Gantry.md).)

  - **Livello 4** → + Glacira. (Si schiera **solo** sul [Complesso di lancio pesante](Launch-Gantry.md);

    serbatoio da 24.000 mB.)
    La schermata nel razzo mostra un indicatore del carburante in tempo reale, una traiettoria di destinazione selezionabile, un selettore
    di stazione e un pulsante Lancia, con il tuo inventario disposto in una griglia di slot sotto i comandi.
    Le mod che ingrandiscono l'inventario principale del giocatore (come _Inventory Extended_ su Fabric) sono
    supportate automaticamente: la console si allarga per mostrare le righe aggiuntive.

- **[Scafo cargo](Cargo-Rocket.md)**: un baule in un anello di ferro e Nerosteel; il prodotto intermedio per il

  Razzo cargo e la [Piattaforma cargo](Cargo-Pad.md).

- **[Razzo cargo](Cargo-Rocket.md)**: un razzo senza equipaggio (razzo di livello 2 + Scafo cargo + tanica, in

  Nerosteel) che si schiera solo su una **Piattaforma cargo** e trasporta la sua stiva a un'altra piattaforma o stazione.

- **Cassa cargo**: non fabbricabile: il carico di un volo cargo che non è stato possibile consegnare in tempo,

  lasciato cadere a destinazione così non si perde nulla. Fai clic destro per disimballarla.

- **[Carta della stazione](Station-Charter.md)**: 8 [Parete della stazione](Station-Wall.md) attorno a un

  [Pavimento della stazione](Station-Floor.md); rinominala in un'incudine, portala a bordo e scegli il nodo
  **FOUND** nell'interfaccia del razzo per fondare la tua stazione orbitale con nome.

## Guida stellare

- **Libro della Guida stellare**: Libro + Nerosium grezzo (senza forma). Fai clic destro per aprire la

  **[Guida stellare](Star-Guide.md)**, l'albero di progressione interattivo (10 capitoli, 45 passi).

- **[Guida stellare](Star-Guide.md)** (blocco piedistallo): contiene il libro e proietta un ologramma del prossimo obiettivo.

## Cibo e drop delle creature

- **Coscia di corridore**: lasciata dal Corridore dei prati; cibo sostanzioso.
- **Coscia di pollo di brace**: lasciata dal Pollo di brace; cibo.
- **Vello di vagante**: lasciato dal Vagante lanoso; si fabbrica in 4 fili.

  Vedi **[Creature](Creatures.md)** per gli animali stessi.

## Eventi meteora

- **Localizzatore di meteore**: indica la meteora più vicina (lettura nella barra delle azioni mentre lo tieni: stato, direzione,

  distanza). Fabbricazione (con forma):

  ```text
  . N .
  R C R
  . N .
  ```

  `N` = Lingotto di Nerosteel · `R` = Redstone · `C` = Bussola

- **Evocatore di meteore**: solo in creativa: fai clic destro su un blocco per farci cadere sopra una meteora.

  Vedi **[Eventi meteora](Meteor-Events.md)** per l'evento del mondo completo, la tabella del bottino e la configurazione.

## Dispositivi di viaggio (creativa)

- Il **Navigatore di Greenxertz** e le **Bussole della stazione / di Greenxertz / di Cindara / di Glacira** sono

  aiuti al viaggio con un clic solo in creativa, per test e costruzioni (nessuna ricetta in sopravvivenza: i razzi sono la
  strada in sopravvivenza).

## Uova generatrici

Esistono uova generatrici creative per tutte e otto le creature: vedi **[Creature](Creatures.md)**.
