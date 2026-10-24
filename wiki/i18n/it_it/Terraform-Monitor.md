# Monitor di terraformazione

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Terraform-Monitor.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/terraform_monitor.gif" alt="Terraform Monitor" width="150" align="right"></p>
<!-- /nerospace:render -->

Il blocco di lettura per la terraformazione avanzata: mostra i raggi delle fasi del [Terraformatore](Terraformer.md) più vicino, la sua riserva di idratazione e il motivo di un eventuale blocco, e segnala tramite comparatore la **fase di terraformazione del terreno locale**.

## Come ottenerlo

**Fabbricazione** (con forma):

```text
N G N
Q R Q
N N N
```

`N` = Lingotto di Nerosteel · `G` = Vetro · `Q` = Quarzo Xertz · `R` = Redstone

## Come funziona

- **Piazzalo ovunque** su un terreno terraformato o nelle vicinanze. Si collega automaticamente al Terraformatore più vicino entro

  **32 blocchi** (senza cavi).

- **Lettura nell'interfaccia:** la fase della colonna locale (Morto / Radicato / Idratato / Vivo), i tre raggi delle fasi della macchina

  collegata, le sue unità di idratazione e un avviso rosso «Serve Glacite» quando la fase dell'acqua si è bloccata.

- **Uscita del comparatore = fase locale:** 0 (morto), 5 (Radicato), 10 (Idratato), 15 (Vivo). Apri i

  cancelli del ranch quando il terreno diventa Vivo, o fai scattare un allarme quando la terraformazione raggiunge la tua base.

## Dettagli

- ID: `nerospace:terraform_monitor` · Attrezzo: piccone, livello ferro · Drop: se stesso
