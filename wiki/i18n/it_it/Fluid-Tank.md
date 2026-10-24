# Serbatoio di fluidi

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Fluid-Tank.md).

> **Spostato in Neroland Core.** Il Serbatoio di fluidi ora è incluso nella libreria condivisa [Neroland Core](Neroland-Core.md) come **`nerolandcore:fluid_tank`**, così ogni mod Neroland usa un unico insieme di blocchi di stoccaggio. Si fabbrica e si usa esattamente come prima: vedi la **wiki di Neroland Core** (pagina *Fluid Tank*) per tutti i dettagli. (Da non confondere con la macchina [Serbatoio di carburante](Fuel-Tank.md) per rifornire i razzi, che resta in Nerospace.)

Un deposito passivo per un singolo fluido: fai clic destro con un secchio per riempirlo o svuotarlo, oppure immetti ed estrai fluidi con i tubi su ogni lato. La variante **Serbatoio di fluidi creativo** fornisce un fluido all'infinito.

Il [Tubo universale](Universal-Pipe.md) di Nerospace vi immette ed estrae ancora carburante per razzi e altri fluidi, quindi il comportamento nel gioco non cambia quando sono installate entrambe le mod.

> **Aggiornare un mondo esistente:** i blocchi piazzati come `nerospace:fluid_tank` vengono rimappati automaticamente in `nerolandcore:fluid_tank` al caricamento (Forge tramite il suo evento per le mappature mancanti, NeoForge e Fabric tramite un alias di registro integrato); oggetti e contenuto memorizzato vengono conservati. Vedi il [registro delle modifiche](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

## Tubi per fluidi di altre mod

**Il serbatoio non è più un punto finale riservato a Nerospace.** Risponde al gestore di fluidi standard della piattaforma su ogni loader, quindi **il tubo per fluidi di un'altra mod può svuotarlo in un contenitore più grande o riempirlo**, senza alcun tubo di Nerospace lungo la linea. Funziona anche al contrario: un [Tubo universale](Universal-Pipe.md) preleva volentieri dal serbatoio di un'altra mod o vi immette, così puoi accumulare qui e spostare il fluido oltre con i tubi che hai già.

Le modalità di I/O per faccia decidono comunque cosa fa un lato: una faccia impostata su **Uscita** resta un'uscita per un tubo esterno così come per uno di Nerospace. Se una data mod si collega dipende dal fatto che usi anch'essa i gestori standard; vedi [Tubo universale](Universal-Pipe.md) per il quadro completo.

Vedi anche: [Neroland Core](Neroland-Core.md), [Tubo universale](Universal-Pipe.md), [Serbatoio di carburante](Fuel-Tank.md), [Blocchi sorgente creativi](Creative-Source-Blocks.md).
