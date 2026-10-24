# Cestino

> **Spostato in Neroland Core.** Il Cestino ora è incluso nella libreria condivisa [Neroland Core](Neroland-Core.md) come **`nerolandcore:trash_can`**, così ogni mod Neroland usa un unico insieme di blocchi di stoccaggio. Si fabbrica e si usa esattamente come prima: vedi la **wiki di Neroland Core** (pagina _Trash Can_) per tutti i dettagli.

Un pozzo senza fondo: oggetti, fluidi o gas immessi con tubi o tramogge vengono distrutti (solo ingresso, nessuna estrazione), tramite un'interfaccia simile al baule con un solo slot di scarico.

Il [Tubo universale](Universal-Pipe.md) di Nerospace vi scarica ancora tramite il ponte di compatibilità, quindi il comportamento nel gioco non cambia quando sono installate entrambe le mod.

> **Aggiornare un mondo esistente:** i blocchi piazzati come `nerospace:trash_can` vengono rimappati in `nerolandcore:trash_can` al caricamento dal meccanismo di alias degli id dei blocchi di Core (Forge `MissingMappingsEvent`; NeoForge/Fabric tramite il mixin di rimappatura del registro), così i blocchi esistenti vengono mantenuti. Vedi il [registro delle modifiche](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

Vedi anche: [Neroland Core](Neroland-Core.md), [Tubo universale](Universal-Pipe.md), [Blocchi sorgente creativi](Creative-Source-Blocks.md).
