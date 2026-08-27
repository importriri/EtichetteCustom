# Manuale operatore

## 1. Vetrina

All'avvio scegli l'etichetta dalla sua anteprima reale. Un click normale apre la
**Modalità operatore**. Usa **Modifica layout** soltanto quando devi cambiare il
disegno dell'etichetta.

Su una nuova installazione compare una sola etichetta **Esempio**. Da quel
momento la vetrina mostra soltanto le etichette che hai salvato o creato.

La ricerca filtra le etichette per nome. Le impostazioni sono raccolte nel
pulsante **Impostazioni…**.

## 2. Prepara il giro

La schermata mostra una scheda per ogni dato realmente usato dall'etichetta.
Se QR e testo usano lo stesso codice, il valore viene inserito una sola volta.

Imposta il numero di copie. Ogni dato progressivo mostra il primo e l'ultimo
codice che usciranno nel giro.

Un'etichetta può avere più progressivi indipendenti: ognuno mantiene il proprio
prefisso, la propria parte numerica e il proprio contatore.

## 3. Stampa

Controlla il riepilogo e premi **Stampa**. La coda reale viene scelta nella
finestra di stampa del sistema operativo.

Se annulli quella finestra, **nessun progressivo avanza**. I numeri vengono
consumati soltanto dopo una stampa completata con successo.

## 4. Modifica layout

L'editor è pensato per essere usato soprattutto con il mouse:

1. clicca un elemento per selezionarlo;
2. trascinalo per spostarlo;
3. trascina una maniglia blu per ridimensionarlo;
4. usa il pannello a destra solo per le scelte che non si fanno direttamente
   sulla carta.

Un QR rimane quadrato mentre lo ridimensioni. La griglia serve come riferimento
visivo e gli spostamenti vengono mantenuti entro la carta quando possibile.

Le misure X/Y/larghezza e il corpo del testo non sono mostrate durante il lavoro
normale. Apri **Misure precise** soltanto quando ti serve inserire un valore in
millimetri.

Scorciatoie principali:

- `R` — ruota di 90°;
- `Ctrl+D` — duplica;
- `Canc` — elimina;
- `Ctrl+Z` — annulla;
- `Ctrl+Y` oppure `Ctrl+Shift+Z` — ripeti.

## 5. Testo leggibile

Quando selezioni un elemento di testo puoi scegliere direttamente:

- allineamento a sinistra, centro o destra;
- disposizione automatica oppure 1, 2 o 3 righe;
- rotazione 0°, 90°, 180° o 270°;
- **Mostra punti e simboli**.

La disposizione automatica prova a mantenere il testo grande e leggibile,
preferendo i separatori naturali del codice quando deve andare a capo. Non deve
mai perdere caratteri in silenzio.

**Mostra punti e simboli** cambia soltanto la scritta visibile. Il valore usato
dal QR o dal barcode rimane sempre quello completo. Per esempio il QR può
contenere `210150.022_02-01.262350009` mentre il testo viene presentato senza i
separatori scelti per la stampa.

## 6. Stesso codice tra più elementi

QR, testo e barcode possono usare lo stesso codice. Nel pannello dell'elemento
usa **Usa lo stesso codice di** per scegliere il dato già usato da un altro
elemento.

Se vuoi separare soltanto l'elemento selezionato, premi **Usa un codice diverso**.
Non serve conoscere gli identificatori interni del file: l'interfaccia mostra i
nomi leggibili degli elementi che condividono il dato.

## 7. QR e barcode

Gli indicatori di qualità aiutano a riconoscere codici troppo piccoli o con zona
bianca insufficiente. Se il QR è troppo piccolo, allargalo direttamente tramite
una maniglia. Un errore rosso indica invece un contenuto non valido.

Per la verifica definitiva conta sempre anche la prova sul supporto e sulla
stampante reale.

## 8. Windows e ridimensionamento dell'interfaccia

L'editor usa controlli e griglia disegnati in modo coerente tra Windows e Linux.
La pipeline del progetto verifica inoltre l'interfaccia su Windows a più scale,
comprese quelle tipiche dei monitor impostati al 125%, 150% e 200%.

Se il sistema usa uno scaling elevato non è necessario cambiare le dimensioni
fisiche dell'etichetta: la geometria di stampa rimane espressa in millimetri.

## 9. Impostazioni

**Generale** contiene le cartelle dei layout e del registro. **Stampante** salva
il nome usato nel registro e il DPI utilizzato dai controlli di leggibilità.

**Manuale** contiene questa guida in italiano e inglese. **Info** contiene il
collegamento al repository GitHub.
