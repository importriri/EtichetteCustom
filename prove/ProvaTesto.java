package prove;

import app.render.Misuratore;
import app.render.Testo;

/** Regression coverage for automatic long-code wrapping. */
public final class ProvaTesto {
    private static final Misuratore FIXED = new Misuratore() {
        @Override public double larghezza(String text, double bodyMm, boolean bold) {
            return text.length() * bodyMm * .6;
        }
    };

    private ProvaTesto() { }

    public static void esegui() {
        Prove.suite("Text wrapping and fallback");

        String source = "210150.002_02-01.262350009";
        String[] groups = Testo.parti(source);
        Prove.uguale("logical grouping finds five source blocks", 5, groups.length);
        Prove.uguale("first logical group stays exact", "210150", groups[0]);
        Prove.uguale("second logical group stays exact", "002", groups[1]);
        Prove.uguale("last logical group stays exact", "262350009", groups[4]);
        Prove.uguale("a selected group is derived without changing the source",
                "002", Testo.parte(source, 2));
        Prove.uguale("group zero always means the complete source", source, Testo.parte(source, 0));

        String code = "740125.003_01-02_584700349";
        Testo.Esito balanced = Testo.componi(code, 46, 4.4, 3, false, FIXED);
        Prove.uguale("a long code fits on two lines", 2, balanced.quanteRighe());
        Prove.vero("the first cut follows a natural separator", naturale(balanced.righe()[0]));
        Prove.vero("the two lines stay reasonably balanced",
                Math.abs(balanced.righe()[0].length() - balanced.righe()[1].length()) <= 4);
        Prove.vero("full-size text is retained when it already fits", !balanced.rimpicciolito());
        Prove.vicino("the requested body size is retained", 4.4, balanced.corpo(), .001);
        Prove.uguale("joining the lines reconstructs the code", code,
                balanced.righe()[0] + balanced.righe()[1]);

        String hiddenSeparators = "210150 002 02 01 262350009";
        Testo.Esito linuxRegression = Testo.componi(hiddenSeparators, 18, 4.4, 3, false, FIXED);
        Prove.uguale("the photographed Linux case uses three logical lines", 3,
                linuxRegression.quanteRighe());
        Prove.uguale("the first numeric group stays intact", "210150", linuxRegression.righe()[0]);
        Prove.uguale("the middle numeric groups stay intact", "002 02 01", linuxRegression.righe()[1]);
        Prove.uguale("the final numeric group stays intact", "262350009", linuxRegression.righe()[2]);
        Prove.vero("logical groups are preferred even when a smaller body is needed",
                linuxRegression.rimpicciolito());

        String digits = "000000000000000000000000000001";
        Testo.Esito fallback = Testo.componi(digits, 46, 4.4, 3, false, FIXED);
        Prove.vero("digits-only text still finds a fallback wrap", fallback.quanteRighe() >= 2);
        Prove.vero("digits-only text does not shrink when fallback lines already fit",
                !fallback.rimpicciolito());
        StringBuilder rebuilt = new StringBuilder();
        for (String line : fallback.righe()) rebuilt.append(line);
        Prove.uguale("fallback wrapping never loses a digit", digits, rebuilt.toString());

        Testo.Esito oneLine = Testo.componi(digits, 46, 4.4, 1, false, FIXED);
        Prove.uguale("one requested line remains one line", 1, oneLine.quanteRighe());
        Prove.vero("one line shrinks only when needed", oneLine.rimpicciolito());
        Prove.vero("the fallback body is smaller than the requested body", oneLine.corpo() < 4.4);
        Prove.vero("the fallback body respects the lower bound", oneLine.corpo() >= 4.4 * .4 - .01);
        Prove.vero("the resulting line actually fits",
                FIXED.larghezza(oneLine.righe()[0], oneLine.corpo(), false) <= 46.001);

        Testo.Esito shortText = Testo.componi("D04", 14, 6.5, 1, true, FIXED);
        Prove.uguale("short text stays on one line", 1, shortText.quanteRighe());
        Prove.vero("short text keeps its body size", !shortText.rimpicciolito());

        Testo.Esito empty = Testo.componi("", 20, 3, 2, false, FIXED);
        Prove.uguale("empty text is handled safely", 1, empty.quanteRighe());

        Testo.Esito three = Testo.componi(code, 46, 4.4, 3, 3, false, FIXED);
        Prove.uguale("an explicit three-line layout produces three lines", 3, three.quanteRighe());
        StringBuilder threeJoined = new StringBuilder();
        for (String line : three.righe()) threeJoined.append(line);
        Prove.uguale("explicit wrapping keeps every character", code, threeJoined.toString());

        Prove.esplode("negative width is rejected", IllegalArgumentException.class,
                new Runnable() {
                    @Override public void run() {
                        Testo.componi("x", -1, 3, 2, false, FIXED);
                    }
                });
    }

    private static boolean naturale(String text) {
        if (text == null || text.isEmpty()) return false;
        return " _-./:;,".indexOf(text.charAt(text.length() - 1)) >= 0;
    }
}
