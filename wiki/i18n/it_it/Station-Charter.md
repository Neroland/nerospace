# Carta della stazione

Fonda la tua stazione orbitale, nome compreso.

## Panoramica

La Carta della stazione crea **stazioni aggiuntive** oltre alla [Stazione orbitale](Home.md) condivisa.
Fai clic destro per aprire una console di denominazione, scrivi un nome e conferma: una stazione nuova di zecca viene costruita in orbita e ancorata da un **[Nucleo della stazione](Station-Core.md)**. Da quel momento compare come destinazione nell'interfaccia di ogni razzo, per ogni giocatore e ogni livello di razzo.

La carta **non** ti teletrasporta: per visitarla vola con un razzo fino alla Stazione orbitale, così il razzo resta il tuo mezzo di viaggio.

## Obtaining

**Fabbricazione** (con forma):

```text
W W W
W F W
W W W
```

`W` = [Parete della stazione](Station-Wall.md) · `F` = [Pavimento della stazione](Station-Floor.md)

## Come funziona

- **Fondazione:** fai clic destro sulla carta per aprire la **schermata di denominazione**, scrivi un nome e premi _Fonda stazione_. Una carta viene consumata e la stazione viene costruita a distanza. Un nome vuoto diventa automaticamente «Station N».
- **In the station dimension:** the station is founded **where you stand**, and you can untick **Build starter
  structure** to get only your claim and a Docking Ring. A spot too close to another station (512 blocks) or too
  far from 0,0 (30,000 blocks) is refused and the charter is kept. See
  [Orbital Stations](Orbital-Stations#placing-your-own-station).
- **Cosa viene costruito:** una sala di osservazione chiusa (pilastri di Parete della stazione, fasce di finestre di vetro, soffitto illuminato) con una **camera stagna** che porta a una **piattaforma di atterraggio di livello 2**, il punto in cui atterra un razzo quando voli lì.
- **Anche dal razzo:** il selettore di attracco nel razzo include un'opzione **Nuova stazione**: sceglila e lancia per fondare una nuova stazione e volarci in un solo viaggio.
- **Visiting:** fly a rocket, choose **Orbital Station**, then pick the station you want in the
  **Dock at** list. Your own station is picked for you by default (see [Landing Pads](Landing-Pads)).
- **Capacità:** fino a **64** stazioni per mondo. La fondazione assegna il progresso _stazione fondata_ e spunta la Guida stellare.
- **Proprietà:** solo tu (il fondatore) puoi **rinominare** o **rimuovere** la tua stazione; chiunque può **volarci**. Il tuo ID è memorizzato localmente solo per questo controllo: mai registrato nei log né condiviso.

## Dettagli

- ID: `nerospace:station_charter`
