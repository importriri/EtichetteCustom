package app.core;

/**
 * Suite per {@link SerialWindow}. Runner a mano, niente JUnit, exit 1 se fallisce.
 */
public final class SerialWindowTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        window_threeDigits_incrementsOnlyTheTail();
        window_widerWindow_eatsMoreDigits();
        window_padding_isPreserved();
        window_codeIsAllDigits_hasEmptyPrefix();
        window_tailNotAllDigits_isRejected();
        window_widthLongerThanCode_isRejected();
        window_widthOutOfRange_isRejected();
        window_emptyCode_isRejected();
        run_withinCapacity_isAccepted();
        run_exceedingCapacity_isBlockedUpFront();
        run_fullCapacity_endsOnTheMaximum();
        run_exceedingCapacity_producesNoCodesAtAll();
        at_beyondCapacity_throws();

        System.out.println("SerialWindow: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- casi felici ----------------------------------------------------------

    private static void window_threeDigits_incrementsOnlyTheTail() {
        SerialWindow w = SerialWindow.of("TST-0000-00-001", 3);
        same("prima etichetta", "TST-0000-00-001", w.at(0));
        same("seconda etichetta", "TST-0000-00-002", w.at(1));
        same("prefisso intatto", "TST-0000-00-", w.prefix());
    }

    private static void window_widerWindow_eatsMoreDigits() {
        SerialWindow w = SerialWindow.of("AB0099", 4);
        same("finestra a 4 cifre", "AB0100", w.at(1));
        same("prefisso ridotto", "AB", w.prefix());
    }

    private static void window_padding_isPreserved() {
        same("lo zero-padding non si perde", "AB010", SerialWindow.of("AB009", 3).at(1));
    }

    private static void window_codeIsAllDigits_hasEmptyPrefix() {
        SerialWindow w = SerialWindow.of("00042", 5);
        same("prefisso vuoto", "", w.prefix());
        same("incremento", "00043", w.at(1));
    }

    // --- validazione ----------------------------------------------------------

    private static void window_tailNotAllDigits_isRejected() {
        rejects("finestra a cavallo di un trattino",
                new Runnable() {
                    public void run() {
                        SerialWindow.of("TST-0000-00-001", 5);
                    }
                });
    }

    private static void window_widthLongerThanCode_isRejected() {
        rejects("finestra più lunga del codice",
                new Runnable() {
                    public void run() {
                        SerialWindow.of("12", 3);
                    }
                });
    }

    private static void window_widthOutOfRange_isRejected() {
        rejects("zero cifre", new Runnable() {
            public void run() {
                SerialWindow.of("ABC123", 0);
            }
        });
        rejects("oltre il massimo", new Runnable() {
            public void run() {
                SerialWindow.of("ABC123456789", 99);
            }
        });
    }

    private static void window_emptyCode_isRejected() {
        rejects("codice vuoto", new Runnable() {
            public void run() {
                SerialWindow.of("", 3);
            }
        });
    }

    // --- pre-flight del giro --------------------------------------------------

    private static void run_withinCapacity_isAccepted() {
        SerialWindow w = SerialWindow.of("X001", 3);
        yes("999 etichette da 001 ci stanno", w.remaining() == 999);
        w.checkRun(999);
        passed++;
        System.out.println("  ok  checkRun(999) non lancia");
    }

    private static void run_exceedingCapacity_isBlockedUpFront() {
        final SerialWindow w = SerialWindow.of("X001", 3);
        rejects("1000 etichette con 3 cifre", new Runnable() {
            public void run() {
                w.checkRun(1000);
            }
        });
    }

    private static void run_fullCapacity_endsOnTheMaximum() {
        String[] codes = SerialWindow.of("X001", 3).run(999);
        yes("giro completo", codes.length == 999);
        same("ultima del giro", "X999", codes[998]);
    }

    private static void run_exceedingCapacity_producesNoCodesAtAll() {
        SerialWindow w = SerialWindow.of("X998", 3);
        String[] codes = null;
        try {
            codes = w.run(5);
        } catch (RuntimeException expected) {
            // atteso
        }
        yes("nessun codice materializzato se il giro non ci sta", codes == null);
    }

    private static void at_beyondCapacity_throws() {
        final SerialWindow w = SerialWindow.of("X999", 3);
        same("l'ultima possibile esce", "X999", w.at(0));
        rejects("quella dopo no", new Runnable() {
            public void run() {
                w.at(1);
            }
        });
    }

    // --- helper ---------------------------------------------------------------

    private static void same(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("  ok  " + what + " -> \"" + actual + "\"");
        } else {
            failed++;
            System.out.println("FAIL  " + what + ": atteso \"" + expected + "\", ottenuto \"" + actual + "\"");
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
        } catch (IllegalStateException e) {
            passed++;
            System.out.println("  ok  " + what + " -> " + e.getMessage());
        }
    }
}
