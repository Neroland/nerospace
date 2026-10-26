# Compatibilità con le mod

Cosa Nerospace condivide con le altre mod del tuo pack e cosa tiene per sé. Niente di tutto ciò richiede configurazione: è il modo in cui si comportano i blocchi quando sono installate entrambe le mod. (Per la superficie di integrazione destinata agli sviluppatori, vedi [API di integrazione pubblica](Public-API.md).)

## Cosa attraversa il confine tra mod

| Livello                         | Attraversa?           | How                                                                                                                                                                     |
| ------------------------------- | --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Energia (FE) | **sì**                | Tramite la rete energetica condivisa di [Neroland Core](Neroland-Core.md).                                                                              |
| Fluidi                          | **sì**                | Nerospace offre e legge il gestore di fluidi standard della piattaforma su NeoForge, Forge e Fabric.                                                    |
| Oggetti                         | **sì**                | Lo stesso, sul gestore di oggetti standard della piattaforma, più i contenitori del gioco base come sempre.                                             |
| Gas                             | **solo mod Neroland** | Tramite la capability per gas condivisa di [Neroland Core](Neroland-Core.md). Oltre a questa non esiste uno standard per i gas tra mod. |

In pratica un [Tubo universale](Universal-Pipe.md) si collega a serbatoi, macchine e tubi di un'altra mod, ne preleva e vi immette, e i tubi di un'altra mod possono svuotare o riempire direttamente un [Serbatoio di fluidi](Fluid-Tank.md), un [Serbatoio di carburante](Fuel-Tank.md), una [Raffineria di carburante](Fuel-Refinery.md), un [Controllore della cava](Quarry-Controller.md), un [Controllore di lancio](Launch-Controller.md) o un Tubo universale di Nerospace, senza alcun tubo di Nerospace lungo la linea. Una [cava](Quarry-Controller.md) può espellere gli oggetti estratti e l'acqua che assorbe in qualsiasi sistema di stoccaggio tu stia già usando.

## L'unica eccezione: la Rampa di lancio per razzi

La connessione per fluidi della [Rampa di lancio per razzi](Rocket-Launch-Pad.md) è a senso unico: rifornisce un razzo attraccato e non può mai essere svuotata. I gestori di fluidi standard sono transazionali: un'altra mod può inserire e poi annullare, come modo ordinario per chiedere «quanto ne prenderesti?», e un pozzo a senso unico non può rispondere onestamente. Per questo la rampa accetta carburante solo dai tubi di Nerospace. Per rifornire un razzo dal sistema di un'altra mod, convoglia il carburante in un [Serbatoio di carburante](Fuel-Tank.md) accanto alla rampa e lascia che sia lui a rifornire il razzo come al solito.

## Le impostazioni delle facce restano valide

Le modalità di I/O per faccia e i [filtri per tubi](Pipe-Filters-and-Upgrades.md) trattano un vicino esterno esattamente come uno di Nerospace. Una faccia impostata su **Disattivato** con il [Configuratore](Configurator.md) risulta chiusa anche per il tubo di un'altra mod, e una faccia filtrata lascia passare solo ciò che le hai detto: la portata tra mod non ti fa mai perdere il controllo su cosa va dove.

## I gas restano all'interno delle mod Neroland

Il livello gas del tubo sposta gas tra Nerospace, [Neroland Core](Neroland-Core.md) e le altre mod Neroland, come NeroTech, tramite la capability per gas condivisa di Core. Trasporta qualsiasi loro gas: ossigeno da un [Generatore di ossigeno](Oxygen-Generator.md) o da un elettrolizzatore, idrogeno da un elettrolizzatore, verso un [Serbatoio di gas](Gas-Tank.md) o un processore chimico. L'ossigeno è un unico gas, `nerospace:oxygen`, in ogni mod Neroland. Non esiste uno standard per i gas oltre le mod Neroland, quindi non aspettarti che il sistema di gas di un'altra mod lo legga, né che Nerospace legga il loro.

## Ricette

Le ricette di Nerospace sono basate sui tag ovunque, quindi gli ingredienti equivalenti di un'altra mod (il suo vetro, la redstone, i drop dei minerali e i tag di materiale comuni `c:`) funzionano ovunque il tag corrisponda.

## Missioni (facoltative: NeroQuests)

Nerospace include un capitolo di missioni, **Diario del viaggiatore stellare** (19 missioni), che compare nel libro delle missioni quando è installata la mod sorella **NeroQuests**. Segue la progressione della mod (primo Nerosium, energia, lancio, orbita, i pianeti, tute, colonie, trasporto merci e terraformazione) più una missione condivisa del registro del mondo per cento consegne cargo. NeroQuests non è obbligatoria: senza di essa il pacchetto è un insieme di dati inerte nel jar.

Le tappe dietro queste missioni sono normali requisiti di progressione ed eventi a soglia di Neroland Core, quindi qualsiasi mod basata su Core può leggerle: i requisiti di registrazione con ambito giocatore `nerospace:first_freight`, `nerospace:terraformed_ground` e `nerospace:living_world`, e il canale del mondo `nerospace:cargo_delivered` (ambito = id della dimensione di destinazione; soglie 1 / 10 / 100 / 1000). Due requisiti _hook_, `nerospace:hook/reveal_star_guide_page` e `nerospace:hook/oxygen_top_up`, funzionano al contrario: aprine uno per un giocatore e Nerospace gli invia un suggerimento della Guida stellare o gli ricarica l'ossigeno una volta, poi richiude il requisito. Nessuno di questi requisiti blocca qualcosa.

## L'avvertenza da leggere

Tutto quanto sopra è la metà dell'accordo che spetta a Nerospace: parla le API standard e native della piattaforma. Se una _particolare_ altra mod si collega dipende dal fatto che le usi anch'essa. Se un blocco di un'altra mod si rifiuta di collegarsi, è la prima cosa da controllare.

---

Vedi anche: [Tubo universale](Universal-Pipe.md), [Neroland Core](Neroland-Core.md), [API di integrazione pubblica](Public-API.md), [Serbatoio di fluidi](Fluid-Tank.md).
