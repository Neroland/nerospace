# Raffineria di carburante

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Fuel-Refinery.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/fuel_refinery.gif" alt="Fuel Refinery" width="150" align="right"></p>
<!-- /nerospace:render -->

Raffina carbone + polvere di blaze + energia di rete in carburante liquido per razzi trasportabile con i tubi.

## Panoramica

La Raffineria di carburante è il modo **da logistica** per produrre carburante per razzi. Invece di fabbricare a mano le taniche, le dai **carbone** grezzo (o carbonella), **polvere di blaze** ed **energia di rete**, e lei produce **Carburante per razzi** liquido in un serbatoio interno da cui i [Tubi universali](Universal-Pipe.md) possono prelevarlo direttamente verso un [Serbatoio di carburante](Fuel-Tank.md) o un razzo sulla rampa. Si può fabbricare prima del tuo primo lancio.

## Come ottenerla

**Fabbricazione** (con forma):

```text
N G N
N F N
N R N
```

`N` = Lingotto di Nerosteel · `G` = Vetro · `F` = Fornace · `R` = Redstone

(Gli ingredienti sono basati sui tag, quindi funzionano anche vetro e redstone equivalenti di altre mod.)

## Come funziona

- **Ingressi:** uno slot del **carbonio** (carbone o carbonella) e uno slot del **catalizzatore** (polvere di blaze). Tramogge e

  tubi li riempiono tramite la capability per gli oggetti: il carbone va automaticamente al carbonio, il blaze al catalizzatore.

- **Energia:** solo energia di rete. Contiene **40.000 FE** e accetta fino a 1.000 FE/tick. Alimentala da un generatore o da una Batteria tramite

  [Tubi universali](Universal-Pipe.md).

- **Raffinazione:** un lotto consuma **1 carbone + 1 polvere di blaze + ~4.000 FE** in ~100 tick e produce

  **2.000 mB** di carburante per razzi: lo stesso carburante di due taniche fabbricate a mano, senza ferro e senza
  tutti quei clic.

- **Uscita:** il serbatoio interno da **8.000 mB** espone la capability per i fluidi, così i tubi portano via il carburante.
- **Spinta automatica:** spinge da sola fino a **200 mB/t** di carburante in qualsiasi serbatoio o tubo a contatto.
- **Comparatore:** emette un segnale di redstone proporzionale al livello di carburante.

`fuelCostMultiplier` scala il consumo di energia (FE/t) e `machineSpeedMultiplier` scala la durata del ciclo. `energyRateMultiplier` non si applica. Vedi [Configurazione](Configuration.md).

## Dettagli

- ID: `nerospace:fuel_refinery` · Attrezzo: piccone, livello ferro · Drop: se stessa
