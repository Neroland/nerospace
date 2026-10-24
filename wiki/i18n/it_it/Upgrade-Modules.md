# Moduli di potenziamento e Involucro del telaio

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Upgrade-Modules.md).

Le parti di fabbricazione e le schede di regolazione per il [Controllore della cava](Quarry-Controller.md). I moduli sono un sistema **condiviso tra macchine**, progettato perché in futuro altre macchine possano usare le stesse schede.

## Involucro del telaio

Il materiale strutturale che la cava consuma per costruire l'anello del telaio: **un involucro per ogni cella del perimetro** (il terreno sull'anello viene sostituito dal telaio; bauli e macchine sull'anello restano al loro posto). Gli involucri sono anche **piazzabili** come blocchi del telaio, così puoi delimitare a mano un'area di scavo invece di usare i segnaposto; un blocco del telaio rotto da un giocatore lascia cadere il suo involucro, e **uno scavo completato restituisce gli involucri ancora in piedi** agli slot del telaio del controllore.

**Fabbricazione** (con forma, ne produce 4):

```text
I I I
I   I
I I I
```

`I` = [Lingotto di Nerosteel](Items.md)

## Moduli di potenziamento

Inserisci le schede modulo negli slot modulo di una macchina per cambiarne il funzionamento. Una macchina conta i moduli presenti nei suoi slot e ne somma gli effetti (puoi sommarne diversi). Il [Controllore della cava](Quarry-Controller.md) ha **1 slot modulo al livello 1** (di più ai livelli superiori).

| Modulo | Effetto | Note |
| --- | --- | --- |
| **Modulo di velocità** | +50% velocità di lavoro per modulo | Limite ×8. L'energia da sola non rende mai più veloce una macchina: solo i moduli di velocità lo fanno (e non cambiano il costo energetico per blocco). |
| **Modulo di efficienza** | −15% costo energetico per modulo | Non scende sotto il 25% del costo base. |
| **Modulo Fortuna** | Applica Fortuna ai blocchi estratti | Si somma fino a Fortuna III. |
| **Modulo Tocco di velluto** | Estrae i blocchi con Tocco di velluto | **Prevale** su Fortuna quando presente. |
| **Modulo evaporatore** | Distrugge i liquidi invece di accumularli | Mantiene vuota la riserva di fluidi della cava: niente da portare via con i tubi. |

**Fabbricazione**: tutti i moduli condividono lo stesso schema con un oggetto caratteristico al centro:

```text
 N
R S R
 N
```

`N` = [Lingotto di Nerosteel](Items.md) · `R` = Redstone · `S` = oggetto caratteristico:

| Modulo | Oggetto caratteristico `S` |
| --- | --- |
| Velocità | Zucchero |
| Efficienza | Lapislazzuli |
| Fortuna | Diamante |
| Tocco di velluto | Scheggia di ametista |
| Evaporatore | Polvere di blaze |

## Dettagli

- ID: `nerospace:frame_casing`, `nerospace:speed_module`, `nerospace:efficiency_module`,

  `nerospace:fortune_module`, `nerospace:silk_touch_module`, `nerospace:evaporator_module`

- Usati da: [Controllore della cava](Quarry-Controller.md) (altre macchine in programma)
