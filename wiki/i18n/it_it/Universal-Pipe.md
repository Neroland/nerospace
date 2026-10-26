# Tubo universale

<!-- nerospace:render -->

<p align="right"><img src="images/universal_pipe.png" alt="Universal Pipe" width="150" align="right"></p>
<!-- /nerospace:render -->

Un solo tubo per tutto: energia, fluidi, gas e oggetti scorrono nello stesso tubo traslucido, nello stesso momento.

## Panoramica

Il Tubo universale è la spina dorsale della logistica di Nerospace. I tubi piazzati si collegano automaticamente tra loro e a qualsiasi macchina, serbatoio o inventario, formando una **rete** che si comporta come un unico sistema condiviso. Tutti e quattro i livelli di risorse viaggiano contemporaneamente sullo stesso grafo di collegamenti:

| Livello                         | Colore                                                                                                                    | Regola                                                                                                                     |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Energia (FE) | rosso                                                                                                                     | riserva condivisa, bilanciata tra tutti i segmenti                                                                         |
| Fluidi                          | blu                                                                                                                       | **un fluido per rete**: il primo fluido che entra la occupa finché non viene svuotata                      |
| Gas                             | ciano (ossigeno), azzurro pallido (idrogeno), grigio (altri gas) | un gas per rete; **rompere un tubo ne libera il gas** (sbuffo visibile)                                 |
| Oggetti                         | —                                                                                                                         | viaggiano come **pacchetti visibili** (~2,5 blocchi/s), a rotazione tra le destinazioni |

## Obtaining

**Fabbricazione** (con forma, ne produce 8): una guaina di Nerosteel attorno a un nucleo di vetro:

```text
N N N
N G N
N N N
```

`N` = Lingotto di Nerosteel · `G` = Vetro

## Come funziona

- **Collegamenti:** il tubo estende un braccio verso qualsiasi cosa con cui può comunicare (tubi, macchine, serbatoi,

  bauli). Ogni faccia ha una modalità di I/O indipendente **per livello**: Auto → Ingresso → Uscita → Disattivato (si imposta con il
  [Configuratore](Configurator.md)). Questo include la [Batteria](Battery.md), il [Serbatoio di fluidi](Fluid-Tank.md),
  il [Serbatoio di gas](Gas-Tank.md), il [Deposito di oggetti](Item-Store.md) e il pozzo [Cestino](Trash-Can.md), tutti
  ora forniti da [Neroland Core](Neroland-Core.md); il tubo collega le capability
  `nerolandcore:fluid` (ed energia/oggetti) di Core ai propri lookup, quindi vi si collega
  esattamente come prima (punta una faccia in **USCITA** verso il Cestino per scaricare un flusso).

- **Energia/fluidi/gas:** la rete preleva dai fornitori, spinge verso i ricevitori e bilancia le proprie

  riserve: flussi pulsanti colorati mostrano cosa scorre e dove. Ogni livello contiene **8.000** (FE o mB)
  e sposta fino a **1.000 per tick**.

- **Oggetti:** le facce di prelievo estraggono dagli inventari; i pacchetti viaggiano fisicamente nel tubo,

  cambiano percorso agli incroci e **non si spargono mai**: se tutte le destinazioni sono piene si fermano e aspettano.
  Gli oggetti si muovono di **8 tick per blocco** (circa 2,5 blocchi al secondo). Una faccia di prelievo prende **16 oggetti
  ogni 4 tick**.
  Rompere un tubo fa cadere gli oggetti al suo interno.

- **Filtri e potenziamenti:** vedi [Filtri per tubi e potenziamenti](Pipe-Filters-and-Upgrades.md).

- **Lettura:** fai clic destro su un tubo a mani vuote per vederne il contenuto attuale; un clic destro da accovacciato

  a mani vuote fa uscire i potenziamenti installati.

## Tubi e macchine di altre mod

Il tubo non si limita ai blocchi di Nerospace. Su ogni loader ora offre e legge i gestori di fluidi e oggetti standard della piattaforma, quelli usati dal più ampio ecosistema di mod tecnologiche, così una linea di Nerospace e la logistica di un'altra mod si incontrano a metà strada:

- **Fluidi:** una faccia in **Uscita** spinge nel serbatoio o nella macchina per fluidi di un'altra mod, una faccia in **Ingresso** ne preleva,

  e il tubo per fluidi di un'altra mod può svuotare o riempire da solo un [Serbatoio di fluidi](Fluid-Tank.md),
  un [Serbatoio di carburante](Fuel-Tank.md), una [Raffineria di carburante](Fuel-Refinery.md), un [Controllore della cava](Quarry-Controller.md),
  un [Controllore di lancio](Launch-Controller.md) o un Tubo universale di Nerospace, senza bisogno di tubi di Nerospace
  lungo la linea.

- **Oggetti:** lo stesso, in entrambe le direzioni, per macchine, casse e tubi per oggetti di un'altra mod.

- L'**energia** attraversava già il confine tra mod tramite la rete condivisa di [Neroland Core](Neroland-Core.md).

- Il **gas** passa tra le mod Neroland tramite la capability per gas condivisa di Neroland Core

  (`nerolandcore:gas`). Il livello gas trasporta qualsiasi gas Neroland, non solo l'ossigeno: preleva idrogeno e
  ossigeno dalle macchine di NeroTech come l'elettrolizzatore, alimenta un processore chimico e riempie un
  [Serbatoio di gas](Gas-Tank.md) di Core con qualsiasi gas trasporti la linea. Anche le macchine di NeroTech possono spingere
  direttamente in un tubo. Entrambe le mod usano un unico ossigeno, `nerospace:oxygen`; l'ossigeno che NeroTech memorizzava
  con il suo vecchio id, `nerotech:oxygen`, viene letto come lo stesso gas. Non esiste ancora uno standard per i gas tra mod
  al di fuori delle mod Neroland, quindi il sistema di gas di un'altra mod non si collegherà.

Le modalità delle facce e i [filtri](Pipe-Filters-and-Upgrades.md) trattano un vicino esterno esattamente come uno di Nerospace: imposta una faccia su **Disattivato** con il [Configuratore](Configurator.md) e anche il tubo di un'altra mod vedrà lì una faccia chiusa, e una faccia filtrata lascia passare solo ciò che è impostata a far passare.

Un'avvertenza onesta: questa è la metà dell'accordo che spetta a Nerospace, che parla le API standard. Se una determinata altra mod si collega dipende dal fatto che le usi anch'essa.

## Consigli

Una sola linea può alimentare una macchina, darle oggetti e portarne via i prodotti allo stesso tempo: usa le modalità delle facce quando una linea serve sia una sorgente sia una destinazione (imposta su Ingresso la faccia lato sorgente così niente torna indietro).

## Dettagli

- ID: `nerospace:universal_pipe` · Attrezzo: piccone, livello ferro · Drop: se stesso
- Configurazione: il tubo non ha chiavi di configurazione proprie. Le velocità seguono i moltiplicatori globali: vedi

  [Configurazione](Configuration.md).
