package prove;

/** Il minimo indispensabile per provare senza tirarsi dentro una libreria. */
public final class Prove {

    private static int fatte;
    private static int fallite;
    private static String suite = "";

    private Prove() { }

    public static void suite(String nome) {
        suite = nome;
        System.out.println("\n== " + nome + " ==");
    }

    public static void vero(String cosa, boolean condizione) {
        fatte++;
        if (condizione) {
            System.out.println("  ok   " + cosa);
        } else {
            fallite++;
            System.out.println("  NO   " + cosa + "   [" + suite + "]");
        }
    }

    public static void uguale(String cosa, Object atteso, Object avuto) {
        vero(cosa + "  (atteso " + atteso + ", avuto " + avuto + ")",
                atteso == null ? avuto == null : atteso.equals(avuto));
    }

    public static void vicino(String cosa, double atteso, double avuto, double tolleranza) {
        vero(cosa + "  (atteso " + arr(atteso) + " +/- " + tolleranza
                + ", avuto " + arr(avuto) + ")", Math.abs(atteso - avuto) <= tolleranza);
    }

    public static void esplode(String cosa, Class<? extends Throwable> tipo, Runnable r) {
        try {
            r.run();
            vero(cosa + "  (non ha sollevato niente)", false);
        } catch (Throwable t) {
            vero(cosa + "  (" + t.getClass().getSimpleName() + ")", tipo.isInstance(t));
        }
    }

    private static String arr(double v) {
        return String.valueOf(Math.round(v * 1000) / 1000.0);
    }

    public static int conclusione() {
        System.out.println("\n--------------------------------------------");
        System.out.println(fatte + " controlli, " + fallite + " falliti");
        return fallite == 0 ? 0 : 1;
    }
}
