package app.modello;

import app.archivio.Formato;
import java.util.ArrayDeque;
import java.util.Deque;

/** Editor undo/redo implemented with serialized model snapshots. */
public class Storia {
    private static final int QUANTE = 40;
    private final Deque<String> pila = new ArrayDeque<String>();
    private final Deque<String> futuro = new ArrayDeque<String>();

    public void segna(Etichetta e) {
        String adesso = Formato.scrivi(e);
        futuro.clear();
        if (adesso.equals(pila.peekLast())) return;
        pila.addLast(adesso);
        limita(pila);
    }

    public boolean qualcosaDaAnnullare() { return !pila.isEmpty(); }
    public boolean qualcosaDaRipetere() { return !futuro.isEmpty(); }

    public boolean annulla(Etichetta e) {
        String adesso = Formato.scrivi(e);
        while (!pila.isEmpty()) {
            String prima = pila.removeLast();
            if (!prima.equals(adesso)) {
                futuro.addLast(adesso);
                limita(futuro);
                e.riprendi(Formato.leggi(prima));
                return true;
            }
        }
        return false;
    }

    public boolean ripeti(Etichetta e) {
        String adesso = Formato.scrivi(e);
        while (!futuro.isEmpty()) {
            String dopo = futuro.removeLast();
            if (!dopo.equals(adesso)) {
                pila.addLast(adesso);
                limita(pila);
                e.riprendi(Formato.leggi(dopo));
                return true;
            }
        }
        return false;
    }

    public void dimentica() { pila.clear(); futuro.clear(); }

    private static void limita(Deque<String> q) {
        while (q.size() > QUANTE) q.removeFirst();
    }
}
