# Generatore passivo

<!-- nerospace:render -->

<p align="right"><img src="images/passive_generator.gif" alt="Passive Generator" width="150" align="right"></p>
<!-- /nerospace:render -->

Un flusso di energia lento e senza pensieri, ricavato da un nucleo di Nerosium.

## Obtaining

**Fabbricazione** (con forma): un telaio di Nerosteel attorno a un Blocco di Nerosium, collegato con la redstone:

```text
N N N
N B N
N R N
```

`N` = Lingotto di Nerosteel · `B` = Blocco di Nerosium · `R` = Redstone

## Come funziona

- **Slot del nucleo** (riempito tramite interfaccia, a mano o con tramoggia/tubo): Nerosium grezzo, un lingotto di Nerosium o polvere di Nerosium.
- Un Nerosium grezzo, un lingotto di Nerosium o una Polvere di Nerosium dura **24.000 tick (20 minuti)**, producendo **8 FE/t** in una riserva da **100.000 FE**.
- **Spinta automatica:** spinge energia in qualsiasi macchina o Batteria a contatto. Anche un tubo può prelevarla.
- Più debole del Generatore a combustione, ma ideale come energia di base da impostare e dimenticare.
- Un [Macinatore di Nerosium](Nerosium-Grinder.md) consuma 20 FE/t, quindi un Generatore passivo fa funzionare un macinatore a circa il 40% della velocità. Per iniziare va bene.

## Dettagli

- ID: `nerospace:passive_generator` · Attrezzo: piccone, livello ferro · Drop: se stesso
- Configurazione: la produzione scala con `energyRateMultiplier`. Vedi [Configurazione](Configuration.md).
