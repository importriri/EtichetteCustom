# Etichette Custom — manuale operatore

Programma per comporre, stampare ed esportare etichette con QR e testo. Ogni
stampa finisce in un registro giornaliero.

- **Avvio**: doppio clic su `EtichetteCustom.jar`. Serve solo Java (8 o più
  recente): niente installazione, niente rete.
- **Una schermata sola.** In cima la riga del giro — quello che si tocca ogni
  giorno. Al centro l'etichetta, con la barra strumenti che le galleggia sopra.
  A destra, e solo quando selezioni qualcosa, le proprietà di quell'elemento.
  Stampante e impostazioni sono due finestrelle dietro le icone in fondo alla
  riga del giro: si aprono una volta al mese.


---

## La riga del giro

**Codice** — il codice della prima etichetta, per esempio
`DEMO-4410.07_A2-01_000001`. Le ultime cifre avanzano di uno a ogni etichetta;
tutto il resto non viene toccato. Quante cifre avanzano lo decide il campo, e
lo vedi in **Valori → Campi**.

**Quantità** — quante etichette stampare.

**Esce** — il giro completo, scritto in viola: `…300001 → …300050`. È ciò che
verrà stampato davvero. Se non è quello che ti aspetti, fermati lì.

**Valori → Campi…** — vedi il capitolo qui sotto: è il pezzo che permette di
disegnare qualunque etichetta.

**Supporto** — larghezza e altezza in millimetri, più il pulsante **⇄** che le
scambia. Non esiste un "verso" dell'etichetta: un supporto verticale è
semplicemente 30 × 50 invece di 50 × 30, e il pulsante fa quello.

**Modello** — un layout già pronto. Cambiarlo **sostituisce** elementi e campi
attuali, quindi il programma chiede conferma.

**Stampa · PDF · PNG · SVG** — il giro, sulla stampante o su file.

---

## I campi: come si fa qualunque etichetta

Un **campo** è un valore con un nome. Gli elementi lo richiamano scrivendo il
suo nome fra graffe, e il programma lo sostituisce a ogni etichetta.


Esempio, un'etichetta con codice composto: tre campi —

| Nome | Tipo | Valore |
|---|---|---|
| `{articolo}` | Fisso | `DEMO-4410` |
| `{versione}` | Fisso | `DEMO_REV_A` |
| `{seriale}` | Progressivo, 6 cifre | `000001` |

e un QR che contiene `{disegno}.{versione}_{seriale}`. Sull'etichetta esce
`DEMO-4410.07_A2-01_000001`, e alla successiva `…000002`. Quando cambia la
commessa si cambia **il campo**, non il layout.

**I tre tipi:**

- **Fisso** — sempre lo stesso valore. Numero di disegno, versione, descrizione
  del prodotto.
- **Progressivo** — le ultime N cifre avanzano di uno a ogni etichetta, contate
  da destra. Il riquadro sotto mostra il taglio sul codice vero, con la parte
  che avanza evidenziata, e quante etichette restano prima di esaurirla.
- **Chiesto a ogni stampa** — il valore viene domandato quando premi Stampa.
  Serve per il lotto o il numero d'ordine, che cambia ogni volta ma resta
  uguale dentro il giro.

Un'etichetta può avere quanti campi vuole, e **due progressivi diversi avanzano
insieme** ognuno per conto suo. Se cancelli un campo ancora usato da qualche
elemento il programma avvisa prima: senza campo, il segnaposto resterebbe
stampato così com'è sul supporto.

---

## Gli elementi

Un elemento è una riga di testo o un QR. La barra strumenti che galleggia
sull'etichetta li governa tutti:

| Icona | Cosa fa |
|---|---|
| ✥ | Seleziona e sposta |
| T | Aggiunge una riga di testo |
| ▦ | Aggiunge un QR |
| ⟳ | Ruota di 90° l'elemento selezionato (anche col tasto **R**) |
| ⧉ | Duplica (anche **Ctrl+D**) |
| ✕ | Elimina (anche **Canc**) |
| ⊞ | Accende la griglia da 5 mm e l'aggancio (anche **Ctrl+G**) |

Se il carattere del tuo Windows non conosce uno di questi simboli, al suo posto
compare una parola: è voluto, meglio brutto che un rettangolo vuoto.

### Il pannello a destra

Compare quando selezioni qualcosa e sparisce quando clicchi sul vuoto.

**Contenuto** — il testo, con i segnaposto `{nome}` dei campi. Sotto, il
programma elenca quali campi hai richiamato; se ne scrivi uno che non esiste
compare in rosso.

**Ruota 90°** — il pulsante grande. Accanto c'è l'angolo attuale e due
scorciatoie per 0° e 180°.

**Posizione** e **Misura** — in millimetri, con i pulsanti − e + per il 10%.

### Il testo che va a capo

Solo per gli elementi di testo, la voce **Va a capo a … mm**.

Stringi quella misura e il testo si dispone **su due righe, poi tre, poi
quattro**. Il carattere non rimpicciolisce: resta della stessa altezza, ed è
quello che serve quando un codice lungo non ci sta in larghezza. Sotto il campo
il programma dice quante righe occupa adesso.

Il taglio cade sugli spazi, sui trattini e sui trattini bassi — quindi
`DEMO-4410.07_A2-01_000001` si spezza dopo un underscore, dove l'occhio se lo
aspetta, e non a metà di un gruppo di cifre. **Zero** vuol dire riga unica.

Lo stesso si fa col mouse: trascina il quadratino arancio in basso a destra di
un testo e lo stringi, guardando le righe formarsi.

## I tuoi layout

Il programma arriva **senza nessuna etichetta di clienti dentro**: solo
un'etichetta vuota con una riga di testo e un QR, da mettere a posto. I codici
di qualcun altro non viaggiano con il programma — e nemmeno i tuoi, una volta
composti: restano su questo PC.

Quando l'etichetta è come deve essere, **Salva layout…** nel pannello di destra
e le dai un nome. La ritrovi con **Apri layout…** quando vuoi, e nell'elenco al
prossimo avvio.

Ogni layout è un file, in chiaro, nella cartella `layout` dentro le
impostazioni. Si copiano su una chiavetta per portare un disegno sull'altro PC
del reparto: niente esporta, niente importa, sono file.

### Anteprima: si lavora con il mouse

L'anteprima è disegnata dallo stesso motore che stampa: quello che vedi è
quello che esce.

| Gesto | Effetto |
|---|---|
| Trascinare il corpo dell'elemento | lo sposta (aggancio a 0,1 mm; con **Shift** a 1 mm) |
| Trascinare il **quadratino arancione** in basso a destra | cambia la misura — su un testo che va a capo, **stringe la larghezza** e il testo si dispone su più righe |
| Trascinare il **pallino verde** in alto a destra | ruota (con **Shift** a scatti di 15°) |
| Rotellina del mouse | ingrandisce e rimpicciolisce |
| Frecce | spostano di 0,1 mm (con **Shift** 1 mm) |
| Tasto **R** | ruota di un quarto di giro |
| Tasti **+** e **−** | cambiano la misura |

La griglia leggera è da 5 mm, non viene stampata, e con l'icona ⊞ accesa fa
anche da **aggancio**: passando vicino a un multiplo di 5 mm l'elemento si
incastra. Mentre trascini compare una **riga arancio** quando l'elemento si
allinea con un altro o col centro dell'etichetta — è così che si centra un
testo sotto un QR senza fare i conti. Con **Shift** l'aggancio si disattiva.

### Avvisi

Sotto l'anteprima, in arancione. Compaiono quando:

- un elemento **esce dall'etichetta** — con le misure esatte di quanto sborda
- il **modulo del QR è troppo piccolo** per essere letto in sicurezza
- a quel DPI ogni modulo starebbe in **meno di due punti di stampa**: la
  stampante arrotonda e il QR esce sporco
- il QR è **sotto il minimo** che hai impostato nelle impostazioni
- il **contatore non basta** per la quantità richiesta
- un **segnaposto non corrisponde a nessun campo**: verrebbe stampato così
  com'è, graffe comprese

Un'etichetta senza avvisi è un'etichetta stampabile.

### Stampa ed esportazione

**Stampa** (verde) manda il giro alla stampante configurata nella finestrella
*Stampante*.

**PDF** mette un'etichetta per pagina in un file solo, **già ruotato nel verso
di stampa impostato**: stampandolo dal browser non serve più scegliere a mano
l'orientamento. **PNG** e **SVG** creano un file per etichetta, con il codice
nel nome; il PNG dichiara il suo DPI, quindi trascinato in Word arriva già
della dimensione fisica giusta.

#### Se stampi passando dal PDF

La strada normale è il pulsante **Stampa**: va diretto alla coda, con la
taratura di questo programma, e non passa da nessuna finestra che possa
rimettere mano alle misure. Il PDF resta comodo per archiviare, per mandare
l'etichetta a qualcun altro o per stampare da un PC dove il programma non c'è.

Il file dichiara al suo interno di volere la **stampa a dimensione reale**, così
Acrobat e Chrome non lo "adattano al foglio" da soli. Se il tuo lettore PDF
ignora quella indicazione, nella sua finestra di stampa metti:

| Voce | Come deve stare |
|---|---|
| Dimensioni / Scala | **Dimensioni effettive** oppure **100%** — mai "Adatta alla pagina" |
| Orientamento / Layout | **Verticale**. Il verso lo ha già messo il programma dentro al file: se qui scegli "orizzontale" glielo giri una seconda volta |
| Carta / Formato | il formato dell'etichetta, lo stesso configurato nel driver |
| Margini | nessuno |

Il segno che è andata storta è sempre lo stesso: un'etichetta piccola in un
angolo di un foglio grande, oppure il QR a cavallo fra due supporti.

---

## Stampante (icona 🖨) — far uscire dritta la stampa

Questa finestrella esiste per un motivo preciso: **una stampante termica tarata male
non dà errori, stampa e stampa male**. Se le etichette escono vuote, o il QR
finisce a cavallo di due supporti, la cura è qui.

### Coda di stampa

Si sceglie la stampante (per esempio `Datamax E-4203`). Sotto c'è
**la riga più importante di tutte**: verde se il driver dichiara una pagina da etichetta, arancio se no: che cosa dichiara il driver.

> `Datamax E-4203 — pagina dichiarata 50,0 x 30,0 mm, area stampabile 50,0 x 30,0 mm, 203 dpi`

Se lì c'è scritto **210 x 297 mm**, in Windows il formato di quella coda è
ancora un A4: la stampante manderà avanti il supporto per un foglio intero a
ogni etichetta, e usciranno decine di etichette vuote con il disegno sparso in
mezzo. Nessuna correzione di tiro può sistemarlo — va corretto il formato nelle
proprietà della stampante in Windows, oppure forzato qui con *Misura
personalizzata*.

**Chiedi la stampante a ogni stampa** — togliendo il segno di spunta si stampa
dritto sulla coda scelta, senza finestre. È come lavora la produzione.

### Pagina mandata al driver

| Modalità | Quando |
|---|---|
| Come l'etichetta | quasi sempre: la pagina è grande esattamente quanto il supporto |
| Quella della stampante | se il formato in Windows è già tarato e non lo si vuole toccare |
| Misura personalizzata | quando il passo del supporto è diverso dall'area stampata (etichette con gap larghi, o due etichette per passo) |

### Verso di stampa

Il verso in cui il rotolo entra nella stampante non c'entra niente con il verso
in cui l'etichetta è disegnata. Se esce coricata, prova **90°** o **270°**: è
la stessa cosa che si faceva scegliendo a mano *"Orientamento orizzontale"*
nella finestra di stampa del browser, ma salvata una volta per tutte. Vale
anche per il PDF esportato.

### Taratura del tiro

Due numeri in millimetri: positivo sposta a destra e in basso.

**Procedura, due minuti:**

1. premi **Stampa pagina di taratura**: esce una griglia da 5 mm con il bordo
   dell'etichetta e le squadrette agli angoli;
2. guarda le squadrette. Se una non esce tutta, il disegno è spostato da quel
   lato;
3. misura col righello di quanto il bordo stampato è fuori posto rispetto al
   bordo vero del supporto;
4. scrivi quella differenza **col segno cambiato** nelle correzioni X e Y;
5. ristampa la taratura e controlla. Quando il riquadro coincide col supporto,
   premi **Salva taratura**.

**Stampa un'etichetta di prova** usa il codice di prova delle Impostazioni: è
il controllo finale prima di lanciare il giro vero.

### Scala (%)

L'ultima manopola, e quella da toccare per ultima. Serve solo se il driver
riscala per conto suo: la stampa esce dritta e centrata ma **più grande o più
piccola del vero**.

1. stampa la pagina di taratura;
2. misura col righello **un quadrato della griglia**: deve essere 5,0 mm;
3. se ne misuri 4,5 il driver sta stampando al 90%: scrivi `111` nella scala
   (5,0 diviso 4,5). Se ne misuri 5,5, scrivi `91`;
4. ristampa e ricontrolla.

`100` vuol dire "non toccare niente" ed è il valore giusto quasi sempre. Se ti
serve una scala molto lontana da 100, il problema vero è il formato configurato
nel driver: guarda la riga di diagnosi in cima alla scheda.

### Che cosa riceve la stampante

- **Immagine al DPI della stampante** (default) — l'etichetta viene
  rasterizzata e mandata come disegno 1:1. Nessun driver può reinterpretarla.
- **Vettoriale** — tracciati, come nel PDF. Più nitido dove il driver lo regge
  bene.

Se la stampa esce sgranata o con i moduli del QR di misura diversa fra loro,
prova a cambiare modalità: sono due strade indipendenti verso lo stesso
risultato.

---

## Impostazioni (icona ⚙)

**Registro giornaliero** — la cartella dove finisce il log e il nome del file:
`%s` viene sostituito con la data, quindi `etichette-%s.log` produce
`etichette-2026-07-25.log`, un file nuovo ogni giorno, mai una riga
sovrascritta. La cartella viene provata **subito**, appena la scegli.

**Stampa e QR** — la **risoluzione** è quella della tua stampante: 203 dpi è il
valore più comune sulle termiche da etichette, ma c'è il menù con 300 e 600 e
la casella per scrivere il numero che ti serve. La **correzione d'errore** si
alza (QUARTILE o HIGH) se le etichette si sporcano o si graffiano. La **soglia
del modulo** è sotto quale misura compare l'avviso di lettura incerta. Il
**lato minimo QR** serve quando un cliente lo impone da capitolato: scrivi la
misura richiesta e sotto quella compare l'avviso; zero = nessun minimo.

La numerazione progressiva non sta più qui: ogni campo ha le sue cifre, in
**Valori → Campi**.

**Aspetto** — variante chiara (`latte`) o scura (`mocha`). Si vede al prossimo
avvio.

**Manuali** — i due pulsanti aprono questo manuale e la versione inglese. Si
apre la copia che viaggia con il programma, nella cartella `docs` accanto al
JAR; se non c'è, si apre quella online.

---

## Il registro giornaliero

Ogni stampa ed esportazione aggiunge una riga: ora, tipo (STAMPA / PDF / PNG /
SVG), quantità, primo e ultimo codice del giro. Siccome i codici sono
sequenziali, primo + ultimo + quantità ricostruiscono ogni singola etichetta
del giro.

Se la cartella scelta non è raggiungibile, il programma **non si ferma**:
scrive nella cartella locale di ripiego e lo dice nella barra di stato in
basso. Quando succede, controlla il percorso di rete o scegli un'altra cartella.

---

## Se qualcosa non torna

**Le etichette escono vuote, o il QR è a cavallo di due etichette.**
È il formato pagina del driver. Vai in *Stampante* e leggi la riga di
diagnosi: se dichiara una misura che non è quella del supporto, il problema è
lì. Poi: modalità pagina *Come l'etichetta*, resa *Immagine*, e stampa la
pagina di taratura.

**Esce coricata.** Verso di stampa a 90° o 270° nella scheda *Stampante*.

**È spostata di qualche millimetro.** Taratura del tiro, procedura qui sopra.

**"Contatore esaurito" / "restano N etichette, ne hai chieste M".**
La finestra di cifre non basta per il giro. Il programma si rifiuta di partire:
riavvolgere a zero produrrebbe due etichette con lo stesso QR. Riduci la
quantità, cambia il codice di partenza, o aumenta le cifre da incrementare.

**"Modulo del QR a X mm: la lettura diventa incerta".**
Il QR è troppo fitto per la misura scelta. Allarga il QR, accorcia il
contenuto, o abbassa la correzione d'errore.

**Un elemento "esce dall'etichetta".** L'avviso dice di quanto e da che parte.
Rimpicciolisci (A−), sposta, o allarga il supporto.

**Il PNG sembra piccolo o gigante in un altro programma.** Alcuni programmi
ignorano il DPI dichiarato. Per condividere, il PDF è la strada fedele.

**La barra in basso dice "Registro non scrivibile".** Le etichette sono uscite
comunque: il log è nella cartella locale di ripiego.
