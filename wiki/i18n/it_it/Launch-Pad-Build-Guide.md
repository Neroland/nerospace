# Guida alla costruzione della rampa di lancio

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Launch-Pad-Build-Guide.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/rocket_launch_pad.png" alt="Rocket Launch Pad" width="150" align="right"></p>
<!-- /nerospace:render -->

Una guida passo passo per costruire una rampa di lancio per ogni livello di razzo, da un singolo blocco per il tuo primo volo fino al Complesso di lancio pesante. Per il riferimento completo sui blocchi vedi [Rampa di lancio per razzi](Rocket-Launch-Pad.md) e [Torre di lancio](Launch-Gantry.md).

## In sintesi

Ogni livello di razzo richiede una rampa più grande. Una rampa più grande funziona sempre per un razzo più piccolo.

| Livello della rampa | Cosa costruire | Lancia | Sblocca |
| --- | --- | --- | --- |
| **1** | Un blocco Rampa di lancio | Razzi di livello 1 | La Stazione orbitale |
| **2** | Un 3×3 completo di Rampe di lancio | Razzi di livello 1–2, Razzi cargo | Greenxertz |
| **3** | Un 3×3 di Rampe di lancio, circondato da 16 Pareti della stazione | Razzi di livello 1–3 | Cindara |
| **4** | Un 5×5 di Rampe di lancio più una Torre di lancio (il **Complesso di lancio pesante**) | Ogni razzo | Glacira |

1. Fabbrica le parti per il livello che vuoi.
2. Disponile su un terreno piano, **tutte alla stessa altezza**.
3. Fai clic destro su un blocco della rampa a **mani vuote** per controllare quale livello viene riconosciuto.
4. Fai clic destro sulla rampa con il razzo per schierarlo, poi fai rifornimento.

## 1. Fabbrica le parti

| Parte | Ricetta | Produce |
| --- | --- | --- |
| [Rampa di lancio per razzi](Rocket-Launch-Pad.md) | 8 lingotti di Nerosteel attorno a un [Blocco di Nerosium](Block-of-Nerosium.md) | 1 |
| [Blocco di Nerosium](Block-of-Nerosium.md) | 9 lingotti di Nerosium | 1 |
| [Parete della stazione](Station-Wall.md) | 8 lingotti di Nerosteel attorno a un lingotto di ferro | 8 |
| [Torre di lancio](Launch-Gantry.md) | 6 lingotti di Nerosteel, sbarre di ferro e una Parete della stazione (vedi la sua pagina) | 1 |

**Quanto costa ogni livello, partendo da zero:**

| Livello della rampa | Blocchi | Lingotti di Nerosteel | Lingotti di Nerosium | Altro |
| --- | --- | --- | --- | --- |
| 1 | 1 rampa | 8 | 9 | — |
| 2 | 9 rampe | 72 | 81 | — |
| 3 | 9 rampe + 16 Pareti della stazione | 88 | 81 | 2 lingotti di ferro |
| 4 | 25 rampe + 1 Torre di lancio | 214 | 225 | 1 lingotto di ferro, 1 sbarre di ferro |

> **Consiglio:** ogni livello riutilizza il precedente. Passare dal livello 2 al livello 3 costa solo le 16 pareti, e quando passi al livello 4 puoi recuperarle (una di esse va nella torre).

## 2. Scegli il posto

- **Piano e livellato.** I blocchi della rampa si uniscono solo quando sono **affiancati alla stessa altezza**. I blocchi che si toccano solo in un angolo, o che sono un blocco più in alto, non contano.
- **Tienile separate.** Ogni blocco di rampa che ne tocca un altro diventa un'unica rampa, e una rampa ospita **un solo razzo**. Lascia almeno un blocco di spazio tra le rampe che vuoi usare per razzi diversi.
- **Spazio per respirare.** Puoi respirare entro circa 6 blocchi da una rampa di lancio, così chi arriva non soffoca all'atterraggio. Tutto ciò che è più lontano richiede una base con un [Generatore di ossigeno](Oxygen-Generator.md).
- **Sulla Stazione orbitale** c'è già una rampa 3×3 costruita per te.

## 3. Costruisci il tuo livello

Legenda degli schemi qui sotto (visti dall'alto): `P` = Rampa di lancio, `W` = Parete della stazione, `G` = Torre di lancio, `F` = [Serbatoio di carburante](Fuel-Tank.md) (facoltativo), `.` = qualsiasi cosa.

### Livello 1: un blocco

```text
P
```

Piazza una Rampa di lancio. Basta per un razzo di livello 1 e per il tuo primo viaggio verso la Stazione orbitale.

### Livello 2: un 3×3 completo

```text
P P P
P P P
P P P
```

Tutti e nove i blocchi, senza buchi. Blocchi di rampa in più attaccati ai lati vanno bene, purché all'interno ci sia un quadrato 3×3 completo.

### Livello 3: un 3×3 circondato dalla Parete della stazione

```text
W W W W W
W P P P W
W P P P W
W P P P W
W W W W W
```

Circonda il 3×3 con **16 [Pareti della stazione](Station-Wall.md)** alla **stessa altezza delle rampe**, angoli compresi. Tutte e 16 devono essere Pareti della stazione: un solo buco o un blocco diverso e la rampa torna al livello 2.

> L'anello di pareti non lascia spazio accanto alle rampe, quindi un Serbatoio di carburante non può stare vicino a una rampa di livello 3. Riforniscila invece dal basso (vedi [Riforniscilo](#5-fuel-it)).

### Livello 4: il Complesso di lancio pesante

```text
. . . G . . .
. P P P P P .
. P P P P P .
. P P P P P .
. P P P P P .
. P P P P P .
. . . F . . .
```

Un **5×5** completo di Rampe di lancio, più almeno una **[Torre di lancio](Launch-Gantry.md)** subito accanto al quadrato all'altezza della rampa. Non servono Pareti della stazione.

- **Metti la torre al centro di un lato**, a contatto con un blocco di rampa. Una torre in un angolo forma comunque il complesso, ma non tocca nessuna rampa, quindi facendo clic destro non sali sul razzo.
- **Passare dal livello 3:** smonta le 16 pareti, riempi quei posti con Rampe di lancio per formare il 5×5, poi aggiungi la torre.
- Un Serbatoio di carburante su qualsiasi altro lato di un complesso pesante pompa alla velocità massima.

## 4. Controlla e schiera il razzo

**La rampa ti dice quando è pronta.** Quando il blocco che piazzi completa un livello (l'ultima rampa del 3×3, la sedicesima Parete della stazione o la torre), compare un messaggio sopra la barra degli oggetti, la rampa suona e scintilla:

> ✔ Rampa di lancio pronta: **livello 3**: piazza un razzo

Non c'è nient'altro da attivare. Quando vedi quel messaggio, resta solo da piazzare un razzo.

**Fai clic destro su un qualsiasi blocco della rampa a mani vuote** per controllarla in qualsiasi momento. La chat mostra livello e dimensione della rampa, conferma che è pronta per un razzo e dice cosa costruire dopo:

> Rampa di lancio **livello 2** · 9 blocchi
> ✔ Pronta: piazza un razzo (fino al **livello 2**)
> Prossimo passo: circonda il 3x3 con 16 Pareti della stazione per il livello 3

Non accovacciarti mentre lo fai: un clic da accovacciato a mani vuote su una rampa che hai nominato come tappa di viaggio la rimuove dall'elenco dei viaggi.

**Schierare:** fai clic destro sulla rampa con l'oggetto razzo. Il razzo si posiziona al centro del 5×5 (o del 3×3, se non c'è un 5×5). Puoi schierarlo su qualsiasi rampa, anche una costruita a metà, e completare la costruzione attorno al razzo. Il controllo della dimensione avviene solo **al lancio**. Se la rampa è troppo piccola, la chat ti dice quale livello viene riconosciuto. Comunque, è più semplice completare prima la rampa così il razzo si posiziona al centro.

<a id="5-fuel-it"></a>

## 5. Riforniscilo

| Rampa | Un Serbatoio di carburante pompa |
| --- | --- |
| Rampa parziale (non un quadrato completo) | 40 mB per tick |
| 3×3 completo | 160 mB per tick (4 volte più veloce) |
| Complesso di lancio pesante | 480 mB per tick (12 volte più veloce) |

- **[Serbatoio di carburante](Fuel-Tank.md):** piazzalo accanto a un qualsiasi blocco della rampa, all'altezza della rampa. Pompa in qualunque razzo si trovi sulla rampa.
- **[Tubo universale](Universal-Pipe.md):** qualsiasi blocco della rampa accetta carburante per razzi (livello fluidi), ossigeno (livello gas) e Taniche di carburante per razzi (livello oggetti) e li passa al razzo attraccato. È il modo per rifornire una rampa di **livello 3**: porta il tubo fino a un blocco della rampa da sotto.
- **A mano:** fai clic destro sul razzo con le Taniche di carburante per razzi.

Un viaggio verso un altro mondo costa **2.500 mB**. Porta taniche di scorta per poter tornare a casa.

## La scorciatoia: il Controllore di lancio

Un [Controllore di lancio](Launch-Controller.md) costruisce la rampa al posto tuo.

1. Piazza il controllore rivolto verso un terreno aperto e piano. Costruisce la rampa **davanti a sé**, centrata **a 6 blocchi di distanza**, all'altezza del controllore.
2. Carica Rampe di lancio, Pareti della stazione e Torri di lancio nei suoi tre slot.
3. Scegli un livello (T1–T4). La lettura mostra quanti blocchi di ogni tipo servono ancora.
4. Premi **Attiva ologramma** per vedere un'anteprima della disposizione, poi **Costruisci rampa**.

Costruisce sopra ciò che c'è già, quindi puoi potenziare una rampa livello per livello senza demolirla. Per il livello 4 sostituisce l'anello di pareti con blocchi di rampa e mette la torre al centro del lato opposto. Il controllore conserva anche carburante, ossigeno ed energia e può lanciare il razzo dalla sua schermata.

## Risoluzione dei problemi

- **«Questa rampa di lancio è riconosciuta come livello X, ma questo è un razzo di livello Y»** → il quadrato sotto il razzo non è completo. Fai clic destro su una rampa a mani vuote per vedere cosa manca. Il quadrato deve essere quello su cui si trova il razzo, non un altro altrove sulla stessa rampa.
- **La rampa viene riconosciuta a un livello più basso del previsto** → controlla che ogni blocco sia alla stessa altezza, che non manchi alcun blocco di rampa e che l'anello del livello 3 abbia tutte e 16 le Pareti della stazione, angoli compresi.
- **«Rampa occupata»** → la rampa ha già un razzo. Due rampe che si toccano contano come una sola.
- **Un razzo schierato non parte più** → un blocco di rampa, una parete o la torre sotto di esso è stato rotto. Rimettilo al suo posto.
- **La torre non ti fa salire a bordo** → è in un angolo. Spostala al centro di un lato.
- I **Razzi cargo** richiedono una [Piattaforma cargo](Cargo-Pad.md) all'interno di un 3×3 completo. La Piattaforma cargo conta come uno dei nove blocchi di rampa.

---

Vedi anche: [Rampa di lancio per razzi](Rocket-Launch-Pad.md), [Torre di lancio](Launch-Gantry.md), [Controllore di lancio](Launch-Controller.md), [Parete della stazione](Station-Wall.md), [Serbatoio di carburante](Fuel-Tank.md), [Per iniziare](Getting-Started.md).
