# Roadmap

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Roadmap.md).

Una panoramica pubblica e di alto livello su dove si trova Nerospace e dove sta andando. **La 1.0.0 è la prima versione completa**: contiene l'intera progressione dal primo minerale di Nerosium a un pianeta terraformato. (Le note di rilascio si trovano nel `CHANGELOG.md` del repository.)

## ✅ Incluso nella 1.0.0

**Materiali e macchine**

- La catena del materiale Nerosium (minerale → grezzo → lingotto → polvere) e il **Macinatore di Nerosium** (resa dei minerali doppia).
- Materiali planetari: **Nerosteel** + **quarzo Xertz** (Greenxertz), **Cindrite** (Cindara),

  **Glacite** (Glacira).

- La **Raffineria di carburante** (carbone + polvere di blaze + energia → carburante per razzi trasportabile con i tubi), blocchi di stoccaggio,

  blocchi della stazione, interfacce delle macchine a tema con indicatori animati + uscita per comparatore.

- Tag comuni `c:` e ricette basate sui tag ovunque, automazione tramite capability su ogni faccia delle macchine.

**Viaggi spaziali**

- **Razzi di livello 1–4** con modelli su misura per ogni livello e un'interfaccia interattiva nel razzo.
- La **rampa di lancio 3×3** e il **Complesso di lancio pesante** (5×5 + Torre di lancio) con rapporti

  sulla struttura; il livello 3 accetta l'anello di Parete della stazione **oppure** il complesso pesante, il livello 4 richiede il
  complesso pesante.

- **Serbatoi di carburante** con rifornimento automatico (fino a 480 mB/t) e alimentazione di carburante automatica attraverso i blocchi della rampa.
- Destinazioni: **Stazione orbitale**, **Greenxertz**, **Cindara**, **Glacira**, più le

  **stazioni fondate dai giocatori** tramite la Carta della stazione (fino a 64 per mondo).

**Sopravvivenza e atmosfera**

- Dimensioni prive d'aria con un'**area di ossigeno** per blocco (stanze sigillate, perdite, porte/vetro come

  confini), il **Generatore di ossigeno** a elettrolisi alimentato dalla rete, ricariche in camera stagna e un HUD O₂.

- La **Tuta a ossigeno** in due livelli più le varianti anti-pericolo **termica** e **criogenica** (calore di Cindara /

  freddo di Glacira = consumo ×4 senza protezione).

**Terraformazione e vita**

- Il **Terraformatore** con maturazione a fasi (**Radicato → Idratato → Vivo**), un ciclo dell'acqua

  alimentato a Glacite (Modulo di idratazione), biomi planetari maturi con meteo reale e il Monitor di terraformazione.

- Otto **creature** su misura: cinque native + tre specie di bestiame allevabile sul terreno Vivo.

**Progressione e strumenti**

- L'albero di progressione interattivo della **Guida stellare** (7 capitoli / 31 passi) basato su un albero

  dei progressi completo; la vetrina creativa `/nerospace gallery`; una suite gametest con oltre 36 test.

**Integrazione con JEI ed EMI**

- Con JEI installato, il macinatore, la raffineria di carburante e il generatore a combustione mostrano le proprie categorie di ricette (le ricette e i tag standard funzionavano già da subito).
- EMI ottiene le stesse tre pagine da un plugin EMI nativo. L'EMI ufficiale non ha una versione per Minecraft 26.x, quindi si punta al port della community **EMI Unofficial Port (Unstable)** su NeoForge e Fabric (26.1.2, 26.2, 26.3); quel port non ha una build per Forge. Con JEI ed EMI entrambi installati, le pagine compaiono una sola volta.

**Pannelli solari**

- **[Pannelli solari](Solar-Panel.md)**: generazione di energia che segue il sole e si raggruppa in schiere, su **tre livelli**

  (livello 1 1×1, livello 2 2×2, livello 3 multiblocchi 3×3), ognuno dei quali include il pannello precedente nella ricetta.

## 🛠️ Prossimi passi (primi aggiornamenti dopo la 1.0)

- **Regolazione del bilanciamento in base ai commenti dei giocatori**: i moltiplicatori di configurazione lo rendono facile.
- **Audio su misura** per sostituire i segnaposto che riutilizzano l'audio del gioco base.

## 🔭 In programma / in esplorazione

Vedi **[Funzionalità future](Future-Features.md)**.

## ⏳ Rimandato

- **L'integrazione su misura con mod specifiche** aspetta che quelle mod vengano portate su Minecraft 26.1. Nerospace è

  indipendente per scelta, e la metà generica è già fatta: energia, fluidi e oggetti viaggiano sulle
  capability e sui lookup standard della piattaforma su tutti e tre i loader, quindi qualsiasi altra mod che li usa si collega
  già oggi: vedi [Compatibilità con le mod](Mod-Compatibility.md).

## Contribuire / commenti

Idee e segnalazioni di bug sono benvenute: apri una [issue su GitHub](https://github.com/Neroland/nerospace/issues) o unisciti al [Discord](https://discord.gg/ArPXvYUzJG).
