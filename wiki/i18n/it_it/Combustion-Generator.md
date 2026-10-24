# Generatore a combustione

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Combustion-Generator.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/combustion_generator.gif" alt="Combustion Generator" width="150" align="right"></p>
<!-- /nerospace:render -->

Brucia carburante per produrre energia (FE) per la rete di tubi.

## Come ottenerlo

**Fabbricazione** (con forma): un telaio di Nerosteel attorno a una fornace, collegato con la redstone:

```text
N N N
N F N
N R N
```

`N` = Lingotto di Nerosteel · `F` = Fornace · `R` = Redstone

## Come funziona

- **Slot del combustibile** (riempito tramite interfaccia, a mano o con tramoggia/tubo): carbone, carbonella, blocchi di carbone, verghe di blaze o Taniche

  di carburante per razzi.

- La combustione produce **20 FE/t** in una riserva da **100.000 FE**. Brucia combustibile solo quando la riserva non è

  piena, quindi non se ne spreca.

- Tempi di combustione: carbone o carbonella **1.600 tick**, blocco di carbone **16.000 tick**, verga di blaze **2.400 tick**,

  Tanica di carburante per razzi **4.000 tick**.

- **Spinta automatica:** spinge energia in qualsiasi macchina o Batteria a contatto. Non servono cavi.
- Anche un Tubo universale (livello energia) può prelevare energia. La produzione totale arriva fino a **1.280 FE/t**.

- L'interfaccia mostra la riserva di energia e l'avanzamento della combustione; emette un segnale per comparatore in base alla carica.

## Dettagli

- ID: `nerospace:combustion_generator` · Attrezzo: piccone, livello ferro · Drop: se stesso
- Configurazione: la produzione scala con `energyRateMultiplier`. Vedi [Configurazione](Configuration.md).
