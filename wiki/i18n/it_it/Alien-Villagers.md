# Villici alieni

Gli alieni sociali dei pianeti di Nerospace: viandanti diffidenti da conquistare per sbloccare scambi e far crescere un villaggio.

## Panoramica

Il **Villico alieno** (`nerospace:alien_villager`) è un PNG pacifico e **diffidente ma neutrale**: passeggia nel suo bioma d'origine, ti osserva e si allontana se gli stai troppo addosso, ma non attacca mai. Guadagnati la sua fiducia e diventerà un mercante e la chiave dell'intero sistema dei villaggi: scambi, il **[Nucleo del villaggio](Village-Core.md)** e le **[strutture](Alien-Structures.md)** aliene.

Il ciclo: _scopri un insediamento → guadagna fiducia → sblocca scambi → insegna al villaggio a costruire → diventa un motore → si aprono strutture più profonde (e un boss)._

## Dove vivono

- Compaiono naturalmente su **Greenxertz** (e nel `terraformed_meadow` maturo).
- Piccoli gruppi compaiono anche su **Cindara** e **Glacira**.
- In creativa: l'**Uovo generatore di villico alieno** (scheda di Nerospace).

## Che aspetto hanno

Ogni villico è visivamente unico senza bisogno di un'entità nuova per ogni aspetto: un aspetto a strati, guidato da un seme:

- **Tinta individuale**: un seme di colore memorizzato dà a ogni villico una sfumatura leggermente diversa, così non ce ne sono

  due identici. È deterministico: un dato villico ha sempre lo stesso aspetto.

- **Pelle per pianeta**: Greenxertz verde/acciaio, Cindara brace/rosso, Glacira brina/pallido. All'interno di

  Greenxertz, il prato maturo sfoggia un set di accessori più chiaro.

- **Calore dell'umore**: man mano che la tua reputazione presso un villaggio cresce, la tinta dei suoi villici si scalda, così puoi leggere

  la fiducia a colpo d'occhio.

- **Occhi luminosi e cristalli sulle spalle**: emissivi, visibili al buio.

## Guadagnare fiducia (reputazione)

Ogni villico tiene traccia di un **punteggio di reputazione per giocatore (0–100)** suddiviso in **6 livelli**:

| Tier | Nome       | Sblocca                                                                                        |
| ---- | ---------- | ---------------------------------------------------------------------------------------------- |
| T0   | Estraneo   | niente: rifiuta di commerciare (scuote la testa diffidente) |
| T1   | Conoscente | scambi di base                                                                                 |
| T2   | Alleato    | scambi intermedi · può insegnare **edifici di livello 1**                                      |
| T3   | Allied     | scambi rari · **edifici di livello 2**                                                         |
| T4   | Honored    | scambi di equipaggiamento esclusivo                                                            |
| T5   | Kin        | scambi migliori                                                                                |

La reputazione cresce tramite:

- **Doni**: fai clic destro su un villico tenendo in mano un oggetto apprezzato (**quarzo Xertz**, **lingotto di Nerosium**,

  **Frammento alieno** o **smeraldi**). Ne prende uno, con le particelle del villico felice.

- **Volume di scambi**: ogni scambio completato fa salire un po' la fiducia.

- **Missioni**: consegnare un compito del villaggio a un **[Nucleo del villaggio](Village-Core.md)** aumenta la fiducia di tutti i

  villici vicini.

La reputazione è indicizzata solo dall'UUID del giocatore Minecraft: nessun nome o registro delle interazioni viene inviato da nessuna parte (sicuro per POPIA/GDPR).

## Fidato

Fai clic destro su un villico di **livello 1+** per aprire la **schermata di scambio del gioco base**. Le offerte dipendono dal livello e sono **cumulative** (un villico con più fiducia offre anche quelle dei livelli inferiori). La valuta sono gli smeraldi, quindi i beni sono utili in qualsiasi modpack.

| Tier | Offerte di esempio                                                                                                     |
| ---- | ---------------------------------------------------------------------------------------------------------------------- |
| T1   | vendi 12 quarzo Xertz → 1 smeraldo · 1 smeraldo → 3 ferro · 2 smeraldi → 6 pane                                        |
| T2   | 4 smeraldi → lingotto di Nerosium · 1 smeraldo + 8 Nerosteel grezzo → 4 lingotti di Nerosteel · vendi Frammenti alieni |
| T3   | 8 smeraldi → diamante · 5 smeraldi → Tanica di carburante per razzi                                                    |
| T4   | 12 smeraldi + 2 Rottami di tecnologia aliena → Nucleo alieno · 16 smeraldi → **[Risonatore Xertz](Alien-Gear.md)**     |
| T5   | 18 smeraldi → 3 diamanti · Nucleo alieno + 24 smeraldi → **[Grav Striders](Alien-Gear.md)**                            |

I livelli più alti sono l'unica fonte di scambio dell'esclusivo **[equipaggiamento dell'Artefice](Alien-Gear.md)**.

## Dettagli

- ID: `nerospace:alien_villager` · categoria CREATURE · dimensioni 0.6 × 1.95
- La variante (pianeta / bioma d'origine / seme di colore) è sincronizzata e salvata; la reputazione è salvata per ogni villico.
- Voice currently aliases the vanilla villager sounds.

Vedi anche: **[Nucleo del villaggio](Village-Core.md)** · **[Strutture aliene](Alien-Structures.md)** · **[Equipaggiamento alieno](Alien-Gear.md)** · **[Creature](Creatures.md)**.
