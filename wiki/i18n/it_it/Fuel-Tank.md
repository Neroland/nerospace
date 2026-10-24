# Serbatoio di carburante

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Fuel-Tank.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/fuel_tank.png" alt="Fuel Tank" width="150" align="right"></p>
<!-- /nerospace:render -->

Conserva carburante per razzi e rifornisce automaticamente un razzo posato su una rampa di lancio adiacente.

## Panoramica

Il Serbatoio di carburante è il primo tassello dell'automazione della rampa: contiene una grande riserva di **Carburante per razzi** e, a ogni tick, lo pompa in un razzo posato su una **[Rampa di lancio per razzi](Rocket-Launch-Pad.md)** collegata, così non devi riempire a mano i razzi con le taniche.

## Come ottenerlo

**Fabbricazione** (con forma):

```text
N G N
G C G
N G N
```

`N` = Lingotto di Nerosteel · `G` = Vetro · `C` = Tanica di carburante per razzi

## Come funziona

- **Capacità:** 32.000 mB di carburante per razzi.
- **Riempimento:** fai clic destro con un **Secchio di carburante per razzi** o una **Tanica** per aggiungere carburante (ti viene restituito il secchio vuoto);

  un tubo per fluidi può riempirlo tramite la capability per i fluidi. Un clic destro a mani vuote apre la
  **schermata del Serbatoio di carburante**.

- **Svuotamento:** fai clic destro con un **secchio vuoto** per prelevare un **Secchio di carburante per razzi**.

- **Alimentazione automatica con taniche:** tramogge e [Tubi universali](Universal-Pipe.md) possono inserire **Taniche di carburante

  per razzi** tramite la capability per gli oggetti del serbatoio: ognuna viene convertita in **1.000 mB** di carburante al tick successivo.
  Abbinalo a una [Raffineria di carburante](Fuel-Refinery.md) (o a una linea di taniche) per un rifornimento completamente automatico.

- **Rifornimento automatico:** quando un razzo si trova su una rampa adiacente, il serbatoio vi pompa carburante. Una

  **rampa di lancio 3×3** completa pompa **4 volte più veloce** (160 contro 40 mB/tick). Su un
  **[Complesso di lancio pesante](Launch-Gantry.md)** (5×5 + torre) la velocità sale a **480 mB/t** (12×),
  riempiendo il serbatoio da 24.000 mB di un livello 4 in meno di un minuto.

- **Automazione completa:** anche la rampa stessa espone lo **slot di immissione del carburante** del razzo come capability

  per gli oggetti, così tramogge e [Tubi universali](Universal-Pipe.md) possono consegnare Secchi e Taniche di carburante per razzi direttamente nel razzo: un Serbatoio di carburante + una linea di oggetti rendono la preparazione al lancio completamente automatica.

- **Comparatore:** emette un segnale di redstone proporzionale al livello di riempimento.

## Dettagli

- ID: `nerospace:fuel_tank` · Attrezzo: piccone, livello ferro · Drop: se stesso
