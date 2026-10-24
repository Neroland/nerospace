# Strutture aliene

Gli insediamenti e le rovine dei pianeti alieni: generati proceduralmente con il set di decorazioni aliene e distanziati in modo da non affollare mai il mondo.

## Panoramica

In superficie si generano tre strutture, tutte costruite con i **[blocchi decorativi alieni](Alien-Decoration.md)** dall'aspetto futuristico: podi di piastrelle, mura di mattoni con **fasce di finestre di cristallo luminose**, **pilastri d'angolo illuminati** più alti, **tetti affusolati** e **guglie di cristallo**. Ognuna contiene un **[Nucleo del villaggio](Village-Core.md)**.

## Hamlet

Un piccolo avamposto alieno: la scoperta più comune e il tuo «villaggio iniziale».

- Una piazza di piastrelle luminose con un bordo di lampade.
- Due torri futuristiche.
- Un **[Nucleo del villaggio](Village-Core.md)** centrale su un podio di cristallo illuminato.

## Rovina antica

Una grande sala aliena consumata e semisepolta: non comune.

- Costruita con **mattoni alieni crepati**, con muri crollati e tetto rotto (sprofondata nel terreno).
- Un nucleo di cristallo spento al centro.
- Una **camera del tesoro** (baule) di rari beni alieni: Nucleo alieno, Rottami di tecnologia aliena, Frammenti alieni, lingotti di

  Nerosium e smeraldi.

## Mega-City

L'enorme insediamento alieno di fine gioco: rarissimo, e una vera spedizione.

- Una **cinta muraria merlata** (41×41) con pilastri e illuminazione e quattro porte.
- Una piazza di piastrelle con torri futuristiche attorno a una **fortezza centrale**.
- La fortezza custodisce una **grande camera blindata** (Nuclei alieni, **entrambi** i pezzi dell'**[equipaggiamento dell'Artefice](Alien-Gear.md)**,

  diamanti e smeraldi) sorvegliata dal boss **[Guardiano delle rovine](Creatures.md#ruin-warden)**.

## Distanza e densità

Per evitare che le strutture si raggruppino (e per limitare quante ne compaiono in un'area), tutte e tre condividono una **griglia a regioni** deterministica:

- Il mondo è diviso in **celle di 16×16 chunk** (≈256 blocchi).

- Ogni cella ospita **al massimo una** struttura, scelta con un hash ponderato: ~26% Borgo, ~8% Rovina, ~2%

  Mega-City, and the rest **empty** for breathing room.

- La struttura si trova in un chunk di ancoraggio deterministico all'interno della cella, lontano dal bordo così l'ingombro

  non scavalca i confini.

Il risultato è una distanza uniforme e un limite rigido di densità, indipendentemente dalla casualità del posizionamento.

## Dettagli

- Elementi di generazione del mondo personalizzati nel bioma `greenxertz` (fase delle strutture di superficie).
- ID: `nerospace:hamlet`, `nerospace:ruin`, `nerospace:mega_city`.
