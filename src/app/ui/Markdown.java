package app.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Il più piccolo lettore di Markdown che renda onestamente il manuale:
 * titoli, paragrafi, grassetto, codice in linea, liste dei due tipi, tabelle e
 * righe di separazione. Niente altro — i manuali sono scritti per questo
 * lettore, non il contrario, e tirarsi dentro una libreria per sei tag sarebbe
 * assurdo in un progetto che si consegna come un JAR solo.
 *
 * <p>Testo dentro, frammento HTML fuori: qui non c'è Swing, quindi si prova
 * tutto senza uno schermo (vedi {@code ManualRenderTest}).
 *
 * <p>L'escape avviene <b>prima</b>, e solo dopo si inserisce il markup: una
 * parentesi angolare finita per sbaglio nel manuale non può chiudere un tag che
 * non ha aperto.
 */
public final class Markdown {

    private Markdown() {
    }

    public static String toHtml(String markdown) {
        StringBuilder out = new StringBuilder();
        if (markdown == null) {
            return "";
        }
        List<String> lines = split(markdown);

        String openBlock = null;          // ul | ol | p | pre
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = raw.trim();

            if (line.startsWith("```")) {                       // fenced code
                openBlock = close(out, openBlock);
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.size() && !lines.get(i).trim().startsWith("```")) {
                    code.append(escape(lines.get(i))).append('\n');
                    i++;
                }
                out.append("<pre>").append(code).append("</pre>\n");
                continue;
            }

            if (line.isEmpty()) {
                openBlock = close(out, openBlock);
                continue;
            }

            if (line.startsWith("#")) {
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') {
                    level++;
                }
                if (level <= 6 && level < line.length() && line.charAt(level) == ' ') {
                    openBlock = close(out, openBlock);
                    out.append("<h").append(level).append('>')
                       .append(inline(line.substring(level + 1).trim()))
                       .append("</h").append(level).append(">\n");
                    continue;
                }
            }

            if (isRule(line)) {
                openBlock = close(out, openBlock);
                out.append("<hr>\n");
                continue;
            }

            if (line.startsWith("|")) {
                openBlock = close(out, openBlock);
                i = table(lines, i, out) - 1;
                continue;
            }

            String bullet = bulletText(line);
            if (bullet != null) {
                if (!"ul".equals(openBlock)) {
                    openBlock = close(out, openBlock);
                    out.append("<ul>\n");
                    openBlock = "ul";
                }
                out.append("<li>").append(inline(bullet)).append("</li>\n");
                continue;
            }

            String numbered = numberedText(line);
            if (numbered != null) {
                if (!"ol".equals(openBlock)) {
                    openBlock = close(out, openBlock);
                    out.append("<ol>\n");
                    openBlock = "ol";
                }
                out.append("<li>").append(inline(numbered)).append("</li>\n");
                continue;
            }

            // una riga rientrata sotto una voce di lista è la seconda riga di quella voce
            if (("ul".equals(openBlock) || "ol".equals(openBlock)) && raw.startsWith("  ")) {
                trimTail(out, "</li>\n");
                out.append(' ').append(inline(line)).append("</li>\n");
                continue;
            }

            if (!"p".equals(openBlock)) {
                openBlock = close(out, openBlock);
                out.append("<p>");
                openBlock = "p";
            } else {
                out.append(' ');
            }
            out.append(inline(line));
        }
        close(out, openBlock);
        return out.toString();
    }

    // --- blocchi --------------------------------------------------------------

    /** @return l'indice della prima riga dopo la tabella */
    private static int table(List<String> lines, int start, StringBuilder out) {
        boolean header = start + 1 < lines.size() && isSeparator(lines.get(start + 1));
        out.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">\n");
        int i = start;
        boolean first = true;
        while (i < lines.size() && lines.get(i).trim().startsWith("|")) {
            if (isSeparator(lines.get(i))) {
                i++;
                continue;
            }
            String cell = (first && header) ? "th" : "td";
            out.append("<tr>");
            for (String c : cells(lines.get(i))) {
                out.append('<').append(cell).append('>').append(inline(c))
                   .append("</").append(cell).append('>');
            }
            out.append("</tr>\n");
            first = false;
            i++;
        }
        out.append("</table>\n");
        return i;
    }

    private static List<String> cells(String row) {
        List<String> out = new ArrayList<String>();
        String body = row.trim();
        if (body.startsWith("|")) {
            body = body.substring(1);
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }
        for (String c : body.split("\\|", -1)) {
            out.add(c.trim());
        }
        return out;
    }

    private static boolean isSeparator(String line) {
        String s = line.trim();
        if (!s.startsWith("|")) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '|' && c != '-' && c != ':' && c != ' ') {
                return false;
            }
        }
        return s.indexOf('-') >= 0;
    }

    private static boolean isRule(String line) {
        if (line.length() < 3) {
            return false;
        }
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != '-') {
                return false;
            }
        }
        return true;
    }

    private static String bulletText(String line) {
        if (line.startsWith("- ") || line.startsWith("* ")) {
            return line.substring(2).trim();
        }
        return null;
    }

    private static String numberedText(String line) {
        int i = 0;
        while (i < line.length() && Character.isDigit(line.charAt(i))) {
            i++;
        }
        if (i == 0 || i + 1 >= line.length()) {
            return null;
        }
        if (line.charAt(i) != '.' || line.charAt(i + 1) != ' ') {
            return null;
        }
        return line.substring(i + 2).trim();
    }

    private static String close(StringBuilder out, String open) {
        if ("ul".equals(open)) {
            out.append("</ul>\n");
        } else if ("ol".equals(open)) {
            out.append("</ol>\n");
        } else if ("p".equals(open)) {
            out.append("</p>\n");
        }
        return null;
    }

    private static void trimTail(StringBuilder out, String tail) {
        int at = out.lastIndexOf(tail);
        if (at >= 0 && at == out.length() - tail.length()) {
            out.setLength(at);
        }
    }

    // --- in linea -------------------------------------------------------------

    /** Prima l'escape, poi il markup: dal testo del manuale non può nascere un tag. */
    static String inline(String text) {
        String s = escape(text);
        s = wrap(s, "**", "<b>", "</b>");
        s = wrap(s, "`", "<tt>", "</tt>");
        return s;
    }

    /** Solo a coppie: un marcatore spaiato resta com'è stato scritto. */
    private static String wrap(String s, String marker, String open, String close) {
        StringBuilder out = new StringBuilder();
        int from = 0;
        for (;;) {
            int a = s.indexOf(marker, from);
            if (a < 0) {
                break;
            }
            int b = s.indexOf(marker, a + marker.length());
            if (b < 0) {
                break;
            }
            out.append(s, from, a).append(open)
               .append(s, a + marker.length(), b).append(close);
            from = b + marker.length();
        }
        out.append(s.substring(from));
        return out.toString();
    }

    static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static List<String> split(String text) {
        List<String> out = new ArrayList<String>();
        for (String l : text.split("\n", -1)) {
            out.add(l.endsWith("\r") ? l.substring(0, l.length() - 1) : l);
        }
        return out;
    }
}
