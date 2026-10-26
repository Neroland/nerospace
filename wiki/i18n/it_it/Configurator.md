# Configuratore

Lo strumento di rete: imposta cosa fa ogni faccia di un tubo, per ogni livello di risorsa.

## Obtaining

**Fabbricazione** (con forma): due lingotti di Nerosteel sopra la redstone, a forma di bacchetta.

## Come funziona

Ogni faccia di un Tubo universale ha quattro modalità di I/O indipendenti (una per livello: energia, fluidi, gas, oggetti):

- **Auto**: preleva dai fornitori e spinge verso i ricevitori (predefinito)
- **Ingresso**: solo prelievo
- **Uscita**: solo spinta
- **Disattivato**: scollegato per quel livello

Con il Configuratore:

- **Clic destro da accovacciato su un tubo** → apre il **pannello di configurazione**: una riga per faccia con il suo colore, la modalità per il livello selezionato (fai clic su ▸ per scorrere) e il suo **slot del filtro**, cioè il [Filtro per tubi o Filtro per tubi avanzato](Pipe-Filters-and-Upgrades.md) fisico installato su quella faccia. Inserisci un filtro per filtrare la faccia, toglilo per azzerarla e **passa il mouse sullo slot** per vedere esattamente cosa corrisponde al filtro.

- **Clic destro sulla faccia di un tubo** → scorre rapidamente la modalità di quella faccia per il livello attualmente selezionato.

- **Clic destro da accovacciato nel vuoto** (o su blocchi che non sono tubi) → scorre il livello modificato dalla modalità rapida

  (energia → fluidi → gas → oggetti).

Mentre hai in mano il Configuratore, ogni Tubo universale **colora le sue sei facce** con gli stessi colori delle righe del pannello (sotto blu, sopra giallo, nord rosso, sud verde, ovest viola, est arancione), le facce con un filtro installato brillano di più, e ogni tubo che contiene un filtro mostra sopra di sé una **copia fluttuante dell'oggetto filtro**, così i tratti filtrati risaltano lungo tutta una linea.

## Consigli

**Gli oggetti entrano nella rete solo attraverso le facce impostate su Ingresso**: Auto spinge fuori gli oggetti ma non li preleva mai (per energia/fluidi/gas Auto preleva comunque). Imposta su **Ingresso** la faccia che tocca ogni baule di origine o uscita di macchina; le facce che riforniscono macchine e bauli funzionano meglio su **Uscita** o Auto.

## Dettagli

- ID: `nerospace:configurator` · Dimensione della pila: 1
