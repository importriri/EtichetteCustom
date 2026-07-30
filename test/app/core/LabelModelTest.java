package app.core;

import java.util.List;

/**
 * Suite per {@link LabelModel}, {@link LabelElement} e {@link Templates}.
 * Runner a mano, niente JUnit, exit 1 se fallisce.
 */
public final class LabelModelTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        defaults_areAPrintableLabel();
        elements_canBeAddedRemovedAndReordered();
        element_content_substitutesTheRunningCode();
        element_limits_areEnforced();
        element_rotation_isAlwaysNormalized();
        element_scale_staysWithinLimits();
        storage_roundTrip_preservesEveryElement();
        storage_separatorsInContent_survive();
        storage_legacyLayout_isConverted();
        storage_brokenInput_fallsBackToDefaults();
        swapSides_changesOnlyTheSupport();
        fields_driveEveryPlaceholder();
        fields_run_stopsBeforeTheCounterWraps();
        wrap_breaksTextIntoLines();
        warnings_flagWhatWouldPrintWrong();
        newLabel_isPrintableStraightAway();
        layoutStore_savesAndReopensADesign();

        copy_isIndependentOfTheOriginal();

        System.out.println("LabelModel: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void defaults_areAPrintableLabel() {
        LabelModel m = LabelModel.defaults();
        yes("50 x 30 mm", m.widthMm() == 50.0 && m.heightMm() == 30.0);
        yes("203 dpi (nativo Datamax)", m.dpi() == 203);
        // QUARTILE e non MEDIUM: le etichette di reparto si sporcano, e il
        // livello di correzione più alto costa qualche modulo in più ma regge
        yes("correzione QUARTILE", m.ecc() == QrCode.Ecc.QUARTILE);
        yes("due elementi di partenza: il codice e il QR", m.elements().size() == 2);
        yes("nessun avviso sul layout di fabbrica",
                m.warnings("TST-0000-00-001").isEmpty());
    }

    private static void elements_canBeAddedRemovedAndReordered() {
        LabelModel m = LabelModel.empty();
        LabelElement a = m.add(LabelElement.text("A", "uno", 1, 5, 3));
        LabelElement b = m.add(LabelElement.qr("B", LabelElement.CODE_TOKEN, 1, 8, 10));
        yes("due elementi", m.elements().size() == 2);
        yes("il primo che usa il codice è il QR", m.firstCodeElement() == b);
        m.move(b, -1);
        yes("il QR è passato davanti", m.elements().get(0) == b);
        m.remove(a);
        yes("rimozione", m.elements().size() == 1 && m.elements().get(0) == b);
        m.move(b, -5);
        yes("spostare oltre il bordo non lancia", m.elements().get(0) == b);
    }

    private static void element_content_substitutesTheRunningCode() {
        LabelElement e = LabelElement.qr("QR", "DEMO-4410.07_A2-01_" + LabelElement.CODE_TOKEN,
                0, 0, 10);
        same("il segnaposto viene sostituito",
                "DEMO-4410.07_A2-01_000001", e.resolve("000001"));
        yes("e l'elemento lo sa", e.usesCode());

        LabelElement literal = LabelElement.text("Fisso", "DEMO-4410", 0, 0, 3);
        same("il testo fisso resta com'è", "DEMO-4410", literal.resolve("qualsiasi"));
        yes("e non dipende dal codice", !literal.usesCode());

        LabelElement english = LabelElement.text("EN", "SN {code}", 0, 0, 3);
        same("vale anche l'alias inglese", "SN X1", english.resolve("X1"));
    }

    private static void element_limits_areEnforced() {
        final LabelElement e = LabelElement.text("T", "x", 0, 0, 4);
        rejects("misura nulla", new Runnable() {
            public void run() {
                e.setSizeMm(0);
            }
        });
        rejects("misura oltre il massimo", new Runnable() {
            public void run() {
                e.setSizeMm(LabelElement.MAX_SIZE_MM + 1);
            }
        });
        rejects("posizione non numerica", new Runnable() {
            public void run() {
                e.setPosition(Double.NaN, 0);
            }
        });
        rejects("rotazione non numerica", new Runnable() {
            public void run() {
                e.setRotationDeg(Double.NaN);
            }
        });
        rejects("tipo nullo", new Runnable() {
            public void run() {
                e.setKind(null);
            }
        });
    }

    private static void element_rotation_isAlwaysNormalized() {
        LabelElement e = LabelElement.text("T", "x", 0, 0, 4);
        e.setRotationDeg(-90);
        yes("-90 diventa 270", e.rotationDeg() == 270.0);
        e.setRotationDeg(450);
        yes("450 diventa 90", e.rotationDeg() == 90.0);
        e.setRotationDeg(0);
        e.rotateQuarterTurn();
        e.rotateQuarterTurn();
        e.rotateQuarterTurn();
        e.rotateQuarterTurn();
        yes("quattro quarti di giro tornano a zero", e.rotationDeg() == 0.0);
    }

    private static void element_scale_staysWithinLimits() {
        LabelElement e = LabelElement.text("T", "x", 0, 0, 4);
        e.scaleBy(2);
        yes("raddoppia", e.sizeMm() == 8.0);
        for (int i = 0; i < 200; i++) {
            e.scaleBy(0.5);
        }
        yes("non scende sotto il minimo", e.sizeMm() >= LabelElement.MIN_SIZE_MM);
        for (int i = 0; i < 200; i++) {
            e.scaleBy(2);
        }
        yes("non sale oltre il massimo", e.sizeMm() <= LabelElement.MAX_SIZE_MM);
    }

    private static void storage_roundTrip_preservesEveryElement() {
        LabelModel m = LabelModel.empty();
        m.setSizeMm(80, 40);
        m.setDpi(300);
        m.setEcc(QrCode.Ecc.HIGH);
        m.setModuleWarnMm(0.45);
        m.setMinQrSideMm(7);
        m.setTemplateName("Prova");
        LabelElement qr = m.add(LabelElement.qr("QR grande", "P_" + LabelElement.CODE_TOKEN,
                4.5, 3.25, 22));
        qr.setRotationDeg(180);
        LabelElement text = m.add(LabelElement.text("Riga", "SN " + LabelElement.CODE_TOKEN,
                30, 35.5, 3.5));
        text.setBold(true);
        text.setAlign(LabelElement.Align.DESTRA);
        text.setRotationDeg(270);

        LabelModel back = LabelModel.fromStorage(m.toStorage());
        yes("misure", back.widthMm() == 80.0 && back.heightMm() == 40.0);
        yes("dpi e correzione", back.dpi() == 300 && back.ecc() == QrCode.Ecc.HIGH);
        yes("soglie", back.moduleWarnMm() == 0.45 && back.minQrSideMm() == 7.0);
        same("nome del modello", "Prova", back.templateName());
        yes("due elementi", back.elements().size() == 2);

        LabelElement q = back.elements().get(0);
        same("nome del QR", "QR grande", q.name());
        yes("tipo", q.kind() == LabelElement.Kind.QR);
        yes("posizione e misura", q.xMm() == 4.5 && q.yMm() == 3.25 && q.sizeMm() == 22.0);
        yes("rotazione", q.rotationDeg() == 180.0);
        same("contenuto", "P_" + LabelElement.CODE_TOKEN, q.content());

        LabelElement t = back.elements().get(1);
        yes("grassetto", t.bold());
        yes("allineamento", t.align() == LabelElement.Align.DESTRA);
        yes("rotazione del testo", t.rotationDeg() == 270.0);
    }

    private static void storage_separatorsInContent_survive() {
        LabelModel m = LabelModel.empty();
        m.add(LabelElement.text("Strano", "A;B=C,D|E%F", 1, 5, 3));
        LabelModel back = LabelModel.fromStorage(m.toStorage());
        yes("un solo elemento anche coi separatori dentro", back.elements().size() == 1);
        same("il contenuto esce intatto", "A;B=C,D|E%F", back.elements().get(0).content());
    }

    private static void storage_legacyLayout_isConverted() {
        // il formato della prima versione, quello a tre elementi fissi
        String legacy = "w=50.000;h=30.000;dpi=203;ecc=MEDIUM;cx=3.000;cy=6.000;ch=4.000;cb=1;"
                + "qx=3.000;qy=8.500;qs=18.000;sx=24.000;sy=18.000;sh=5.000;sb=1;st=F04";
        LabelModel m = LabelModel.fromStorage(legacy);
        yes("misure conservate", m.widthMm() == 50.0 && m.heightMm() == 30.0);
        yes("tre elementi ricostruiti", m.elements().size() == 3);
        yes("il codice è diventato un testo col segnaposto",
                m.elements().get(0).usesCode()
                && m.elements().get(0).kind() == LabelElement.Kind.TESTO);
        yes("il QR è al suo posto",
                m.elements().get(1).kind() == LabelElement.Kind.QR
                && m.elements().get(1).sizeMm() == 18.0);
        same("la sigla è diventata testo fisso", "F04", m.elements().get(2).content());
    }

    private static void storage_brokenInput_fallsBackToDefaults() {
        String[] broken = {null, "", "v=2;el=", "roba a caso", "v=2;w=9999;h=-3;el=x"};
        for (String s : broken) {
            LabelModel m = LabelModel.fromStorage(s);
            yes("input rotto " + (s == null ? "null" : "\"" + s + "\"") + " -> default",
                    m.widthMm() == 50.0 && m.heightMm() == 30.0 && !m.elements().isEmpty());
        }
    }

    private static void swapSides_changesOnlyTheSupport() {
        LabelModel m = LabelModel.empty();
        m.setSizeMm(50, 30);
        LabelElement e = m.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN, 10, 5, 12));
        m.swapSides();
        yes("i lati si scambiano", m.widthMm() == 30.0 && m.heightMm() == 50.0);
        // gli elementi NON si muovono: era il difetto della vecchia rotazione,
        // che spostava sotto i piedi tutto quello che l'operatore aveva appena
        // posizionato
        yes("l'elemento resta dov'era",
                e.xMm() == 10.0 && e.yMm() == 5.0 && e.rotationDeg() == 0.0);
        m.swapSides();
        yes("scambiare due volte torna al punto di partenza",
                m.widthMm() == 50.0 && m.heightMm() == 30.0);
    }

    private static void fields_driveEveryPlaceholder() {
        LabelModel m = LabelModel.empty();
        m.addField(LabelField.fixed("articolo", "DEMO-4410"));
        m.addField(LabelField.sequential("seriale", "000001", 6));
        LabelElement qr = m.add(LabelElement.qr("QR", "{articolo}_{seriale}", 3, 3, 15));

        same("i due campi finiscono nello stesso contenuto",
                "DEMO-4410_000001", qr.resolve(m.valuesAt(0)));
        same("e alla terza etichetta avanza solo il progressivo",
                "DEMO-4410_000003", qr.resolve(m.valuesAt(2)));
        yes("l'elemento dichiara quali campi usa",
                qr.tokens().size() == 2 && qr.uses("articolo") && qr.uses("seriale"));

        // due progressivi diversi avanzano insieme, ognuno per conto suo
        LabelModel two = LabelModel.empty();
        two.addField(LabelField.sequential("a", "100", 3));
        two.addField(LabelField.sequential("b", "X-0007", 4));
        LabelElement both = two.add(LabelElement.text("T", "{a}/{b}", 2, 5, 3));
        same("due contatori indipendenti", "104/X-0011", both.resolve(two.valuesAt(4)));

        rejects("due campi con lo stesso nome sarebbero ambigui", new Runnable() {
            public void run() {
                LabelModel clash = LabelModel.empty();
                clash.addField(LabelField.fixed("lotto", "A"));
                clash.addField(LabelField.fixed("lotto", "B"));
            }
        });

        LabelModel orphan = LabelModel.empty();
        orphan.add(LabelElement.text("T", "SN {inesistente}", 2, 5, 3));
        yes("un segnaposto senza campo viene segnalato",
                orphan.unknownTokens().contains("inesistente"));
        same("e intanto resta scritto com'è, invece di sparire in silenzio",
                "SN {inesistente}", orphan.elements().get(0).resolve(orphan.valuesAt(0)));
    }

    private static void fields_run_stopsBeforeTheCounterWraps() {
        LabelModel m = LabelModel.empty();
        m.addField(LabelField.sequential("seriale", "0998", 4));
        m.add(LabelElement.qr("QR", "{seriale}", 2, 2, 15));

        same("il giro produce una mappa per etichetta", "3", String.valueOf(m.run(3).size()));
        same("e i valori avanzano", "1000", m.run(3).get(2).get("seriale"));

        // un campo chiesto o fisso non si esaurisce mai
        LabelModel fixed = LabelModel.empty();
        fixed.addField(LabelField.fixed("x", "A"));
        fixed.add(LabelElement.text("T", "{x}", 2, 5, 3));
        same("un campo fisso regge qualunque quantità", "5000",
                String.valueOf(fixed.run(5000).size()));

        final LabelModel tight = LabelModel.empty();
        tight.addField(LabelField.sequential("seriale", "9998", 4));
        tight.add(LabelElement.text("T", "{seriale}", 2, 5, 3));
        rejects("il giro si ferma prima di riavvolgere il contatore", new Runnable() {
            public void run() {
                tight.run(5);
            }
        });
    }

    private static void wrap_breaksTextIntoLines() {
        LabelElement e = LabelElement.text("Codice", "DEMO-4410.07_A2-01_000001", 3, 6, 3);
        same("senza larghezza resta una riga sola", "1",
                String.valueOf(LabelLayout.lineCount(e, e.content())));

        double full = LabelLayout.textWidthMm(e.content(), false, 3);
        e.setWrapWidthMm(full / 2 + 1);
        int two = LabelLayout.lineCount(e, e.content());
        yes("stretto a metà va su due righe (" + two + ")", two >= 2);

        e.setWrapWidthMm(full / 3 + 1);
        int three = LabelLayout.lineCount(e, e.content());
        yes("stretto a un terzo su tre o più (" + three + ")", three >= three && three > two - 1);
        yes("più si stringe, più righe servono", three >= two);

        // il carattere NON rimpicciolisce: è il punto di tutta la faccenda
        same("l'altezza del carattere resta quella", "3.0", String.valueOf(e.sizeMm()));

        // il taglio cade sui separatori del codice, non a metà di un numero
        java.util.List<String> lines = LabelLayout.wrapLines(e, e.content());
        boolean cleanBreaks = true;
        for (int i = 0; i < lines.size() - 1; i++) {
            char last = lines.get(i).charAt(lines.get(i).length() - 1);
            if (last != '_' && last != '-' && last != ' ') {
                cleanBreaks = false;
            }
        }
        yes("le righe si chiudono su un separatore, non a metà di un gruppo", cleanBreaks);

        // gli a capo scritti a mano valgono sempre
        LabelElement manual = LabelElement.text("T", "riga uno\nriga due", 2, 5, 3);
        same("un a capo scritto a mano vale anche senza larghezza", "2",
                String.valueOf(LabelLayout.lineCount(manual, manual.content())));
    }

    private static void warnings_flagWhatWouldPrintWrong() {
        LabelModel out = LabelModel.empty();
        out.setSizeMm(50, 30);
        out.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN, 45, 8.5, 18));
        yes("elemento fuori dal supporto segnalato",
                contains(out.warnings("X001"), "esce dall'etichetta"));

        LabelModel tiny = LabelModel.empty();
        tiny.setSizeMm(50, 30);
        tiny.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN, 3, 3, 5));
        yes("modulo troppo piccolo segnalato in mm",
                contains(tiny.warnings("TST-0000-00-001"), "modulo a"));

        LabelModel spec = LabelModel.empty();
        spec.setSizeMm(50, 30);
        spec.setMinQrSideMm(7);
        spec.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN, 3, 3, 6));
        yes("sotto il minimo del capitolato viene segnalato",
                contains(spec.warnings("X1"), "capitolato"));

        LabelModel none = LabelModel.empty();
        yes("etichetta senza elementi segnalata",
                contains(none.warnings("X1"), "nessun elemento"));

        LabelModel orphan = LabelModel.empty();
        orphan.add(LabelElement.text("T", "{lotto}", 3, 5, 3));
        yes("segnaposto senza campo segnalato",
                contains(orphan.warnings("X1"), "non corrisponde a nessun campo"));

        // un elemento vuoto non è un difetto: non si disegna e non deve
        // riempire di avvisi chi la sigla non la usa
        LabelModel emptyText = LabelModel.empty();
        emptyText.addField(LabelField.sequential(LabelField.DEFAULT_NAME, "X1", 1));
        emptyText.add(LabelElement.text("Vuoto", "", 3, 3, 4));
        emptyText.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN, 3, 8, 15));
        yes("un elemento vuoto non genera avvisi", emptyText.warnings("X1").isEmpty());
    }

    /**
     * Il punto di partenza deve essere stampabile subito.
     *
     * <p>Nessun modello di clienti: quelli erano dati di chi li ha
     * commissionati e non stanno in un programma da consegnare a chiunque.
     * Resta un'etichetta vuota ma funzionante, e i disegni veri li salva
     * l'operatore con {@link LayoutStore}.
     */
    private static void newLabel_isPrintableStraightAway() {
        LabelModel m = LabelModel.defaults();
        same("si chiama \"Nuova etichetta\" finché non la salvi",
                Templates.NUOVA, m.templateName());
        yes("ha un campo progressivo pronto", m.mainField() != null
                && m.mainField().type() == LabelField.Type.SEQUENZIALE);
        yes("due elementi: il codice scritto e il codice nel QR", m.elements().size() == 2);
        yes("nessun avviso: si può premere Stampa e basta", m.warnings("0001").isEmpty());
        yes("nessun segnaposto orfano", m.unknownTokens().isEmpty());

        // e il giro funziona senza toccare nient'altro
        same("il giro parte", "0003", m.run(3).get(2).get(LabelField.DEFAULT_NAME));

        LabelModel dirty = LabelModel.empty();
        dirty.addField(LabelField.fixed("x", "1"));
        dirty.add(LabelElement.text("T", "{x}", 2, 5, 3));
        Templates.reset(dirty);
        yes("reset ripulisce campi ed elementi di prima",
                dirty.fields().size() == 1 && dirty.elements().size() == 2);
    }

    private static void layoutStore_savesAndReopensADesign() throws Exception {
        java.nio.file.Path home = java.nio.file.Files.createTempDirectory("etichette-layouts");
        System.setProperty("user.home", home.toString());

        LabelModel m = LabelModel.defaults();
        m.setSizeMm(60, 40);
        m.addField(LabelField.asked("lotto", "L-01"));
        m.add(LabelElement.text("Lotto", "LOTTO {lotto}", 30, 10, 3));

        LayoutStore.save("Demo layout", m);
        yes("il layout compare nell'elenco", LayoutStore.names().contains("Demo layout"));

        LabelModel back = LayoutStore.load("Demo layout");
        yes("misura conservata", back.widthMm() == 60.0 && back.heightMm() == 40.0);
        yes("campi conservati", back.fields().size() == m.fields().size()
                && back.field("lotto") != null);
        yes("elementi conservati", back.elements().size() == m.elements().size());
        same("e prende il nome con cui l'hai salvato", "Demo layout", back.templateName());

        // i caratteri che i filesystem non digeriscono diventano trattini,
        // invece di far fallire il salvataggio in silenzio
        LayoutStore.save("Cavo 3/4\" speciale", m);
        yes("un nome con caratteri strani si salva lo stesso",
                LayoutStore.names().size() == 2);

        yes("si cancella", LayoutStore.delete("Demo layout"));
        yes("e sparisce dall'elenco", !LayoutStore.names().contains("Demo layout"));
        yes("cancellarlo due volte non lancia", !LayoutStore.delete("Demo layout"));

        rejects("un layout senza nome non si salva", new Runnable() {
            public void run() {
                try {
                    LayoutStore.save("  ", LabelModel.defaults());
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private static void copy_isIndependentOfTheOriginal() {
        LabelModel a = LabelModel.defaults();
        LabelModel b = a.copy();
        b.setSizeMm(100, 60);
        b.elements().get(0).setName("CAMBIATO");
        yes("modificare la copia non tocca l'originale",
                a.widthMm() == 50.0 && !"CAMBIATO".equals(a.elements().get(0).name()));

        LabelModel c = LabelModel.empty();
        c.copyFrom(a);
        yes("copyFrom ricopia tutto",
                c.widthMm() == a.widthMm() && c.elements().size() == a.elements().size());
        c.elements().get(0).setName("ANCORA");
        yes("e nemmeno copyFrom condivide gli elementi",
                !"ANCORA".equals(a.elements().get(0).name()));
    }

    // --- helper ---------------------------------------------------------------

    private static boolean contains(List<String> list, String piece) {
        for (String s : list) {
            if (s.contains(piece)) {
                return true;
            }
        }
        return false;
    }

    private static void same(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("  ok  " + what + " -> \"" + actual + "\"");
        } else {
            failed++;
            System.out.println("FAIL  " + what + ": atteso \"" + expected
                    + "\", ottenuto \"" + actual + "\"");
        }
    }

    private static void yes(String what, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what);
        }
    }

    private static void rejects(String what, Runnable block) {
        try {
            block.run();
            failed++;
            System.out.println("FAIL  " + what + ": doveva essere rifiutato e non lo è stato");
        } catch (IllegalArgumentException e) {
            passed++;
            System.out.println("  ok  " + what + " -> " + e.getMessage());
        }
    }
}
