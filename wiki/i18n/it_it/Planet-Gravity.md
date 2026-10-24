# Gravità planetaria

Ogni mondo di Nerospace ha la **propria gravità**. L'Overworld è normale, la **Stazione orbitale** è quasi senza peso e i pianeti stanno nel mezzo: quanto salti in alto, quanto velocemente cadi e quanto lontano vola un oggetto lanciato cambiano tutti a seconda di dove ti trovi.

## La gravità in sintesi

I valori sono relativi alla normale gravità dell'Overworld (`1.0×`). Sono i valori predefiniti attuali e si possono regolare completamente con l'opzione di configurazione [`gravityMultiplier`](Configuration.md).

| Dimensione            | Gravità | Sensazione                                                                                    |
| --------------------- | ------- | --------------------------------------------------------------------------------------------- |
| Overworld             | `1.0×`  | Normale.                                                                      |
| **Stazione orbitale** | `~0.1×` | Quasi senza peso: salti lunghi e fluttuanti.                  |
| **Greenxertz**        | `~0.6×` | Leggera: salti notevolmente più alti, atterraggi più morbidi. |
| **Cindara**           | `~0.8×` | Leggermente bassa: un po' più fluttuante che a casa.          |
| **Glacira**           | `~0.4×` | Low — big, slow bounds across the ice.                                        |

Una gravità più bassa significa un **salto più alto** e una **caduta più lenta**, quindi subisci anche **meno danni da caduta**: un lungo volo su Glacira che nell'Overworld ti ferirebbe è sopravvivibile.

## Cosa influenza

La gravità si applica a **tutto ciò che cade**, non solo a te:

- **Tu e le creature**: movimento, altezza del salto e velocità/danno da caduta scalano tutti. Le creature native saltellano nei loro mondi a bassa gravità proprio come te.
- **Oggetti lasciati cadere, frecce, blocchi in caduta, TNT e sfere di esperienza**: fluttuano e vanno alla deriva nei mondi a bassa gravità e si posano più in fretta in quelli pesanti.
- **Meteore**: una [meteora](Meteor-Events.md) in caduta scende **più lentamente** in un mondo a bassa gravità, allungando il suo arco infuocato.

Poiché la gravità è applicata tramite la statistica di gravità del gioco stesso (per te e le creature), il movimento resta fluido e reattivo: nessun lag né scatti all'indietro quando passi da un mondo all'altro.

## Variazioni per bioma

La gravità di un pianeta è il suo **valore di base**, ma singoli **biomi** possono attrarre più o meno del valore predefinito del loro mondo: il fondo di un cratere potrebbe essere quasi senza peso mentre un bacino denso sembra quasi normale. La gravità dei biomi è basata sui dati (tag di bioma dei pacchetti dati: `gravity_micro`, `gravity_low`, `gravity_high`), quindi i modpack possono regolare o aggiungere zone senza codice.

## La terraformazione ripristina la gravità normale

Il terreno recuperato ritrova il suo peso. Quando un [Terraformatore](Terraformer.md) trasforma un pianeta morto a bassa gravità in terra viva, quel **terreno terraformato torna a una gravità normale, simile a quella terrestre**: così una base completamente terraformata sembra casa anche su una luna che all'inizio era fluttuante. (Un tag di gravità del bioma esplicito, se un modpack lo imposta, ha la precedenza.)

## Configurazione

- **[`gravityMultiplier`](Configuration.md)** (`0.1`–`10`, predefinito `1.0`): una scala globale su _tutta_ la gravità. Impostalo sotto `1` per un gioco ancora più fluttuante ovunque, o sopra `1` per uno più pesante.

## Controllare la gravità nel gioco

Il comando per creativa/op **`/nerospace gravity`** indica la gravità in vigore dove ti trovi: il valore, da dove proviene (bioma, terreno terraformato o valore predefinito del pianeta) e la tua statistica di gravità attuale. Utile per regolare un modpack o segnalare un bug.

## Dettagli

- Priorità delle fonti: **tag di gravità del bioma → terreno terraformato (normale) → valore predefinito del pianeta → normale**, poi scalata da `gravityMultiplier`.
- Influenza: giocatori, creature viventi, oggetti lasciati cadere, frecce, blocchi in caduta, TNT innescata, sfere di esperienza e meteore in caduta.
- Tag di bioma: `nerospace:gravity_micro`, `nerospace:gravity_low`, `nerospace:gravity_high`.
