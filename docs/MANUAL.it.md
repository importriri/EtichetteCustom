# Manuale operatore

## 1. Etichette

Scegli un'etichetta dalla sua anteprima. Un click apre la preparazione della
stampa. Usa **Modifica layout** soltanto quando devi cambiare il disegno fisico
dell'etichetta.

Una nuova installazione parte con una sola etichetta **Esempio**, già
modificabile. Da quel momento compaiono soltanto le etichette create o salvate
dall'utente.

La schermata rimane volutamente semplice: la ricerca compare solo quando ci sono
abbastanza etichette da renderla utile e le ultime stampe compaiono soltanto dopo
il primo giro registrato. Per creare un modello usa **Nuova etichetta**. Rinomina,
duplica ed elimina sono nel menu della singola tessera.

## 2. Prepara la stampa

Questa schermata mostra soltanto le scelte che appartengono al giro corrente.

I valori fissi non vengono ripetuti. Un dato progressivo mostra il codice di
partenza e l'intervallo esatto che verrà stampato. Un dato impostato su **Chiedi
alla stampa** mostra invece il campo da compilare per quel giro.

Imposta le copie, controlla anteprima e intervallo, poi premi **Stampa**. La
configurazione del progressivo rimane nell'editor e non viene riproposta ogni
volta che devi stampare.

## 3. Stampa

La coda reale viene scelta nella finestra di stampa del sistema operativo.

Se annulli quella finestra, **nessun progressivo avanza**. I numeri vengono
consumati soltanto dopo una stampa completata con successo. Subito dopo vengono
salvati il nuovo stato dell'etichetta e il registro del giro.

## 4. Modifica layout

L'editor si apre senza nessun elemento selezionato. Il pannello a destra rimane
quasi vuoto finché non clicchi qualcosa sulla carta o nell'elenco degli elementi.

Il flusso normale è:

1. clicca un elemento;
2. trascinalo per spostarlo;
3. trascina una maniglia blu per ridimensionarlo;
4. usa il pannello contestuale soltanto per ciò che non puoi fare direttamente
   sulla carta.

Un QR rimane quadrato mentre lo ridimensioni. Le misure X/Y/dimensioni rimangono
nascoste dietro **Misure precise**. Lo scambio larghezza/altezza dell'etichetta e
i dati avanzati sono raccolti nel menu **Altro** in alto, invece di occupare
spazio permanente.

Scorciatoie principali:

- `R` — ruota di 90°;
- `Ctrl+D` — duplica;
- `Canc` — elimina;
- `Ctrl+Z` — annulla;
- `Ctrl+Y` oppure `Ctrl+Shift+Z` — ripeti.

## 5. Testo leggibile

Quando selezioni un testo compaiono soltanto le scelte comuni:

- un selettore per sinistra, centro o destra;
- un selettore per automatico, 1, 2 o 3 righe;
- **Mostra punti e simboli**;
- un solo pulsante **Ruota 90°**.

Premendo più volte **Ruota 90°** passi da 0° a 90°, 180°, 270° e di nuovo 0°.
La disposizione automatica preferisce i separatori naturali del codice quando
deve andare a capo e non deve perdere caratteri in silenzio.

**Mostra punti e simboli** cambia soltanto la scritta visibile. QR e barcode
mantengono sempre il valore sorgente completo.

## 6. Contenuto e valori condivisi

Ogni QR, barcode o testo legge da un contenuto. L'interfaccia evita di mostrarti
gli identificatori interni usati nei file.

Il comportamento corrente viene riassunto in modo semplice:

- **Non cambia** — il valore resta salvato con l'etichetta;
- **Aumenta automaticamente** — avanza una parte numerica;
- **Chiesto alla stampa** — viene inserito a ogni giro.

Apri **Come cambia…** soltanto quando devi modificare quel comportamento o il
numero di cifre che aumentano.

Quando più elementi usano lo stesso valore, compare un unico controllo compatto,
per esempio **QR + Testo**. Aprilo soltanto se vuoi rendere indipendente
l'elemento selezionato. Un elemento indipendente può anche usare **Usa contenuto
esistente…** per collegarsi a un valore già presente nell'etichetta.

## 7. QR e barcode

Le schede QR e barcode mostrano prima un risultato semplice sulla leggibilità.
Il livello di correzione QR e le misure tecniche rimangono dietro **Opzioni QR**
o **Misure precise**.

Se un QR è troppo piccolo, allargalo con una maniglia. Se è troppo vicino a un
bordo, spostalo verso l'interno. Un contenuto non valido viene segnalato
direttamente nel pannello.

Per la verifica definitiva conta sempre anche la prova sul supporto e sulla
stampante reale.

## 8. Windows e scaling

Il flusso principale evita `JSpinner` e usa geometria disegnata
dall'applicazione. La pipeline verifica Etichette Custom anche su Windows nativo
a più scale, corrispondenti ai profili comuni 100%, 125%, 150% e 200%.

Lo scaling di Windows non cambia le dimensioni fisiche dell'etichetta: stampa ed
esportazione continuano a usare i millimetri.

## 9. Impostazioni

**Generale** contiene le cartelle delle etichette e del registro. **Stampante**
contiene i metadati della stampante e il DPI usato dai controlli di leggibilità.

**Manuale** contiene questa guida in italiano e inglese. **Info** contiene il
collegamento al repository GitHub.
