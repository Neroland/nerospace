# Rockets

I quattro razzi con equipaggio che ti portano fuori dalla Terra, che aspetto ha ciascuno e cosa fa sulla rampa e in volo. Per i valori del carburante, le dimensioni delle rampe e le destinazioni vedi [Rampa di lancio per razzi](Rocket-Launch-Pad.md); per lo scafo da trasporto senza equipaggio vedi [Razzo cargo](Cargo-Rocket.md).

## Panoramica

Schiera l'oggetto razzo su una [rampa di lancio](Rocket-Launch-Pad.md), riforniscilo, fai clic destro per aprire la console di volo e premi **Lancia**. Sali a bordo quando i motori si accendono e arrivi sulla rampa di destinazione pochi secondi dopo. Each tier reaches every destination of the tiers below it plus its own, so a Tier 3 or Tier 4
rocket still flies to [Luna](Luna) and [Mars](Mars). Every rocket can fly home.

| Tier          | Raggiunge                                                                                                        | Pad                         | Aspetto                                                                                                                                                              |
| ------------- | ---------------------------------------------------------------------------------------------------------------- | --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Livello 1** | Orbital Station, Luna                                                                                            | un blocco di rampa          | Livrea rossa, ugello singolo, quattro alette inclinate, quattro gambe di atterraggio, cabina con oblò                                                                |
| **Livello 2** | Orbital Station, Luna, Greenxertz, Mars                                                                          | 3×3                         | Livrea viola Nerosium, due booster laterali, antenna radar rotante, due alette                                                                                       |
| **Livello 3** | Orbital Station, Luna, Greenxertz, Mars, Cindara                                                                 | 3×3 circondato              | Livrea oro / verde, ugello principale circondato da quattro motori vernier, ali solari ripiegabili, braccio con antenna di scansione, cupola panoramica, ogiva lunga |
| **Livello 4** | Orbital Station, Luna, Greenxertz, Mars, Cindara, Glacira, Nyxara (after Deep Space progress) | Complesso di lancio pesante | Livrea azzurro ghiaccio, ampio stadio centrale a cinque motori, quattro booster, alette a griglia, gambe pesanti, l'ogiva più alta                                   |

Ogni livello è più grande del precedente, sia nella geometria sia nella scala, così la progressione si legge da un capo all'altro della base.

## La cabina

Ogni scafo ha una fascia di finestre all'altezza degli occhi del passeggero, ritagliata nello scafo e vetrata dall'interno con vetro colorato. Dietro c'è una cabina illuminata: una console con uno schermo sotto la finestra anteriore, una lampada a soffitto e strisce luminose sulle pareti laterali. Il passeggero è seduto in modo che gli occhi siano esattamente all'altezza della fascia di finestre, così in prima persona guardi attraverso il vetro la rampa che si allontana; dall'esterno, di notte, la cabina brilla attraverso le finestre.

Il **portello** sul lato sud resta aperto quando non c'è nessuno a bordo, con una scala che scende alla rampa, e si sigilla nel momento del lancio. Mentre è aperto puoi vedere direttamente la cabina attraverso l'apertura.

## Flying with friends (crew)

Every rocket seats **up to four players** (a server can lower this with `rockets.maxCrew`). All four tiers
have four seats; bigger tiers go further, not fuller.

- **Boarding.** Right-click the rocket to open its console and press **Take seat**. Nobody can put you in a
  rocket: you always board yourself. The button reads **Seated** once you are aboard and **Full** when every
  seat is taken; hover it to see how many are aboard. To climb out again before launch, close the console
  and sneak.
- **The pilot** is the first player aboard. Only the pilot can press **Launch**, pick the destination, or
  choose where to dock or land. Anyone else who tries is told so. Boarding later never takes the pilot's
  seat. On an empty rocket, pressing **Launch** boards you as the pilot, as before.
- **Everyone goes.** The whole crew travels together, rides the descent down, and steps out beside the
  rocket when it lands. Each of you gets the arrival message, your own Star Guide progress and your own
  first-visit record.
- **Oxygen is shared.** With two or more aboard, the rocket needs enough onboard oxygen for every seat
  for the flight. The consoles show **CREW n/4 · O₂ FOR n**: if the oxygen covers fewer seats than are
  filled, the status reads **NEEDS OXYGEN**. Load more from an Oxygen Generator or fly with fewer crew.
  On arrival the rocket's leftover oxygen is shared out between the crew as your reserve. A solo flight
  needs no onboard oxygen, as before.
- **Cooldown.** Each player has their own short launch cooldown, so riding along doesn't skip yours. If
  someone aboard is still cooling down, the pilot is told "a crew member" is, and that player sees their
  own wait.
- **Fees.** If the server runs NeroEconomy, the launch fee is charged **once per flight, to the pilot**.
- **Stations.** A members-only station is checked against the **pilot**. Crew who aren't members still
  arrive and are visitors there.
- **Disconnects.** If the pilot drops out mid-flight, the next player aboard takes over for the landing
  (their Station Charter is never spent on a "New Station" they didn't pick). A crew member who drops out
  mid-flight rejoins beside the rocket, not in the sky, as long as the server hasn't restarted.
- The rocket keeps no record of who flew in it.

## Sulla rampa

- Le gambe di atterraggio sono piantate; sul livello 3 le ali solari sono spiegate.
- I fari di navigazione lampeggiano rosso (ovest) e verde (est); una luce stroboscopica bianca lampeggia sull'ogiva.
- L'antenna radar del livello 2 ruota; i livelli 3 e 4 hanno un'antenna su un braccio laterale che scansiona avanti e indietro.
- Uno scafo rifornito emette un po' di vapore freddo dalla parete.

## Sequenza di lancio

1. **Accensione**: il portello si sigilla, i pennacchi di scarico sbocciano e tremolano sotto ogni ugello (nucleo incandescente, bordo del colore del livello), lo scafo vibra e una nube si allarga sulla rampa. Le ali solari e il braccio dell'antenna si ripiegano.
2. **Salita**: le gambe di atterraggio si ritraggono verso l'alto lungo lo scafo, gli ugelli dei motori si orientano e sul livello 4 si aprono le alette a griglia.
3. **Separazione**: superati i due terzi della salita, i booster dei livelli 2 e 4 si spengono e si staccano.
4. **Arrivo**: il razzo ricompare sulla rampa di destinazione, con le gambe abbassate e il portello che si apre.

Lo scafo e il suo vetro sono illuminati dal mondo; solo lampade, fari e scarichi sono a piena luminosità, così un razzo parcheggiato di notte è una sagoma scura con finestre luminose.

## Razzo cargo

Il [Razzo cargo](Cargo-Rocket.md) riprende lo scafo e la livrea del livello 2 in scala leggermente maggiore. Nessuno vi sale, quindi il suo portello resta sigillato e non ha scala.

## Per i contributori

La geometria dei razzi, i file Blockbench in `art/blockbench/entity/` e i fogli delle texture sono tutti generati da un'unica specifica, `tools/rocket_models.py`; l'animazione si trova in `RocketModel`. Modifica il Python, eseguilo con `--multiloader` (o `./gradlew genAssets`) e ricompila.
