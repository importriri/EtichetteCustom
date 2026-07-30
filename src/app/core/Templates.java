package app.core;

/**
 * Il punto di partenza di un'etichetta nuova.
 *
 * <p>Qui non ci sono più i modelli dei clienti. Le versioni precedenti
 * arrivavano con dentro numeri di disegno e capitolati veri: comodo da
 * mostrare, sbagliato da consegnare — quei dati sono di chi li ha
 * commissionati, e un programma che se li porta dietro non si può dare a
 * nessun altro.
 *
 * <p>Resta una sola cosa: un'etichetta vuota ma già stampabile, da cui
 * l'operatore parte per disegnare la sua. Quando è come la vuole, la salva con
 * {@link LayoutStore} e da lì in poi è nell'elenco dei suoi layout.
 */
public final class Templates {

    /** Il nome dell'etichetta appena creata, finché non viene salvata. */
    public static final String NUOVA = "Nuova etichetta";

    private Templates() {
    }

    /**
     * Riporta il modello al punto di partenza: un supporto da 50 x 30, un campo
     * progressivo e due elementi — il codice scritto e lo stesso codice nel QR.
     *
     * <p>È il minimo che serve perché il primo Stampa funzioni senza che
     * l'operatore debba capire niente. Tutto il resto lo aggiunge lui.
     */
    public static void reset(LabelModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Modello nullo.");
        }
        model.elements().clear();
        model.fields().clear();
        model.setSizeMm(50, 30);
        model.setDpi(203);
        model.setEcc(QrCode.Ecc.QUARTILE);
        model.setMinQrSideMm(0);
        model.setModuleWarnMm(LabelModel.DEFAULT_MODULE_WARN_MM);
        model.setTemplateName(NUOVA);

        model.addField(LabelField.sequential(LabelField.DEFAULT_NAME, "0001", 4));

        LabelElement code = LabelElement.text("Codice", LabelElement.CODE_TOKEN, 3.0, 5.5, 3.2);
        code.setBold(true);
        model.add(code);
        model.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN, 3.0, 8.5, 18.0));
    }
}
