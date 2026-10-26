# Deposito di oggetti

> **Spostato in Neroland Core.** Il Deposito di oggetti ora è incluso nella libreria condivisa [Neroland Core](Neroland-Core.md) come **`nerolandcore:item_store`**, così ogni mod Neroland usa un unico insieme di blocchi di stoccaggio. Si fabbrica e si usa esattamente come prima: vedi la **wiki di Neroland Core** (pagina _Item Store_) per tutti i dettagli.

Un contenitore da 27 slot con un'interfaccia simile al baule, pensato per l'automazione con i tubi: tramogge e tubi possono inserire ed estrarre su ogni lato, compresa la faccia superiore. La variante **Deposito di oggetti creativo** fornisce un flusso infinito di un oggetto configurato.

Il [Tubo universale](Universal-Pipe.md) di Nerospace lo riempie e lo svuota ancora, quindi il comportamento nel gioco non cambia quando sono installate entrambe le mod.

> **Aggiornare un mondo esistente:** i blocchi piazzati come `nerospace:item_store` vengono rimappati automaticamente in `nerolandcore:item_store` al caricamento (Forge tramite il suo evento per le mappature mancanti, NeoForge e Fabric tramite un alias di registro integrato); oggetti e contenuto memorizzato vengono conservati. Vedi il [registro delle modifiche](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

Vedi anche: [Neroland Core](Neroland-Core.md), [Tubo universale](Universal-Pipe.md), [Blocchi sorgente creativi](Creative-Source-Blocks.md).
