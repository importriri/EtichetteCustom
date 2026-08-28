package app.render;

/**
 * Measures text in millimetres. Keeping measurement behind this interface lets
 * wrapping tests run without a display while production uses real font metrics.
 */
public interface Misuratore {

    /** Text width in millimetres for the requested size and weight. */
    double larghezza(String testo, double corpoMm, boolean grassetto);
}
