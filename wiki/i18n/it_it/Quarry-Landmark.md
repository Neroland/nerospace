# Quarry Landmark

<!-- nerospace:render -->

<p align="right"><img src="images/quarry_landmark.gif" alt="Quarry Landmark" width="150" align="right"></p>
<!-- /nerospace:render -->

Un segnaposto d'angolo che definisce l'area che un [Controllore della cava](Quarry-Controller.md) estrarrà.

## Panoramica

I segnaposto sono il modo in cui disegni l'area di una cava. Ognuno proietta **laser di segnalazione** animati lungo gli assi orizzontali; piazzane **tre a L** e il controllore li interpreta come gli angoli di un rettangolo. Vengono consumati quando la cava si attiva.

> I segnaposto sono facoltativi: puoi invece delimitare tu stesso il rettangolo con **blocchi del telaio** piazzati a mano (l'Involucro del telaio si può piazzare) e mettere il controllore accanto all'anello chiuso: vedi [Controllore della cava](Quarry-Controller.md).

## Obtaining

**Fabbricazione** (con forma, ne produce 3):

```text
R
G
I
```

`R` = Redstone · `G` = Vetro · `I` = [Lingotto di Nerosteel](Items.md)

## Come funziona

- **Tre segnaposto = un rettangolo.** Due segnaposto si «collegano» quando condividono una riga o una colonna alla stessa Y

  entro la portata; una L di tre fornisce tutti e quattro i limiti del rettangolo.

- **Stesso livello Y.** Piazzali tutti e tre alla stessa altezza: quell'altezza diventa il piano di riferimento

  della cava (il telaio viene costruito lì; lo scavo va da appena sotto fino al substrato roccioso).

- **Entro il limite di dimensione.** Ogni lato del rettangolo può essere lungo fino a **64 blocchi** (il valore di configurazione

  `quarryMaxSide`). Una disposizione troppo grande o degenere semplicemente non viene acquisita: il
  controllore resta su «Inattivo: imposta i segnaposto o il telaio».

- **Collegamento.** Piazza il [Controllore della cava](Quarry-Controller.md) accanto o in linea con un segnaposto.

  All'attivazione analizza il gruppo, **rimuove i blocchi segnaposto** e costruisce il telaio.

- **Per il resto solo estetico:** i laser sono un effetto lato client; i segnaposto non hanno inventario né

  energia.

## Dettagli

- ID: `nerospace:quarry_landmark` · Attrezzo: qualsiasi attrezzo o a mani nude · Drop: se stesso
- Vedi la [Guida all'allestimento della cava](Quarry-Setup-Guide.md) per una guida passo passo e il [Controllore della cava](Quarry-Controller.md) per il riferimento completo.
