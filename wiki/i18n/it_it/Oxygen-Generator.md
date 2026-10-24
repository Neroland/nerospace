# Generatore di ossigeno

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Oxygen-Generator.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/oxygen_generator.gif" alt="Oxygen Generator" width="150" align="right"></p>
<!-- /nerospace:render -->

Ti tiene in vita fuori dalla Terra trasformando l'energia di rete in ossigeno e pressurizzando l'aria intorno a sé.

## Panoramica

Ogni dimensione di Nerospace è **priva d'aria**: i giocatori esposti perdono ossigeno e soffocano. Il Generatore di ossigeno è una **macchina per l'elettrolisi**: consuma energia dalla rete di tubi e produce **ossigeno** in un serbatoio interno. Quel serbatoio alimenta l'area respirabile attorno alla macchina e può anche essere **convogliato** tramite i Tubi universali (livello gas) in Serbatoi di gas o in altre stanze.

## Come ottenerlo

**Fabbricazione** (con forma):

```text
N G N
R C R
N N N
```

`N` = Lingotto di Nerosteel · `G` = Vetro · `R` = Redstone · `C` = Tanica di carburante per razzi

## Come funziona

- **Ingresso di energia:** collega un Tubo universale che trasporta energia (da un Generatore a combustione/passivo o da una

  Batteria). La macchina immagazzina fino a **50.000 FE** e accetta fino a **1.000 FE/t**. Non può bruciare
  combustibile direttamente: funziona solo con la rete.

- **Uscita di ossigeno:** quando è alimentata produce **4 mB di ossigeno per tick** a **20 FE per mB** (80 FE/t

  durante la produzione) in un serbatoio interno da 8.000 mB. I tubi collegati possono portare via il gas (flusso ciano).

- **Area di ossigeno:** finché il serbatoio contiene gas, la macchina è una **sorgente** di ossigeno e si svuota lentamente

  (2 mB/t) per mantenere l'aria. L'aria si diffonde verso l'esterno attraverso lo spazio aperto collegato:

  - **Stanza sigillata** (pareti/tetto: i blocchi interi e il vetro sono ermetici): **l'intera stanza si riempie** e

    resta respirabile.

  - **Spazio aperto / con perdite:** si pressurizza solo una bolla attorno al generatore (l'aria fugge verso qualsiasi

    apertura), quindi sigilla la tua base per una copertura completa. Una porta o un varco contano come perdita.

  - La copertura arriva fino a 16 blocchi d'aria collegata dal generatore. In spazio aperto la bolla ha un raggio di circa 14 blocchi. Questi valori sono fissi.
- **Perdita:** se il serbatoio si esaurisce o la macchina viene rotta, l'ossigeno **evapora in circa 10

  secondi**.

- **Spegnimento con redstone:** un segnale di redstone vicino al generatore lo spegne, e la sua bolla d'aria

  collassa.

- **HUD:** nelle dimensioni prive d'aria compare una **barra O₂** ciano sopra la barra degli oggetti (diventa rossa quando è bassa).
- **Camera stagna:** un giocatore che indossa una [Tuta a ossigeno](Oxygen-Suit.md) completa entro **4 blocchi**

  ricarica l'aria della tuta direttamente dal serbatoio della macchina, consumandone il gas: comodo alla porta di una base
  anche quando la stanza stessa non è ancora respirabile.

- **Automazione:** emette un **segnale per comparatore** in base al livello del serbatoio di ossigeno.

## Consigli

Costruisci una stanza ermetica, porta una linea di energia a un generatore al suo interno e la stanza resterà respirabile finché la rete funziona. Convoglia l'ossigeno in eccesso in un **Serbatoio di gas** come riserva per i blackout. Una **Tuta a ossigeno** completa è l'alternativa portatile per esplorare lontano da un generatore.

## Dettagli

- ID: `nerospace:oxygen_generator` · Attrezzo: piccone, livello ferro · Drop: se stesso
- Interfaccia: indicatore di energia + indicatore del serbatoio di ossigeno con stato In produzione/Nessuna energia
- Raggio della bolla (14), portata delle perdite (16 blocchi), tempo di evaporazione (10 s), soglia di respirabilità (6) e raggio della camera stagna (4) sono fissi, non chiavi di configurazione. Vedi [Configurazione](Configuration.md) per ciò che puoi regolare.
