# Nucleo della stazione

> Questa pagina è una bozza tradotta automaticamente; fa fede la versione inglese: [English](../../Station-Core.md).

<!-- nerospace:render -->
<p align="right"><img src="../../images/station_core.png" alt="Station Core" width="150" align="right"></p>
<!-- /nerospace:render -->

Il blocco di ancoraggio permanente di una stazione fondata da un giocatore.

## Panoramica

Ogni stazione fondata con una **[Carta della stazione](Station-Charter.md)** è costruita attorno a un Nucleo della stazione al centro. Il Nucleo segna la registrazione della stazione e mantiene persistente l'intera struttura: finché resta al suo posto, la stazione è elencata come destinazione per i razzi ed è raggiungibile.

## Come ottenerlo

**Non fabbricabile.** Un Nucleo della stazione viene piazzato automaticamente quando si fonda una stazione (tramite la schermata di denominazione della carta o l'opzione di attracco *Nuova stazione* del razzo). Emette una luce tenue ed è **indistruttibile**: la stazione è permanente, quindi le tue costruzioni al suo interno sono al sicuro.

## Come funziona

- **Ancoraggio e persistenza:** il Nucleo lega la stazione al suo slot nel registro e sopravvive a riavvii e scaricamenti dei chunk. Una volta costruita, la stazione **non si rigenera mai**: tutti i blocchi che piazzi o rimuovi al suo interno vengono conservati nelle visite successive.
- **Rinominare:** fai clic destro sul Nucleo per aprire una console di rinomina. Il nuovo nome si aggiorna ovunque: nel selettore di attracco del razzo e nel nodo di viaggio della piattaforma di atterraggio della stazione.
- **Riservato al fondatore:** solo il giocatore che ha fondato la stazione può rinominarla (l'ID del fondatore è memorizzato localmente solo per questo controllo). Chiunque può comunque **volare** verso qualsiasi stazione.
- **Atterraggio:** volando verso questa stazione il tuo razzo atterra sulla **rampa registrata più vicina al Nucleo** (per impostazione predefinita la piattaforma di livello 2 della camera stagna, o qualsiasi rampa costruita più vicino al Nucleo).
- **Rimuovere una stazione:** poiché il Nucleo è permanente, smantellala con il comando `/nerospace station remove`: stai sul Nucleo oppure indica uno slot da `/nerospace station list`. Solo il fondatore (o un amministratore in creativa) può rimuoverla.

## Dettagli

- ID: `nerospace:station_core` · Dimensione: la dimensione della stazione · Indistruttibile
