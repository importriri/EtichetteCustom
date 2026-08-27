package app.ui.dati;

import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Etichetta;
import app.modello.Elemento;
import app.modello.Tipo;
import java.util.ArrayList;
import java.util.List;

/** Human-readable names for persisted identifiers without changing file compatibility. */
public final class NomiDati {
    private NomiDati() { }

    public static String nome(Etichetta eti, Campo campo) {
        if (campo == null) return "Dato";
        String raw = campo.nome() == null ? "" : campo.nome().trim();
        String lower = raw.toLowerCase(java.util.Locale.ROOT);

        if (significativo(lower)) return titolo(raw);
        if (campo.comportamento() == Comportamento.PROGRESSIVO) {
            int pos = indice(eti, campo, Comportamento.PROGRESSIVO);
            return pos <= 1 ? "Serie principale" : "Serie " + pos;
        }
        if (campo.comportamento() == Comportamento.CHIESTO) {
            int pos = indice(eti, campo, Comportamento.CHIESTO);
            return pos <= 1 ? "Dato di stampa" : "Dato di stampa " + pos;
        }
        int pos = indice(eti, campo, Comportamento.FISSO);
        return pos <= 1 ? "Codice fisso" : "Dato fisso " + pos;
    }

    public static String nomeConTecnico(Etichetta eti, Campo campo) {
        String umano = nome(eti, campo);
        if (campo == null || umano.equalsIgnoreCase(campo.nome())) return umano;
        return umano + "  ·  " + campo.nome();
    }

    public static String uso(Etichetta eti, Campo campo) {
        if (eti == null || campo == null) return "";
        List<String> nomi = new ArrayList<String>();
        for (Elemento e : eti.elementiPerCampo(campo)) {
            nomi.add(e.nome());
        }
        if (nomi.isEmpty()) return "non usato sul layout";
        StringBuilder b = new StringBuilder("usato da ");
        for (int i = 0; i < nomi.size(); i++) {
            if (i > 0) b.append(i == nomi.size() - 1 ? " + " : ", ");
            b.append(nomi.get(i));
        }
        return b.toString();
    }

    public static String tipoUso(Etichetta eti, Campo campo) {
        boolean qr = false, barcode = false, testo = false;
        for (Elemento e : eti.elementiPerCampo(campo)) {
            qr |= e.tipo() == Tipo.QR;
            barcode |= e.tipo() == Tipo.BARCODE;
            testo |= e.tipo() == Tipo.TESTO || e.tipo() == Tipo.CODICE;
        }
        List<String> p = new ArrayList<String>();
        if (qr) p.add("QR");
        if (barcode) p.add("Barcode");
        if (testo) p.add("Testo");
        if (p.isEmpty()) return "Dato";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.size(); i++) {
            if (i > 0) b.append(" + ");
            b.append(p.get(i));
        }
        return b.toString();
    }

    public static String comportamento(Campo c) {
        if (c == null) return "Dato";
        if (c.comportamento() == Comportamento.PROGRESSIVO) return "Progressivo";
        if (c.comportamento() == Comportamento.CHIESTO) return "Chiesto alla stampa";
        return "Fisso";
    }

    private static int indice(Etichetta eti, Campo campo, Comportamento tipo) {
        int n = 0;
        if (eti != null) {
            for (Campo c : eti.campiUsati()) {
                if (c.comportamento() == tipo) {
                    n++;
                    if (c == campo || c.nome().equals(campo.nome())) return n;
                }
            }
        }
        return Math.max(1, n + 1);
    }

    private static boolean significativo(String n) {
        if (n.isEmpty()) return false;
        if (n.matches("(codice|dato|campo)([ _-]*\\d+)?")) return false;
        return true;
    }

    private static String titolo(String s) {
        if (s == null || s.trim().isEmpty()) return "Dato";
        String t = s.trim().replace('_', ' ');
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
