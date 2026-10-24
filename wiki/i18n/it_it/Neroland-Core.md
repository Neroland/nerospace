# Neroland Core

Nerospace è costruita su **Neroland Core** (id mod `nerolandcore`), la libreria di base condivisa dell'ecosistema di mod Neroland. Core è una **dipendenza obbligatoria**: deve essere installata insieme a Nerospace e si carica per prima. I modpack e i server che usano Nerospace hanno bisogno di entrambi i jar (per la stessa versione di Minecraft e lo stesso loader); il launcher si rifiuterà di avviare Nerospace senza una `nerolandcore` compatibile `1.0`–`2.0`.

Basarsi su Core significa che Nerospace non reinventa più i sistemi condivisi da tutte le mod Neroland. In pratica questo ti offre quattro cose.

Core ora fornisce anche i **blocchi di stoccaggio condivisi**: [Batteria](Battery.md), [Serbatoio di fluidi](Fluid-Tank.md), [Serbatoio di gas](Gas-Tank.md), [Deposito di oggetti](Item-Store.md) e [Cestino](Trash-Can.md) (i primi quattro con le loro varianti [creative](Creative-Source-Blocks.md)) sono passati da Nerospace a Core a partire da `nerolandcore` 1.1.0, così ogni mod Neroland usa un unico insieme di punti di stoccaggio. Si fabbricano e si comportano come prima, e il [Tubo universale](Universal-Pipe.md) di Nerospace si collega ancora a essi (e scarica ancora nel Cestino tramite il ponte di compatibilità).

## Un'unica rete energetica tra le mod

Ogni generatore, batteria, macchina e tubo di Nerospace ora espone la propria energia sulla capability condivisa `nerolandcore:energy` di Core, e i tubi di Nerospace leggono i vicini tramite il lookup di Core. Così un [Tubo universale](Universal-Pipe.md) di Nerospace può portare energia direttamente nella macchina di un'altra mod Neroland, e il cavo di un'altra mod può alimentare una macchina di Nerospace: è un'unica rete continua, non reti separate che si fermano al confine tra mod. L'energia interna di Nerospace funziona esattamente come prima; questo aggiunge solo la portata tra mod.

## Tappe di progressione condivise

Man mano che avanzi, Nerospace apre silenziosamente i **requisiti di progressione** di Core validi per tutto l'ecosistema, così le altre mod Neroland possono reagire al punto in cui ti trovi del viaggio: costruire il primo generatore apre _Energia industriale_, il primo lancio di un razzo apre _Orbita raggiunta_, fondare una stazione con una [Carta della stazione](Station-Charter.md) apre _Prima colonia_, e raggiungere un pianeta lontano o riportare in vita un mondo con la terraformazione apre _Spazio profondo_. È un meccanismo a senso unico: Nerospace guida i requisiti condivisi ma regola comunque il proprio ritmo con la [Guida stellare](Star-Guide.md), che resta il riferimento per la tua progressione in Nerospace. Puoi controllare i requisiti condivisi nel gioco con `/neroland gate list`.

## Materiali delle meteore condivisi

Macinare una **[Roccia di meteora](Meteor-Rock.md)** nel **[Macinatore di Nerosium](Nerosium-Grinder.md)** legge il **Registro dei materiali delle meteore** di Core valido per tutto l'ecosistema (Core `1.2.0`+). Ogni mod Neroland dichiara una volta in Core i propri materiali macinabili (con un livello di rarità, un [requisito di progressione](#shared-progression-milestones) e un eventuale legame con un pianeta) e il macinatore ricava un risultato casuale ponderato da quell'aggregato attivo. Così il gruppo delle meteore cresce automaticamente con le mod Neroland installate, senza modifiche al macinatore.

Nerospace è il primo utilizzatore del registro: contribuisce con i suoi materiali grezzi e i minerali caratteristici di ogni pianeta (quarzo Xertz e Nerosteel grezzo su Greenxertz, Cindrite su Cindara, Glacite su Glacira, più Frammenti alieni / Rottami di tecnologia aliena e l'esotico Nucleo alieno), e fornisce il punto di aggancio di _ricerca del pianeta_ di Core così il risolutore sa su quale pianeta avviene una macinazione (e applica il moltiplicatore di vantaggio planetario). Il registro memorizza **solo metadati degli oggetti, nessun dato dei giocatori**; l'operatore della macinazione viene usato tramite UUID solo per il contesto dei requisiti e non viene mai registrato nei log (POPIA/GDPR). Controlla il gruppo attivo con `/neroland meteor list`.

## Un unico percorso per privacy e cancellazione dei dati

Nerospace registra i propri dati legati ai giocatori nell'hook di cancellazione condiviso di Core, quindi un solo `/neroland data eraseme` (o il `/neroland data erase <uuid>` di un amministratore, o la pulizia per inattività di Core) cancella anche i tuoi dati di Nerospace: la proprietà della tua stazione viene scollegata (la stazione resta come contenuto condiviso del mondo), le rotte delle tue [Piattaforme cargo](Cargo-Pad.md) vengono rimosse e le tue piattaforme e i tuoi voli scollegati (il carico in volo viene comunque consegnato), e lo stato di ossigeno e Guida stellare viene azzerato. In linea con POPIA/GDPR, Nerospace indicizza questi dati solo tramite l'UUID del giocatore e non registra mai la tua identità nei log. Vedi `PRIVACY.md` per il quadro completo.

## Un unico comando di ricaricamento della configurazione e una scheda condivisa

La [configurazione](Configuration.md) di Nerospace è ora gestita da Core: si trova in `config/nerospace.properties` e si ricarica in tempo reale con `/neroland config reload`, e i moltiplicatori di bilanciamento del gioco sono decisi dal server (un client connesso usa i valori del server). La disattivazione della telemetria anonima resta una scelta personale, per client, che un server non può mai scavalcare.

Infine, i materiali caratteristici e gli oggetti chiave di Nerospace compaiono anche nella scheda creativa condivisa **Neroland** di Core, così un pack con diverse mod Neroland mostra una scheda ordinata, mentre il catalogo completo di Nerospace resta nella sua scheda dedicata.

---

Vedi anche: [Configurazione](Configuration.md), [Guida stellare](Star-Guide.md), [Tubo universale](Universal-Pipe.md), [Carta della stazione](Station-Charter.md).
