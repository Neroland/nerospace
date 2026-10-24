# Alimentare le macchine

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Powering-Machines.md).

La maggior parte delle macchine di Nerospace ha bisogno di energia (indicata come **FE**). **Non** la producono da sole. Se la barra dell'energia di una macchina resta allo **0%**, niente la sta alimentando.

## In breve

1. Costruisci un **generatore**.
2. Mettilo **subito accanto** alla macchina (a contatto con un lato qualsiasi).
3. Metti del combustibile nel generatore.

Tutto qui. I generatori spingono da soli energia in ogni macchina che toccano. Per le macchine a contatto con il generatore non serve alcun cavo.

## Generatori

| Generatore | Combustibile | Potenza | Quanto dura un oggetto |
| --- | --- | --- | --- |
| [Generatore a combustione](Combustion-Generator.md) | Carbone, carbonella, blocco di carbone, verga di blaze, Tanica di carburante per razzi | 20 FE/tick | Carbone: 80 s · Blocco di carbone: 13 min · Verga di blaze: 2 min · Tanica: 3 min 20 s |
| [Generatore passivo](Passive-Generator.md) | Nerosium grezzo, lingotto di Nerosium o Polvere di Nerosium | 8 FE/tick | 20 minuti |
| [Pannello solare](Solar-Panel.md) | Luce del sole (niente da fornire) | Livello 1: 20 · Livello 2: 100 · Livello 3: 400 FE/tick in pieno sole | Per sempre |

- I generatori **a combustione** e **passivo** richiedono **lingotti di Nerosteel** per essere fabbricati. Il Minerale di Nerosteel si trova in profondità nell'Overworld (sotto circa y 16): vedi [Minerale di Nerosteel](Nerosteel-Ore.md).
- I **pannelli solari** richiedono quarzo Xertz, che ottieni più avanti su Greenxertz.
- Nei mondi senza aria (la stazione e i pianeti) i pannelli solari producono il **doppio** dell'energia e funzionano anche sotto un tetto.

## Quanta energia consumano le macchine?

| Macchina | Consumo | Buona configurazione iniziale |
| --- | --- | --- |
| [Macinatore di Nerosium](Nerosium-Grinder.md) | 20 FE/tick durante la macinazione | 1 Generatore a combustione (piena velocità) o 1 Generatore passivo (circa 40% di velocità) |
| [Generatore di ossigeno](Oxygen-Generator.md) | 80 FE/tick durante la produzione di ossigeno | 4 Generatori a combustione, o il solare |
| [Raffineria di carburante](Fuel-Refinery.md) | 40 FE/tick durante la raffinazione | 2 Generatori a combustione |
| [Controllore della cava](Quarry-Controller.md) | 40 FE per blocco estratto | 1–2 generatori |
| [Terraformatore](Terraformer.md) | 12–48 FE per colonna | Diversi generatori o solare di livello 2/3 |

Una macchina che riceve meno energia di quanta ne consuma funziona comunque: va solo più lenta, a scatti.

## Portare l'energia più lontano

- **[Tubo universale](Universal-Pipe.md)**: trasporta energia, fluidi, gas e oggetti. Collega un generatore a macchine che non lo toccano.
- **[Batteria](Battery.md)** (da Neroland Core): immagazzina energia così le macchine continuano a funzionare quando il generatore finisce il combustibile o il sole tramonta.
- **[Configuratore](Configurator.md)**: fai clic destro sulla faccia di una macchina per scegliere cosa fa quel lato (ingresso energia, ingresso oggetti, uscita oggetti, disattivato).

## Problemi comuni

- **L'energia è allo 0%**: nessun generatore tocca la macchina o è collegato tramite tubo. Oppure il generatore non ha combustibile.
- **Il generatore ha combustibile ma la macchina è ancora allo 0%**: controlla il lato con il [Configuratore](Configurator.md); una faccia impostata su *disattivato* non riceve energia.
- **Il generatore smette di bruciare**: la sua riserva interna è piena. È normale: aspetta che qualcosa usi l'energia.
