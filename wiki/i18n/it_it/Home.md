# Wiki di Nerospace

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Home.md).

**Nerospace** è una mod di esplorazione spaziale e progressione tecnologica per Minecraft (Java Edition), per **NeoForge, Fabric e Forge** su **Minecraft 26.1.2, 26.2 e 26.3**. Estrai materiali alieni, costruisci macchine, fabbrica razzi, viaggia verso altri mondi, sopravvivi alle loro atmosfere prive d'aria con l'equipaggiamento per l'ossigeno, fonda le tue stazioni orbitali e infine **terraforma** un pianeta morto trasformandolo in terra abitabile e bagnata dalla pioggia.

> 💬 Domande, costruzioni, commenti? Unisciti al [Discord](https://discord.gg/ArPXvYUzJG) o apri una [issue su GitHub](https://github.com/Neroland/nerospace/issues).

## Per iniziare

> 🚀 **Nuovo giocatore? Comincia dalla guida passo passo [Per iniziare](Getting-Started.md).** Macchina ferma allo 0% di energia? Vedi **[Alimentare le macchine](Powering-Machines.md)**.

1. Estrai il **Minerale di Nerosium** nell'Overworld e fabbrica subito un **[Libro della Guida stellare](Star-Guide.md)**

   (Libro + Nerosium grezzo): è la versione nel gioco di questa wiki, un albero di progressione
   di 10 capitoli sempre aggiornato che mostra il tuo prossimo passo.

2. Estrai il **Minerale di Nerosteel** in profondità nell'Overworld (sotto y 16) e costruisci un **generatore**: ogni macchina ha bisogno di energia. Poi costruisci un **Macinatore di Nerosium** per raddoppiare la resa dei minerali tramite la **Polvere di Nerosium**.
3. Fabbrica un **razzo** di livello 1 e una **[Rampa di lancio](Rocket-Launch-Pad.md)** (il livello 1 richiede un solo blocco di rampa; il livello 2 una 3×3 completa) e vola alla Stazione

   orbitale; prosegui verso **Nerosteel** e **quarzo Xertz** su Greenxertz.

4. Porta con te una **[Tuta a ossigeno](Oxygen-Suit.md)** e installa dei **Generatori di ossigeno**: ogni mondo là fuori

   è privo d'aria. Porta le varianti **termica**/**criogenica** per Cindara e Glacira.

   - Automatizza lo scavo con un **[Controllore della cava](Quarry-Controller.md)**: segna un'area con

     i segnaposto (o delimitala con blocchi del telaio piazzati a mano) e scaverà da solo l'intero
     rettangolo fino al substrato roccioso. È la prima volta? Segui la
     **[Guida all'allestimento della cava](Quarry-Setup-Guide.md)**.

5. Fase avanzata: costruisci il **[Complesso di lancio pesante](Launch-Gantry.md)**, fonda stazioni con una

   **[Carta della stazione](Station-Charter.md)** e fai funzionare un **[Terraformatore](Terraformer.md)** finché un pianeta
   morto non si copre di vegetazione, si bagna di pioggia e si riempie di bestiame.

## Contenuti

- **Blocchi**: vedi la barra laterale per una pagina su ogni blocco (minerali, blocchi di stoccaggio, macchine, blocchi

  da costruzione della stazione, infrastrutture di lancio).

- **[Oggetti](Items.md)**: materiali, attrezzi, la famiglia delle Tute a ossigeno, razzi e carburante, carte e

  la Guida stellare.

- **[Creature](Creatures.md)**: le creature native di Greenxertz, Cindara e Glacira, più il

  bestiame della terraformazione.

- **[Villici alieni](Alien-Villagers.md)**: PNG alieni diffidenti con cui fare amicizia per commerciare e far crescere villaggi,

  con il **[Nucleo del villaggio](Village-Core.md)**, le **[strutture](Alien-Structures.md)** aliene,
  i **[blocchi decorativi](Alien-Decoration.md)** e l'**[equipaggiamento](Alien-Gear.md)** esclusivo.

- **[Eventi meteora](Meteor-Events.md)**: rari schianti di meteore che seminano nel mondo materiali alieni

  e minerali extraterrestri; seguili con il Localizzatore di meteore.

- **[Razzi](Rockets.md)**: i quattro scafi con equipaggio, le loro cabine illuminate e le finestre, e cosa fanno sulla rampa e durante il lancio.
- **[Piattaforma cargo](Cargo-Pad.md)** e **[Razzo cargo](Cargo-Rocket.md)**: trasporto merci automatizzato tra piattaforme e stazioni: carica una stiva, scegli una destinazione e il carico vola senza nessuno alle due estremità.
- **[Guida stellare](Star-Guide.md)**: la guida alla progressione nel gioco.
- **[API di integrazione pubblica](Public-API.md)**: per gli sviluppatori di mod, la superficie supportata per interrogare pianeti, ambiente e visite, contribuire con ossigeno e applicare sovrapposizioni di terraformazione reversibili.
- **[Compatibilità con le mod](Mod-Compatibility.md)**: cosa Nerospace condivide con le altre mod del tuo pack: energia, fluidi e oggetti attraversano il confine tra mod; il gas resta dalla parte di Nerospace.
- **[Gravità planetaria](Planet-Gravity.md)**: la gravità di ogni mondo, le variazioni per bioma e come la terraformazione la ripristina.
- **[Neroland Core](Neroland-Core.md)**: la libreria di base obbligatoria su cui è costruita Nerospace: un'unica rete energetica tra mod, tappe di progressione condivise, un comando unificato per privacy e cancellazione, la configurazione gestita da Core e i blocchi di stoccaggio condivisi ([Batteria](Battery.md), [Serbatoio di fluidi](Fluid-Tank.md), [Serbatoio di gas](Gas-Tank.md), [Deposito di oggetti](Item-Store.md), [Cestino](Trash-Can.md)) spostati in Core.
- **[Configurazione](Configuration.md)**: le sei chiavi moltiplicatore per regolare server e modpack.
- **[Backup dei dati salvati](Saved-Data-Backups.md)**: backup automatici dell'ultimo stato valido e ripristino a prova di crash dei dati del mondo di Nerospace.
- **[Roadmap](Roadmap.md)**: cosa è uscito nella 1.0 e cosa viene dopo.
- **[Funzionalità future](Future-Features.md)**: la lista dei desideri a lungo termine.

## Le dimensioni in sintesi

| Dimensione | Raggiunta con | Cielo | [Gravità](Planet-Gravity.md) | Note |
| --- | --- | --- | --- | --- |
| Overworld | — | normale | `1.0×` | Fonte del Nerosium. |
| **Stazione orbitale** | razzo di livello 1+ | cielo stellato spaziale | `~0.1×` | Una piattaforma in orbita, più qualsiasi [stazione fondata](Station-Charter.md) da te. Priva d'aria/vuoto. |
| **Greenxertz** | razzo di livello 2+ | giorno/notte + sole | `~0.6×` | Pianeta verde; Nerosteel + quarzo Xertz. Privo d'aria. |
| **Cindara** | razzo di livello 3+ | cielo stellato spaziale | `~0.8×` | Luna vulcanica; Cindrite. Priva d'aria + **pericolo calore** (consumo di O₂ ×4 senza una [Tuta termica](Oxygen-Suit.md)). |
| **Glacira** | razzo di livello 4 | cielo stellato spaziale | `~0.4×` | Luna ghiacciata; Glacite. Priva d'aria + **pericolo freddo** (consumo di O₂ ×4 senza una [Tuta criogenica](Oxygen-Suit.md)). |

Tutte le dimensioni di Nerospace sono **prive d'aria**: senza una zona respirabile (un Generatore di ossigeno, una Rampa di lancio, terreno terraformato o una Tuta a ossigeno completa) finirai l'ossigeno e soffocherai. Ognuna ha anche la **propria [gravità](Planet-Gravity.md)**, più leggera di casa sulle lune e quasi nulla in orbita, che la [terraformazione](Terraformer.md) riporta alla normalità man mano che recuperi il terreno.
