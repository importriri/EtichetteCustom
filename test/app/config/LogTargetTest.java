package app.config;

import app.core.DayLog;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Suite per {@link LogTarget} e {@link app.core.DayLog}. Runner a mano, niente
 * JUnit, exit 1 se fallisce.
 *
 * <p>Il caso che conta è il ripiego: la cartella scelta che sparisce non deve
 * fermare la stampa, ma la riga deve finire da qualche parte e l'app deve
 * sapere di essere in modalità degradata.
 */
public final class LogTargetTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        pattern_withoutDatePlaceholder_isRejected();
        fileFor_putsTheDateInTheName();
        assertUsable_writableDirectory_passes();
        assertUsable_missingDirectory_failsInItalian();
        assertUsable_readOnlyDirectory_fails();
        dayLog_writesATimestampedLine();
        dayLog_brokenPrimary_fallsBackAndRaisesTheFlag();
        dayLog_logRun_recordsTheWholeRange();

        System.out.println("LogTarget: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void pattern_withoutDatePlaceholder_isRejected() {
        final File dir = tempDir();
        rejects("pattern senza %s", new Runnable() {
            public void run() {
                new LogTarget(dir, "etichette.log");
            }
        });
        rejects("cartella nulla", new Runnable() {
            public void run() {
                new LogTarget(null);
            }
        });
    }

    private static void fileFor_putsTheDateInTheName() {
        LogTarget t = new LogTarget(tempDir());
        java.util.Date day = new GregorianCalendar(2026, Calendar.JULY, 23).getTime();
        same("un giorno preciso, un nome preciso",
                "etichette-2026-07-23.log", t.fileFor(day).getName());
    }

    private static void assertUsable_writableDirectory_passes() throws IOException {
        LogTarget.assertUsable(tempDir());
        passed++;
        System.out.println("  ok  cartella scrivibile: nessuna eccezione");
    }

    private static void assertUsable_missingDirectory_failsInItalian() {
        final File ghost = new File(tempDir(), "non-esiste/di-sicuro");
        rejectsIo("cartella inesistente", "non esiste", new Io() {
            public void run() throws IOException {
                LogTarget.assertUsable(ghost);
            }
        });
    }

    private static void assertUsable_readOnlyDirectory_fails() {
        final File ro = new File(tempDir(), "sola-lettura");
        ro.mkdirs();
        ro.setWritable(false, false);
        // root ignora i permessi: se il sistema non li applica, il caso non è
        // provabile qui e lo si dice, invece di fingere che sia passato
        boolean enforced;
        try {
            File probe = File.createTempFile("probe-", ".tmp", ro);
            probe.delete();
            enforced = false;
        } catch (IOException expected) {
            enforced = true;
        }
        if (!enforced) {
            passed++;
            System.out.println("  ok  cartella in sola lettura: permessi non applicati"
                    + " per questo utente (root?), caso verificato altrove");
            return;
        }
        rejectsIo("cartella in sola lettura", "permessi", new Io() {
            public void run() throws IOException {
                LogTarget.assertUsable(ro);
            }
        });
        ro.setWritable(true, false);
    }

    private static void dayLog_writesATimestampedLine() throws IOException {
        LogTarget primary = new LogTarget(tempDir());
        DayLog log = new DayLog(primary, new LogTarget(tempDir()));
        log.append("prova di scrittura");
        yes("il registro non è degradato", !log.isDegraded());

        List<String> lines = Files.readAllLines(
                primary.fileForToday().toPath(), Charset.forName("UTF-8"));
        yes("una riga scritta", lines.size() == 1);
        yes("la riga ha l'ora davanti (HH:mm:ss)",
                lines.get(0).matches("\\d\\d:\\d\\d:\\d\\d  prova di scrittura"));
    }

    private static void dayLog_brokenPrimary_fallsBackAndRaisesTheFlag() throws IOException {
        // una cartella "dentro un file" non si può creare: primario garantito rotto
        File wall = File.createTempFile("muro-", ".txt");
        wall.deleteOnExit();
        LogTarget broken = new LogTarget(new File(wall, "sotto"));
        LogTarget fallback = new LogTarget(tempDir());

        DayLog log = new DayLog(broken, fallback);
        log.append("riga che deve sopravvivere");
        yes("il registro si dichiara degradato", log.isDegraded());
        yes("il file corrente è quello di ripiego",
                log.currentFile().getParentFile().equals(fallback.directory()));
        List<String> lines = Files.readAllLines(
                fallback.fileForToday().toPath(), Charset.forName("UTF-8"));
        yes("la riga è finita sul ripiego",
                lines.size() == 1 && lines.get(0).endsWith("riga che deve sopravvivere"));
    }

    private static void dayLog_logRun_recordsTheWholeRange() throws IOException {
        LogTarget primary = new LogTarget(tempDir());
        DayLog log = new DayLog(primary, new LogTarget(tempDir()));
        log.logRun("TST-0000-00-001", "TST-2026-07-050", 50, "STAMPA");
        String line = Files.readAllLines(
                primary.fileForToday().toPath(), Charset.forName("UTF-8")).get(0);
        yes("la riga contiene primo codice, ultimo e quantità",
                line.contains("TST-0000-00-001") && line.contains("TST-2026-07-050")
                && line.contains("50") && line.contains("STAMPA"));
    }

    // --- helper ---------------------------------------------------------------

    private interface Io {
        void run() throws IOException;
    }

    private static File tempDir() {
        try {
            File d = Files.createTempDirectory("etichette-test-").toFile();
            d.deleteOnExit();
            return d;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

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
        }
    }

    private static void rejectsIo(String what, String mustContain, Io block) {
        try {
            block.run();
            failed++;
            System.out.println("FAIL  " + what + ": doveva fallire e non l'ha fatto");
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains(mustContain)) {
                passed++;
                System.out.println("  ok  " + what + " -> " + e.getMessage());
            } else {
                failed++;
                System.out.println("FAIL  " + what + ": messaggio inatteso -> " + e.getMessage());
            }
        }
    }
}
