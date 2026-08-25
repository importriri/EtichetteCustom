package app.render;

/**
 * Misura un testo in millimetri. Esiste per poter provare l'andata a capo
 * senza schermo: le prove usano un misuratore finto a passo fisso, l'app
 * usa quello vero costruito sulle metriche del font.
 */
public interface Misuratore {

    /** Larghezza in millimetri del testo, a quel corpo (in mm) e quel peso. */
    double larghezza(String testo, double corpoMm, boolean grassetto);
}
