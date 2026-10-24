# Tuta a ossigeno

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Oxygen-Suit.md).

Armatura in quattro pezzi che fa anche da supporto vitale personale nei mondi senza aria. Due livelli, più due **varianti anti-pericolo** di classe livello 2 per i mondi estremi.

## Panoramica

Indossare il **set completo** (tutti e quattro i pezzi) ti fa respirare fuori dalla Terra consumando una riserva d'aria limitata invece di soffocare. La riserva si ricarica all'istante in qualsiasi zona respirabile (area di ossigeno, terreno terraformato, zona sicura della rampa di lancio) e, grazie all'integrazione tra tuta e stazione, anche nelle **camere stagne** (vedi sotto).

| | Tuta a ossigeno di livello 1 | Tuta a ossigeno di livello 2 |
| --- | --- | --- |
| Riserva d'aria | 900 | 900 |
| Ricarica in camera stagna | 90 aria / 10 tick | 90 aria / 10 tick |
| Protezione | classe diamante | un po' più resistente, +robustezza |
| Materiale di riparazione | Lingotto di Nerosteel | Cindrite |

Ogni tuta completa (livello 1, livello 2, termica o criogenica) contiene **900 aria**. Senza tuta hai solo **300 aria** (i tuoi polmoni). Un set **misto** funziona comunque come supporto vitale. Manca un pezzo = niente supporto vitale.

**Consumo:** con una tuta completa perdi **3 aria** a ogni controllo (ogni 10 tick). Senza tuta perdi **30 aria** a ogni controllo. Un pericolo non schermato (vedi sotto) moltiplica il consumo per **4**.

## Varianti anti-pericolo: Tuta termica e Tuta criogenica

Cindara è **calda** e Glacira è **fredda**: senza protezione, il pericolo planetario fa lavorare la tua tuta oltre il dovuto e l'ossigeno si consuma a **4×** la velocità normale (non ci sono danni separati; il pericolo resta esaurire l'aria). Il badge dell'HUD avverte con un **CALDO!** / **FREDDO!** rosso mentre sei esposto, con sbuffi di brace (Cindara) o una vignettatura di brina che avanza (Glacira) come segnale.

| | **Tuta termica** | **Tuta criogenica** |
| --- | --- | --- |
| Protegge da | calore di Cindara | freddo di Glacira |
| Fabbricazione (per pezzo) | pezzo di livello 2 + 4 **Cindrite** | pezzo di livello 2 + 4 **Glacite** |
| Materiale di riparazione | Cindrite | Glacite |
| Classe | Livello 2 (riserva 900) | Livello 2 (riserva 900) |

La protezione si attiva solo quando **tutti e quattro i pezzi corrispondono** al pericolo; il badge dell'HUD mostra allora **TUTA CALDO** / **TUTA FREDDO** e l'ossigeno si consuma alla normale velocità 1×. Mischiare pezzi delle varianti con altri pezzi mantiene il supporto vitale: perdi solo la protezione dal pericolo.

## Nota sulla progressione

La Cindrite (termica) si estrae proprio su Cindara, quindi il ciclo abituale è: entra di corsa con una normale tuta di livello 2, estrai la tua prima Cindrite, poi fabbrica la Tuta termica per le spedizioni serie. Lo stesso vale per la Glacite e la Tuta criogenica su Glacira.

## Ricarica in camera stagna

Entro **4 blocchi** da un [Serbatoio di gas](Gas-Tank.md) o da un [Generatore di ossigeno](Oxygen-Generator.md) **che contiene ossigeno**, una tuta indossata si ricarica da quella riserva. Ogni tuta riceve **90 aria** a ogni controllo (ogni 10 tick). Ogni unità d'aria costa **2 mB** di ossigeno (scalati da `fuelCostMultiplier`). Un Serbatoio di gas all'ingresso della base funziona quindi come camera stagna: entra, fai il pieno, riparti. Funziona anche il Serbatoio di gas creativo.

## Come ottenerla

**Livello 1** (con forma, Nerosteel): elmo `NNN / NGN` (G = visiera di vetro), corazza `N N / NCN / NNN` (C = Tanica di carburante per razzi come nucleo del supporto vitale), gambiere e stivali tutti in Nerosteel.

**Livello 2** (con forma): ogni pezzo di livello 1 circondato da 4 **Cindrite**:

```text
. C .
C P C
. C .
```

`P` = il pezzo di livello 1 corrispondente · `C` = Cindrite

La Cindrite si estrae solo su **Cindara** (razzo di livello 3, a sua volta vincolato alla [Parete della stazione](Station-Wall.md)), quindi la tuta di livello 2 si trova alla stessa profondità di progressione del razzo di livello 3.

## Dettagli

- ID: `nerospace:oxygen_suit_{helmet,chestplate,leggings,boots}`,

  `nerospace:oxygen_suit_t2_{...}`, `nerospace:oxygen_suit_heat_{...}` (termica),
  `nerospace:oxygen_suit_cold_{...}` (criogenica)

- Configurazione: `oxygenCapacityMultiplier` (capacità d'aria) e `oxygenDrainMultiplier` (velocità di consumo). Vedi

  [Configurazione](Configuration.md).
