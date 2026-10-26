# Terraformatore

<!-- nerospace:render -->

<p align="right"><img src="images/terraformer.gif" alt="Terraformer" width="150" align="right"></p>
<!-- /nerospace:render -->

La macchina di fine gioco: trasforma lentamente un pianeta morto in terra abitabile, respirabile e **viva**, in tre fasi visibili.

## Panoramica

Piazza un Terraformatore su un mondo spoglio, alimentalo, e farà avanzare una **frontiera circolare in continua espansione** a partire da sé. Con la terraformazione avanzata la macchina gestisce **tre frontiere**, ognuna dietro la precedente, così un mondo che funziona da tempo è sempre un gradiente: chimica grezza al bordo, un ecosistema vivo al centro:

| Fase | Nome         | Cosa fa                                                                                                                                                                                                         |
| ---- | ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | **Radicato** | erba + terra, respirabile in modo permanente, un vivace **bioma terraformato** al neon, piante sparse                                                                                                           |
| 2    | **Idratato** | i bacini sotto la falda della macchina si riempiono di vera acqua: costa **Glacite** tramite un [Modulo di idratazione](Hydration-Module.md)                                                    |
| 3    | **Vivo**     | il bioma si assesta in una **tavolozza naturale per pianeta** (prato / savana / tundra) con alberi cresciuti, **pioggia o neve** e mandrie iniziali del [bestiame](Creatures.md) del pianeta |

## Obtaining

**Fabbricazione** (con forma):

```text
N D N
D O D
N B N
```

`N` = Lingotto di Nerosteel · `D` = Terra · `O` = [Generatore di ossigeno](Oxygen-Generator.md) · `B` = Blocco di Nerosteel

## Come funziona

- **Energia di rete:** funziona esclusivamente con energia convogliata: collega un Tubo universale che trasporta FE. La sua

  riserva da 100.000 FE si consuma mentre lavora e accetta fino a **2.000 FE/t** (livelli più alti / più energia =
  più veloce). Converte al massimo **48 colonne per tick** (un limite fisso per proteggere la velocità del server).

- **Frontiere in espansione:** a ogni ciclo di lavoro converte un anello di colonne di superficie per fase, poi aumenta

  il raggio di quella fase, **senza limite**, ma regolato dall'energia. La fase 1 ha la priorità (il terreno respirabile
  non aspetta mai); le fasi successive ricevono quote minori e restano sempre all'interno della fase precedente.
  Le colonne della fase 2 costano **2×** energia più **1 unità di idratazione per sorgente d'acqua**; quelle della fase 3 costano
  **4×** energia.

- **Acqua (Idratato):** la falda si trova un blocco sotto la base della macchina. I bacini sotto di essa si riempiono

  a filo del terreno; le colline restano asciutte; i baratri più profondi del limite vengono saltati. Niente Glacite nella
  riserva = la fase 2 si blocca (l'interfaccia dice «Serve Glacite»). Su Glacira i laghi si ricongelano naturalmente.

- **Vita (Vivo):** il bioma maturo attiva un vero **meteo** (pioggia; su Glacira la neve si accumula),

  **alberi cresciuti** sparsi (quercia/betulla, acacia, abete rosso a seconda del pianeta) e fa comparire coppie iniziali del
  bestiame del pianeta (con un limite di popolazione).

- **Gravità:** il terreno terraformato torna a una **[gravità](Planet-Gravity.md) normale, simile a quella terrestre**: così

  recuperare una luna a bassa gravità fa sembrare casa la terra che hai convertito (un tag di gravità del bioma
  esplicito, se un modpack lo imposta, ha comunque la precedenza).

- **Deriva estetica:** la terra assestata continua a far spuntare da sola una vegetazione rada: pura decorazione,

  con un piccolo budget fisso.

- **Livelli:** metti un potenziamento nello slot di potenziamento: un **lingotto di Nerosteel → livello 2**, una \*\*Cindrite →

  livello 3\*\*. I livelli più alti convertono più colonne per ciclo; il **livello 3** semina anche, con bassa frequenza, minerali nel
  sottosuolo convertito (minerali di Nerospace).

- **Pigro / recupero:** le colonne nei chunk non caricati vengono saltate e convertite più tardi quando esplori

  quelle zone: tutte le fasi vengono riprodotte al caricamento del chunk, così un pianeta terraformato si completa mentre lo percorri. Per impostazione predefinita
  non forza il caricamento dei chunk. Imposta `terraformerForceLoadEnabled = true` per tenere caricata una piccola finestra di
  chunk mentre funziona (costa memoria e velocità del server).

- **Mondi vecchi:** i Terraformatori esistenti continuano a funzionare senza modifiche: la loro terra conta come fase 1 e le

  nuove fasi la ricoprono semplicemente una volta costruiti i nuovi blocchi. Nessuna migrazione.

## Dettagli

- ID: `nerospace:terraformer` · Attrezzo: piccone, livello ferro · Drop: se stesso

- Complementi: [Modulo di idratazione](Hydration-Module.md) (ingresso di Glacite, deve toccarlo),

  [Monitor di terraformazione](Terraform-Monitor.md) (lettura della fase + comparatore)

- Configurazione: solo `terraformerForceLoadEnabled` (predefinito `false`). Anche velocità ed energia seguono i

  moltiplicatori globali. Vedi [Configurazione](Configuration.md).
