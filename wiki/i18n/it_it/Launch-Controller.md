# Controllore di lancio

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Launch-Controller.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/launch_controller.png" alt="Launch Controller" width="150" align="right"></p>
<!-- /nerospace:render -->

Una console di controllo missione che costruisce la tua rampa di lancio e fa volare il tuo razzo.

## Panoramica

Il Controllore di lancio è una console solida **larga 3 × alta 2**: un telaio color canna di fucile scuro con un pannello animato e luminoso a «cervello elettronico» (uno schermo attivo, bracci proiettori e una fascia luminosa pulsante). Da un solo blocco svolge due compiti: **costruire** una rampa di lancio con i materiali caricati e **rifornire + lanciare** un razzo parcheggiato su quella rampa. Piazzalo rivolto verso un'area libera; costruisce e supervisiona la rampa che ha davanti.

## Come ottenerlo

**Fabbricazione** (con forma):

```text
W R W
P C P
W R W
```

`W` = [Parete della stazione](Station-Wall.md) · `P` = [Rampa di lancio per razzi](Rocket-Launch-Pad.md) ·
`R` = Redstone · `C` = Comparatore

Piazzarlo forma l'intera struttura 3×2 (serve lo spazio: se è ostruito il posizionamento viene annullato). Rompere una qualsiasi parte rimuove l'intera console e lascia cadere un controllore.

## Come funziona

L'interfaccia ha due modalità (si cambiano in alto a destra):

**Modalità costruzione**

- Carica i blocchi da costruzione nei tre slot: **Rampa di lancio**, **Parete della stazione**, **Torre di lancio**.
- Scegli un **livello (T1–T4)** di destinazione. L'ingombro cresce con il livello: T1 una sola rampa, T2 un 3×3, T3 un 3×3 circondato dalla Parete della stazione, T4 un Complesso di lancio pesante con una torre.
- **Attiva ologramma** proietta un fantasma dei *blocchi effettivi* ancora da piazzare, emesso dai bracci della console.
- **Costruisci rampa** posa la struttura nel mondo **in modo additivo**, promuovendo una rampa esistente di livello inferiore a un livello superiore senza demolirla. Piazza solo i blocchi che hai caricato.
- Una lettura in tempo reale mostra quante Rampe / Pareti / Torri servono ancora, più i livelli di **Carburante / O₂ / Energia** a bordo del controllore.

**Modalità lancio**

- Il controllore ha a bordo riserve di **carburante**, **ossigeno** ed **energia**: riempile con i [Tubi universali](Universal-Pipe.md) (livelli fluidi / gas / energia). Le pompa in un razzo attraccato sulla rampa.
- Leggi carburante/ossigeno/energia del razzo attraccato, scegli la destinazione e premi **Lancia** direttamente dalla console (sali a bordo al decollo).

## Dettagli

- ID: `nerospace:launch_controller`
- Un multiblocco 3×2; i cubi di riempimento sono solidi (non puoi attraversare la console).
- Le capability (fluidi / gas / energia) sono esposte sul blocco centrale per l'automazione con i tubi.
