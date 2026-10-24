# Eventi meteora

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Meteor-Events.md).

Le meteore in caduta sono un **evento del mondo**: rari schianti che seminano nell'Overworld (e sui pianeti) materiali alieni e una spinta iniziale di minerali extraterrestri, senza bisogno di un razzo per avere un primo assaggio di ciò che c'è là fuori.

## Panoramica

Ogni tanto, vicino a un giocatore attivo, viene programmata la caduta di una meteora. Dopo una breve finestra di preavviso scende dal cielo con un arco infuocato, si schianta al suolo e lascia un piccolo **cratere di [Roccia di meteora](Meteor-Rock.md)** attorno a un luminoso **[Nucleo di meteora](Meteor-Core.md)**, lo «scrigno» che contiene il bottino casuale. Lo schianto è volutamente contenuto (un cratere di pochi blocchi, nessuna grande esplosione, nessun incendio), così non rovina le tue costruzioni.

Gli impatti avvengono **quando ti avvicini** al sito: le meteore vengono programmate lontano e cadono solo quando l'area è caricata, quindi seguire il preavviso verso il sito fa parte della caccia.

## Il Localizzatore di meteore

Il **Localizzatore di meteore** è una bussola di preallarme. Mentre lo tieni in mano, mostra nella barra delle azioni la meteora tracciata più vicina:

- **Stato**: *In arrivo* (ancora in caduta / non ancora atterrata) o *Atterrata* (il cratere ti aspetta).
- **Direzione**: una direzione della bussola (N, NE, E, …) verso il sito.
- **Distanza**: quanto è lontana, in metri.

Puoi fabbricarlo in sopravvivenza (con forma):

```text
. N .
R C R
. N .
```

`N` = Lingotto di Nerosteel · `R` = Redstone · `C` = Bussola

## Bottino

Rompere il [Nucleo di meteora](Meteor-Core.md) sparge il contenuto della meteora (rompi e raccogli). Il bottino viene estratto **una sola volta** quando la meteora atterra e poi fissato, quindi è lo stesso per chiunque la raggiunga: niente trucchi per ripetere l'estrazione. Una meteora porta sempre una manciata di **Frammenti alieni**, più estrazioni bonus ponderate di minerali grezzi esistenti (Nerosium grezzo, Nerosteel grezzo, quarzo Xertz) e dei più rari **Rottami di tecnologia aliena** e **Nucleo alieno**.

| Oggetto | Rarità | Ruolo |
| --- | --- | --- |
| **Frammento alieno** | comune (garantito) | Futura materia prima per lo **scanner**. |
| **Rottami di tecnologia aliena** | non comune | Futura fabbricazione di potenziamenti. |
| **Nucleo alieno** | raro | Requisito di alto valore per scanner/potenziamenti. |

I tre sono raggruppati nel tag oggetto `nerospace:alien_materials` per ricette future. Lo scanner che li trasforma in potenziamenti è nella **[Roadmap](Roadmap.md)** / nelle **[Funzionalità future](Future-Features.md)**.

## Evocare una meteora (creativa)

L'**Evocatore di meteore** è uno strumento solo per la modalità creativa: fai clic destro su un blocco qualsiasi per far cadere subito una meteora in quel punto, con un bottino appena estratto, seguendo lo stesso percorso della generazione naturale. In sopravvivenza non fa nulla.

## Configurazione

Il ritmo delle meteore è fisso. Non ci sono chiavi di configurazione per le meteore. I valori fissi sono:

- circa una meteora naturale ogni **9.000 secondi** (2,5 ore), vicino a un giocatore attivo;
- un preavviso di **30 secondi** prima della caduta;
- atterra a una distanza tra **200 e 500 blocchi**;
- fino a **4** siti di meteore contemporaneamente per dimensione.

Vedi **[Configurazione](Configuration.md)** per le chiavi che esistono.

## Dettagli

- Attivazione: timer raro vicino ai giocatori (circa una ogni 9.000 secondi; fisso).
- Dimensioni: Overworld, Greenxertz, Cindara, Glacira (non la stazione nel vuoto).
- Gravità: le meteore scendono più lentamente nei mondi a bassa [gravità](Planet-Gravity.md).
- Blocchi: [Roccia di meteora](Meteor-Rock.md), [Nucleo di meteora](Meteor-Core.md) · Entità: `nerospace:falling_meteor`.
- Oggetti: `alien_fragment`, `alien_tech_scrap`, `alien_core`, `meteor_tracker`, `meteor_caller`.
