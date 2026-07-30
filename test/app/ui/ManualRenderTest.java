package app.ui;

import app.config.Manuals;

/**
 * Manual test suite for {@link Markdown} and {@link app.config.Manuals}. No
 * JUnit; exits with status 1 when a check fails.
 *
 * <p>No display is required: the Markdown reader accepts text and returns HTML,
 * while both manuals are loaded from the classpath. The documentation an
 * operator reads during a fault must remain testable without launching the UI.
 */
public final class ManualRenderTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        headings_becomeHeadings();
        paragraphs_joinTheirLines();
        boldAndCode_areMarkedUp();
        oddMarker_isLeftAlone();
        lists_closeThemselves();
        listContinuation_staysInTheItem();
        table_readsItsHeader();
        rule_becomesAnHr();
        angleBrackets_cannotBecomeTags();
        emptyInput_isEmptyOutput();
        manuals_bothLanguagesLoad();
        manuals_unknownLanguage_fallsBackToItalian();
        manuals_neverReturnNull();
        manuals_renderWithoutBlowingUp();

        System.out.println("ManualRender: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Markdown -------------------------------------------------------------

    private static void headings_becomeHeadings() {
        yes("a level-one heading becomes h1",
                Markdown.toHtml("# Title").contains("<h1>Title</h1>"));
        yes("a level-three heading becomes h3",
                Markdown.toHtml("### Section").contains("<h3>Section</h3>"));
        yes("a hash without a following space is not a heading",
                !Markdown.toHtml("#not-a-heading").contains("<h1>"));
    }

    private static void paragraphs_joinTheirLines() {
        String html = Markdown.toHtml("first line\nsecond line\n\nnew paragraph");
        yes("paragraph lines join",
                html.contains("<p>first line second line</p>"));
        yes("a blank line opens a new paragraph",
                html.contains("<p>new paragraph</p>"));
    }

    private static void boldAndCode_areMarkedUp() {
        yes("bold text is marked up",
                Markdown.toHtml("text **bold** here").contains("<b>bold</b>"));
        yes("inline code is marked up",
                Markdown.toHtml("press `Ctrl+G`").contains("<tt>Ctrl+G</tt>"));
    }

    private static void oddMarker_isLeftAlone() {
        String html = Markdown.toHtml("measure 5 ** 2 and stop");
        yes("an unmatched marker stays literal",
                html.contains("5 ** 2") && !html.contains("<b>"));
    }

    private static void lists_closeThemselves() {
        String html = Markdown.toHtml("- one\n- two\n\nafter");
        yes("an unordered list opens and closes",
                html.contains("<ul>") && html.contains("</ul>")
                && html.contains("<li>one</li>") && html.contains("<li>two</li>"));
        String numbered = Markdown.toHtml("1. first\n2. second");
        yes("an ordered list opens and closes",
                numbered.contains("<ol>") && numbered.contains("<li>second</li>"));
    }

    private static void listContinuation_staysInTheItem() {
        String html = Markdown.toHtml("- one item\n  continued below\n- another");
        yes("an indented continuation stays inside its item",
                html.contains("<li>one item continued below</li>"));
    }

    private static void table_readsItsHeader() {
        String html = Markdown.toHtml("| Field | Type |\n|---|---|\n| batch | fixed |");
        yes("the table header becomes th", html.contains("<th>Field</th>"));
        yes("table rows become td", html.contains("<td>batch</td>"));
        yes("the separator row is not rendered", !html.contains("<td>---</td>"));
    }

    private static void rule_becomesAnHr() {
        yes("a rule becomes hr", Markdown.toHtml("---").contains("<hr>"));
    }

    private static void angleBrackets_cannotBecomeTags() {
        String html = Markdown.toHtml("write <script> by mistake");
        yes("angle brackets cannot open a tag",
                html.contains("&lt;script&gt;") && !html.contains("<script>"));
    }

    private static void emptyInput_isEmptyOutput() {
        same("empty input returns empty output", "", Markdown.toHtml(""));
        same("null input returns empty output", "", Markdown.toHtml(null));
    }

    // --- manuals --------------------------------------------------------------

    private static void manuals_bothLanguagesLoad() {
        String it = Manuals.text(Manuals.IT);
        String en = Manuals.text(Manuals.EN);
        yes("the Italian text is long enough to be a manual", it.length() > 3000);
        yes("the English text is long enough to be a manual", en.length() > 3000);
        yes("the two manuals differ", !it.equals(en));
        yes("the Italian manual starts with its title",
                it.startsWith("# Etichette Custom"));
        yes("neither language reports a missing manual",
                !it.contains("Manuale non disponibile")
                && !en.contains("Manuale non disponibile"));
    }

    private static void manuals_unknownLanguage_fallsBackToItalian() {
        same("an unknown language falls back to Italian",
                Manuals.text(Manuals.IT), Manuals.text("klingon"));
        same("a null language falls back to Italian",
                Manuals.text(Manuals.IT), Manuals.text(null));
    }

    private static void manuals_neverReturnNull() {
        yes("the manual loader never returns null", Manuals.text("anything") != null);
    }

    private static void manuals_renderWithoutBlowingUp() {
        for (String lang : new String[] {Manuals.IT, Manuals.EN}) {
            String html = Markdown.toHtml(Manuals.text(lang));
            yes("manual " + lang + " renders to substantial HTML (" + html.length()
                    + " characters)", html.length() > 3000);
            yes("manual " + lang + " contains headings and paragraphs",
                    html.contains("<h1>") && html.contains("<p>"));
            // Every opened tag must close or the editor truncates the document.
            for (String tag : new String[] {"ul", "ol", "p", "table"}) {
                int open = count(html, "<" + tag + ">") + count(html, "<" + tag + " ");
                int close = count(html, "</" + tag + ">");
                yes("manual " + lang + ": " + tag + " tags balance ("
                        + open + "/" + close + ")", open == close);
            }
        }
    }

    // --- helpers --------------------------------------------------------------

    private static int count(String haystack, String needle) {
        int n = 0;
        int at = 0;
        while ((at = haystack.indexOf(needle, at)) >= 0) {
            n++;
            at += needle.length();
        }
        return n;
    }

    private static void same(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what);
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
}
