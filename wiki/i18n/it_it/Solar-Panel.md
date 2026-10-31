# Pannello solare

<!-- nerospace:render -->

<p align="right"><img src="images/solar_panel.png" alt="Solar Panel" width="150" align="right"></p>
<!-- /nerospace:render -->

Generatori che seguono il sole e si uniscono ai vicini in un'unica grande schiera solare. Disponibili in **tre livelli**: il livello 1 è un singolo blocco; i livelli 2 e 3 sono **multiblocchi** 2×2 e 3×3 che piazzi da un solo oggetto.

## Schiere

**Livello 1** (con forma): un piano di vetro sopra celle di quarzo/rame su un alloggiamento di Nerosteel:

```text
G G G
Q C Q
N N N
```

`G` = Vetro · `Q` = Quarzo Xertz · `C` = Lingotto di rame · `N` = Lingotto di Nerosteel

**Livello 2**: quattro pannelli di livello 1 attorno a un Blocco di Nerosium:

```text
. P .
P N P
. P .
```

`P` = Pannello solare di livello 1 · `N` = Blocco di Nerosium

**Livello 3**: quattro pannelli di livello 2 attorno a un Blocco d'oro:

```text
. Q .
Q G Q
. Q .
```

`Q` = Pannello solare di livello 2 · `G` = Blocco d'oro

Ogni livello include il **pannello precedente** nella ricetta, quindi il potenziamento riutilizza ciò che hai già costruito.

## Come funziona

- **Entra luce solare, esce FE.** La produzione segue il sole: massima a mezzogiorno, calando fino a **zero di notte**.

  Nelle dimensioni normali il pannello ha bisogno di una **vista libera sul cielo**: qualsiasi blocco solido direttamente sopra
  ferma la generazione.

- **Meteo:** non c'è una penalità fissa per pioggia o temporale. La produzione segue quanto è scuro il cielo:

  produzione × (10 − oscurità del cielo) / 10. Pioggia e tempeste scuriscono il cielo, quindi la produzione cala di conseguenza.

- **Nerospace planets** (Greenxertz, Cindara, Glacira, Luna, Mars and Nyxara) follow the planet's own day: a panel

  peaks at that planet's noon, dips at night, on Cindara dims during the eclipse and on Mars falls during a dust storm.
  Nyxara's sun never moves, so a panel there gives a steady ×2.04. Over a full planet day
  every planet gives **at least the old ×2 rate on average** (the peaks are higher to make up for the
  nights), so bases that run at night need a battery. The **Orbital Station** (and founded stations) stays
  a constant ×2. Panels do **not** need a view of the sky on a planet. Server owners can restore the old
  constant ×2 with `solar.clockDriven=false`. Il solare è la fonte di energia naturale fuori dalla Terra.

- Produzione e accumulo scalano con il livello (tutti i valori × la configurazione `energyRateMultiplier`). La riserva

  non accetta mai spinte dall'esterno.

- **Spinta automatica:** i pannelli spingono energia in qualsiasi macchina o Batteria a contatto.

| Tier          | Footprint | Output @ noon | Storage                                      |
| ------------- | --------- | -------------------------- | -------------------------------------------- |
| **Livello 1** | 1×1       | 20 FE/t                    | 50.000 FE                    |
| **Livello 2** | 2×2       | 100 FE/t                   | 250.000 FE                   |
| **Livello 3** | 3×3       | 400 FE/t                   | 1.000.000 FE |

Ogni livello superiore rende di più **per superficie** rispetto ad affiancare pannelli del livello inferiore, e ha una riserva molto più grande.

### Multiblocchi (livelli 2 e 3)

- Un pannello di livello 2 / livello 3 è un **singolo oggetto che, una volta piazzato, occupa tutto il suo ingombro N×N**: ti serve

  quell'area piana libera (non si piazza in un ingombro ostruito).

- Si comporta come **un'unica unità**: un grande piano inclinabile, una riserva condivisa, energia su ogni faccia esterna.

- **Rompere una qualsiasi cella restituisce l'intero pannello come un solo oggetto**: nessuna duplicazione, e riottieni il tuo pannello

  back wherever you mined it.

### Arrays

- Piazza unità dello stesso livello **una accanto all'altra** e si uniscono automaticamente in un'unica **schiera**: l'accumulo della

  schiera è la **somma delle riserve di ogni unità** e la sua generazione la **somma della produzione di ogni unità**. Costruisci più largo per avere di più di entrambe: le schiere possono avere quasi qualsiasi dimensione.

- **Ogni lato è una porta di uscita.** Un Tubo universale (livello energia), o qualsiasi macchina/Batteria, che tocca

  _qualsiasi_ faccia preleva dalla riserva condivisa della schiera, così un solo tubo svuota l'intera schiera.

- **I livelli non si mescolano mai.** Una schiera di livello 1 e una di livello 2 affiancate restano separate; si uniscono solo unità

  of the _same_ tier pool together.

### Aspetto

I livelli sono **visibilmente diversi**: il livello 1 ha un accento verde e ruota su un **palo a T**, seguendo il sole da est a ovest; il livello 2 (magenta Nerosium) e il livello 3 (oro) sono schiere piatte N×N i cui piani **si inclinano verso il cielo di giorno e si ripiegano sui loro alloggiamenti di notte**. Tutti i pannelli leggono la stessa ora del mondo, quindi un campo di pannelli si muove all'unisono.

## Dettagli

- ID: `nerospace:solar_panel` (livello 1) / `nerospace:solar_panel_t2` / `nerospace:solar_panel_t3`
- Attrezzo: il livello 1 richiede un piccone di ferro; i livelli 2 e 3 qualsiasi piccone.
- Il livello 1 lascia cadere se stesso; i livelli 2/3 restituiscono un oggetto per l'intero multiblocco quando una qualsiasi cella viene rotta.
- Emette un segnale per comparatore in base alla carica della schiera.
- Configurazione: `energyRateMultiplier` (scala sia la produzione sia l'accumulo). Vedi **[Configurazione](Configuration.md)**.
