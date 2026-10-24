# Controllore della cava

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Quarry-Controller.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/quarry_controller.gif" alt="Quarry Controller" width="150" align="right"></p>
<!-- /nerospace:render -->

Un minatore automatico in stile BuildCraft: delimita un'area con i segnaposto e il controllore costruisce un telaio luminoso e scava l'interno, strato dopo strato, fino al substrato roccioso.

## Panoramica

Il Controllore della cava è il cervello del minatore. Delimiti un'area rettangolare con i **[Segnaposto della cava](Quarry-Landmark.md)**, piazzi il controllore accanto, gli dai **materiale per il telaio** ed **energia**, e lui:

1. **Costruisce un telaio**: un anello strutturale luminoso e trasparente attorno al rettangolo acquisito.
2. **Scava** l'**interno** del rettangolo (le colonne sotto l'anello del telaio restano intatte) **strato

   dopo strato**, dall'alto fino al substrato roccioso, come una stampante 3D al contrario: una testa di perforazione percorre il ponte fino a ogni
   blocco, un blocco alla volta.

3. **Accumula ed espelle automaticamente** ciò che scava: gli oggetti estratti in un inventario interno, e i liquidi

   che incontra in un serbatoio di fluidi interno; entrambi vengono spinti verso depositi / tubi adiacenti (comprese le altre
   mod: vedi [Tubo universale](Universal-Pipe.md)). Il liquido che non ha dove mettere viene distrutto invece
   di bloccare lo scavo.

Non distrugge mai un **oggetto** che non può conservare: se la riserva di uscita si riempie o l'energia finisce, si **mette in pausa** (l'interfaccia ti dice perché) e riprende da solo una volta risolto il problema. I liquidi sono l'eccezione voluta: vedi sotto.

## Come ottenerlo

**Fabbricazione** (con forma):

```text
I D I
F R F
I I I
```

`I` = [Lingotto di Nerosteel](Items.md) · `D` = Diamante · `F` = [Involucro del telaio](Upgrade-Modules.md) · `R` = Blocco di redstone

> Oggi la versione fabbricabile è il livello 1. Controllori di livello 2 / 3 (aree più grandi, più slot modulo, accesso ai pianeti ostili) sono in programma: vedi [Livelli e pianeti](#tiers--planets).

## Allestimento

> È la prima volta? La **[Guida all'allestimento della cava](Quarry-Setup-Guide.md)** ti accompagna passo passo, con le quantità di materiali e la risoluzione dei problemi.

1. **Piazza 3 [Segnaposto della cava](Quarry-Landmark.md) a L** allo **stesso livello Y** per definire gli

   angoli del rettangolo. Ogni lato può essere lungo fino a **64 blocchi** (il limite di configurazione `quarryMaxSide`).

2. **Piazza il controllore accanto o in linea con** un segnaposto: analizza lungo gli assi per trovare il

   gruppo, poi **consuma i segnaposto** e parte.

> **Niente segnaposto? Delimitalo tu.** L'Involucro del telaio si può piazzare: costruisci un **rettangolo chiuso di blocchi del telaio** (stesso livello Y, perimetro completo, stessi limiti di dimensione) e piazza il controllore **accanto all'anello**: adotta il contorno come area di scavo e inizia subito, saltando la fase di costruzione del telaio.

3. **Metti l'[Involucro del telaio](Upgrade-Modules.md) nello slot del telaio** (in alto a sinistra nell'interfaccia). Il telaio costa

   **un involucro per ogni cella del perimetro** (la cella del controllore stesso e gli eventuali block entity sul
   perimetro vengono saltati, lasciando un buco invece di sovrascriverli).

4. **Porta l'energia con i tubi** (vedi sotto). Costruire il telaio è gratis, ma **scavare richiede energia**.

## Come funziona

- **Energia:** una riserva interna da **200.000 FE**, riempita tramite la capability energetica su qualsiasi lato:

  collega un [Tubo universale](Universal-Pipe.md) da un [Generatore a combustione](Combustion-Generator.md)/[Generatore
  passivo](Passive-Generator.md) o da una [Batteria](Battery.md). **La velocità di scavo è fissa**: la velocità base
  del livello × il bonus di velocità dei moduli × il fattore di velocità del pianeta (× la configurazione `machineSpeedMultiplier`).
  Più energia **non** lo fa scavare più in fretta; tiene solo piena la riserva, e se
  la riserva si esaurisce lo scavo si **mette in pausa** («energia esaurita») finché l'energia non torna. Il costo base è
  di **40 FE per blocco** (ridotto dai moduli di efficienza): una cava di livello 1 senza moduli scava circa
  4 blocchi al secondo, circa 8 FE/t.

- **Riserva di uscita:** 12 slot interni per gli oggetti estratti; **espelle automaticamente** in un inventario /

  tubo adiacente. Lo scavo si **mette in pausa** («riserva piena») quando un drop non ci sta: niente viene mai distrutto.

- **Riserva di fluidi:** i liquidi sorgente (acqua, lava) nell'area di scavo vengono **aspirati** in un

  serbatoio interno da **16.000 mB** che espelle automaticamente verso un [Serbatoio di fluidi](Fluid-Tank.md) / tubo adiacente;
  vale anche il serbatoio o il tubo di un'altra mod (vedi [Tubo universale](Universal-Pipe.md)). Il serbatoio contiene un
  fluido alla volta, per l'equivalente di 16 blocchi sorgente, e **la cava non lo aspetta mai**: tutto ciò
  che non ci sta (l'eccedenza, e qualsiasi fluido diverso da quello che sta già contenendo) viene distrutto
  durante lo scavo, così uno scavo attraverso un oceano o un lago di lava prosegue comunque. Elimina al massimo **64
  blocchi sorgente per tick di scavo**, quindi una grande massa d'acqua si svuota in modo costante in pochi secondi invece
  di sparire tutta insieme. Non vuoi conservarne affatto? Inserisci un
  **[Modulo evaporatore](Upgrade-Modules.md)**: con uno installato la cava distrugge ogni liquido a vista e
  la riserva resta vuota.

- **Ostacoli:** **salta** il substrato roccioso e gli altri blocchi indistruttibili, e **salta l'intera colonna**

  sotto un tile entity (bauli, generatori di mostri, macchine) così restano intatti. Salta anche i telai di altre
  cave.

- **Solo l'interno:** scava l'**interno** del rettangolo e non scava mai le colonne sotto

  l'anello del telaio, così il telaio mantiene l'appoggio e le pareti della fossa restano pulite.

- **Testa di perforazione + ponte:** mentre lavora, un solido **ponte** 3D scorre sul telaio (una trave a ponte con

  carrelli alle estremità, un carrello che segue la colonna di scavo e un'asta di supporto) e una **punta di perforazione** 3D
  rotante (mandrino, scanalature e una punta conica luminosa) scende con la punta in basso fino al blocco esatto
  che viene scavato.

- **Il telaio** è una vera struttura 3D: montanti d'angolo e binari laterali luminosi con un centro aperto

  e trasparente (così puoi guardare lo scavo attraverso).

- **Bordi lontani:** un'area grande viene caricata forzatamente **un chunk alla volta** durante lo scavo attivo, così

  lo scavo continua mentre ti muovi ai suoi bordi; ogni chunk viene rilasciato appena lo scavo lo supera (e
  quelli rimasti quando finisce o viene rimosso), quindi la cava non blocca mai l'intera regione, mantenendo
  bassi l'uso di memoria e il tempo di salvataggio del mondo.

- **Danni al telaio:** se un blocco del telaio viene rotto durante lo scavo (lascia cadere un Involucro del telaio), la cava

  **ricostruisce il buco** dalla sua scorta di involucri, oppure si **mette in pausa** («telaio incompleto») finché non ripari
  l'anello a mano o inserisci involucri negli slot del telaio.

- **Recupero:** quando lo scavo **finisce**, il telaio viene smontato e gli involucri ancora in piedi vengono

  restituiti agli slot del telaio del controllore. **Rompere il controllore durante lo scavo** invece rende il telaio
  *orfano*: i blocchi del telaio si sgretolano lentamente uno alla volta nei minuti successivi (ogni blocco
  in un suo momento casuale, circa 30 s – 4 min per impostazione predefinita, regolabile con `quarryFrameDecayTicks`),
  e **ognuno lascia cadere il suo Involucro del telaio**, lo stesso drop che otterresti estraendolo tu. Piazzare un
  nuovo controllore accanto a un anello ancora in piedi lo riadotta e ferma il deterioramento.

### Righe di stato dell'interfaccia

| Riga | Significato |
| --- | --- |
| **Inattivo: imposta i segnaposto o il telaio** | Nessuna regione valida trovata: piazza i segnaposto o un contorno di telaio costruito a mano. |
| **Costruzione del telaio** | Sta piazzando l'anello del telaio (consumando involucri). |
| **Scavo in corso** | Sta scavando: `Depth` (sul bordo destro della riga, mostrato durante costruzione/scavo) conta gli strati sotto il piano del telaio. |
| **In pausa: …** | Fermo: la riga indica il motivo: telaio incompleto, Involucri del telaio esauriti, energia esaurita, riserva di uscita piena o pianeta sbagliato (livello troppo basso). I liquidi non mettono mai in pausa lo scavo. La testa di perforazione in pausa resta dove lo scavo si è fermato. |
| **Completato: telaio recuperato** | Ha raggiunto il substrato roccioso; il telaio è stato smontato e i suoi involucri restituiti agli slot del telaio. |

<a id="tiers--planets"></a>

## Livelli e pianeti

Il minatore funziona **ovunque abbia energia**, ma le lune esterne ostili sono vincolate al livello:

| Livello | Area massima | Slot modulo | Velocità base | Pianeti in cui può scavare |
| --- | --- | --- | --- | --- |
| **Livello 1** | 64 × 64 | 1 | 2 blocchi/ciclo | Overworld, Greenxertz, Stazione orbitale |
| Livello 2 *(in programma)* | 64 × 64 | 2 | 4 blocchi/ciclo | + **Cindara** |
| Livello 3 *(in programma)* | 64 × 64 | 4 | 8 blocchi/ciclo | + **Glacira** |

L'area massima è il valore di configurazione `quarryMaxSide` (predefinito e massimo 64; vedi [Configurazione](Configuration.md)).

Anche **velocità/resa dello scavo variano per pianeta** (le dense lune esterne si scavano un po' più lentamente). Un livello troppo basso su un pianeta vincolato si mette in pausa con «pianeta sbagliato».

## Potenziamenti e futuro

- I **[Moduli di potenziamento](Upgrade-Modules.md)** (Velocità / Efficienza / Fortuna / Tocco di velluto / Evaporatore) si inseriscono nel

  controllore e ne regolano il comportamento. Sono un sistema **condiviso tra macchine**: le stesse schede funzioneranno
  anche in altre macchine.

- I **filtri** (tieni ciò che è in lista, distruggi il resto, per esempio per eliminare il pietrisco) sono un seguito in programma; la

  catena di uscita è già pronta per accoglierli.

## Dettagli

- ID: `nerospace:quarry_controller` · Attrezzo: piccone, livello ferro · Drop: se stesso (più gli oggetti in riserva, gli involucri e i moduli)
- Blocchi complementari: [Segnaposto della cava](Quarry-Landmark.md), Telaio della cava (piazzato dalla macchina o

  a mano da un Involucro del telaio; lascia sempre cadere un Involucro del telaio, sia che lo rompa un giocatore sia che si sgretoli
  dopo la distruzione del suo controllore)

- Capability: energia **in ingresso** (qualsiasi lato); Involucro del telaio e moduli di potenziamento **in ingresso** (qualsiasi lato: una

  tramoggia o un tubo possono tenere riforniti gli slot del telaio); **oggetti estratti in uscita** (qualsiasi lato); **fluidi in uscita**
  (qualsiasi lato). Si possono estrarre solo gli slot di uscita, quindi l'automazione non può mai sfilare i tuoi moduli o
  involucri

- Configurazione: scala con i normali `energyRateMultiplier`, `fuelCostMultiplier` e

  `machineSpeedMultiplier`; `quarryFrameDecayTicks` regola la lentezza con cui si sgretola un telaio orfano
  (vedi [Configurazione](Configuration.md))
