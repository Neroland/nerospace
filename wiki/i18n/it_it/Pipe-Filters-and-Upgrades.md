# Filtri per tubi e potenziamenti

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Pipe-Filters-and-Upgrades.md).

Regola con precisione cosa scorre dove, e a che velocità.

## Filtro per tubi

Limita il **livello oggetti** di una faccia del tubo a un solo oggetto.

- **Fabbricazione** (con forma, ne produce 4): angoli di Nerosteel attorno a sbarre di ferro.
- **Impostare il filtro:** tieni il filtro in una mano e l'oggetto da far corrispondere nell'altra, poi

  fai clic destro nel vuoto. (Altra mano vuota = azzera l'oggetto del filtro.)

- **Installare:** fai clic destro sulla **faccia** di un Tubo universale con il filtro configurato: l'oggetto viene **consumato nello slot del filtro di quella faccia** (come i potenziamenti dei tubi), e solo l'oggetto corrispondente viene prelevato o spinto attraverso la faccia. Un filtro già presente sulla faccia ti viene restituito.
- **Rimuovere / ispezionare:** apri il pannello del [Configuratore](Configurator.md) del tubo: il filtro di ogni faccia si trova in un vero slot; passaci sopra il mouse per vedere cosa corrisponde, toglilo per azzerare la faccia. Rompere il tubo fa cadere i filtri installati.

I filtri influenzano l'estrazione (le facce di prelievo prendono solo l'oggetto corrispondente), l'instradamento (i pacchetti non si dirigono verso una faccia filtrata che li rifiuta) e la consegna.

**Regole di instradamento (come interagiscono filtri e destinazioni):**

- Gli oggetti entrano in una rete di tubi **solo attraverso le facce impostate su Ingresso** (con il [Configuratore](Configurator.md)). Una faccia **Auto** consegna ma non estrae mai oggetti, così la rete non può svuotare di nuovo i propri bauli di destinazione. Le facce Auto per energia, fluidi e gas prelevano comunque come prima.
- **Gli incroci sono controllati.** Un oggetto attraversa un incrocio tra tubi solo se la faccia in uscita permette oggetti in uscita (**Uscita**/Auto), la faccia in entrata permette oggetti in entrata (**Ingresso**/Auto) ed **entrambi i filtri delle facce** lo lasciano passare. Un filtro installato sulla faccia di diramazione di un incrocio a T controlla quindi esattamente cosa può imboccare quella diramazione: gli oggetti che non corrispondono la aggirano o restano indietro.
- **Le facce filtrate reclamano per prime i propri oggetti.** Un oggetto che corrisponde a una qualsiasi faccia filtrata va a una faccia filtrata; le destinazioni non filtrate ricevono solo ciò che nessun filtro ha reclamato. Quindi «pietrisco → baule A (lista consentiti)» significa davvero che tutto il pietrisco finisce in A, e il baule di troppo pieno non filtrato riceve tutto il resto.

> **Consiglio: fai attenzione alla faccia su cui fai clic.** Il filtro finisce sulla faccia su cui hai fatto clic, che non è sempre quella a contatto con il baule di destinazione. Tieni in mano il Configuratore per vedere la colorazione per faccia, poi controlla il pannello: il filtro deve trovarsi nella riga il cui colore corrisponde alla faccia colorata rivolta verso la tua destinazione. Gli oggetti **senza alcuna faccia che li accetti** aspettano dentro i tubi (contropressione: niente viene mai lasciato cadere), quindi una linea completamente filtrata ha bisogno di almeno una destinazione non filtrata per gli avanzi.

## Filtro per tubi avanzato

Il filtro di livello superiore: una sola linea di tubi, **molti tipi di oggetti**, con ogni faccia che decide cosa accettare. Uno smistamento in stile tubo di diamante di BuildCraft senza linee parallele.

- **Fabbricazione** (con forma, ne produce 1): un nucleo di Filtro per tubi circondato da lingotti di Nerosteel e quarzo Xertz (materiali di livello Greenxertz).
- **Configurare:** fai clic destro nel vuoto per aprire l'interfaccia del filtro:
  - **9 slot fantasma** (3×3): fai clic con un oggetto sul cursore per impostare una voce, fai clic con il cursore vuoto per azzerarla, Maiusc+clic sugli oggetti direttamente dal tuo inventario.
  - **Clic destro su una voce** (cursore vuoto) per scorrerne la modalità di corrispondenza: l'oggetto esatto → ciascuno dei **tag** dell'oggetto (per esempio `#c:ores` copre ogni minerale con una sola voce, segnata da un `#` dorato) → di nuovo l'oggetto esatto.
  - Interruttore **Lista consentiti / Lista bloccati**: solo questi oggetti, oppure tutto **tranne** questi oggetti.
  - Interruttore **Corrispondenza esatta / Ignora dati**: se devono corrispondere anche i componenti dati dell'oggetto (NBT).
- **Installare:** fai clic destro sulla **faccia** di un Tubo universale: l'oggetto (con tutta la sua configurazione) viene consumato nello slot del filtro di quella faccia; un filtro installato in precedenza ti viene restituito. Rimuovilo o ispezionalo tramite gli slot delle facce del pannello del [Configuratore](Configurator.md).
- **Copiare:** **clic destro da accovacciato** sulla faccia di un tubo per rileggerne il filtro attuale nell'oggetto che hai in mano: replica una parete di smistamento senza ricostruire la configurazione.
- **Ispezionare:** il **tooltip dell'oggetto elenca la configurazione completa** (riga della modalità + ogni voce; le voci di tag in oro), sia nell'inventario sia negli slot delle facce del pannello del Configuratore.

La configurazione è memorizzata sull'oggetto, quindi un filtro regolato è un **modello**: impilalo in un baule, condividilo o applicalo a una dozzina di facce di fila. Un filtro senza voci si comporta come nessun filtro (passa tutto) sia in modalità lista consentiti sia lista bloccati.

**Esempio: la diramazione del macinatore.** Fai partire un tubo dal macinatore; sulla faccia della fornace applica una lista consentiti con le voci di tag dei minerali fondibili, e sulla faccia del baule di bypass applica una **lista bloccati** con le stesse voci. Ciò che si può fondere va alla fornace, tutto il resto prosegue oltre.

I Filtri per tubi base continuano a funzionare esattamente come prima: un filtro avanzato sostituisce semplicemente il filtro base di una faccia quando viene applicato (e viceversa).

## Potenziamento di velocità

- **Fabbricazione** (con forma): redstone + nucleo d'oro in un telaio di Nerosteel.
- **Installare:** fai clic destro su un segmento di tubo (viene consumato, fino a **3** per tubo).
- Ognuno moltiplica la **portata** di energia/fluidi/gas del segmento e fa **viaggiare più velocemente** gli oggetti al suo interno.

## Potenziamento di capacità

- **Fabbricazione** (con forma): quarzo Xertz + nucleo di baule in un telaio di Nerosteel.
- **Installare:** fai clic destro su un segmento di tubo (fino a **3**).
- Ognuno moltiplica le **riserve** di fluidi/gas del segmento e quante pile di oggetti possono essere **in transito** contemporaneamente.

## Rimuovere i potenziamenti

Clic destro da accovacciato sul tubo a **mani vuote**: tutti i potenziamenti installati vengono restituiti.
