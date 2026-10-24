# Backup dei dati salvati

Nerospace conserva lo stato del mondo (fonti di ossigeno, avanzamento dei terraformatori, siti delle meteore, i registri delle [rampe di lancio](Rocket-Launch-Pad.md) e delle [stazioni](Station-Charter.md) e il registro delle [rotte cargo](Cargo-Pad.md), cioè piattaforme, rotte e voli in corso) nei file dei dati salvati del mondo, accanto a quelli del gioco base. A partire dalle versioni successive alla 1.0.0, la mod protegge questo stato anche dai problemi del disco.

## Cosa fa

- **File di backup.** Nerospace scrive periodicamente una copia compatta dell'ultimo stato valido di ciascuno dei suoi archivi di dati salvati in `data/nerospace_<name>_backup.dat`, nella cartella della dimensione corrispondente del salvataggio del mondo. I backup vengono riscritti solo quando qualcosa è effettivamente cambiato, e in modo atomico, così un arresto anomalo nel momento sbagliato non può mai corrompere il backup stesso.
- **Ripristino automatico.** Se Minecraft non riesce a leggere uno dei file principali dei dati salvati di Nerospace (disco danneggiato, file troncato dopo un'interruzione di corrente), il gioco non va più in crash. Nerospace ripristina invece l'ultimo backup valido; se anche il backup è illeggibile, ricorre a un archivio nuovo e vuoto così il mondo resta giocabile. In ogni caso, al successivo salvataggio del mondo viene riscritto un file pulito e il problema viene annotato nel log.

## Cosa significa in pratica il ripristino

Campi di ossigeno, raggi di terraformazione e stato delle meteore si ricostruiscono in gran parte da soli in pochi secondi a partire dai blocchi del mondo. I registri delle rampe, delle stazioni e delle rotte cargo sono la parte preziosa: con un backup tornano all'ultimo punto di backup invece di andare persi. Le Piattaforme cargo si registrano di nuovo quando i loro chunk vengono caricati, quindi anche un archivio nuovo si ripara da solo; una piattaforma che ha perso il proprietario in questo modo può essere rivendicata con un clic destro da accovacciato, e un volo che era in corso viene consegnato a partire dal backup.

## Privacy

I backup contengono esattamente gli stessi dati dei file principali e non lasciano mai il salvataggio del mondo. Una [richiesta di cancellazione dei dati](Neroland-Core.md) viene applicata subito anche al backup, non solo al successivo passaggio periodico.
