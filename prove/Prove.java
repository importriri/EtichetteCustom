package prove;

/** Minimal test harness with no external dependency. */
public final class Prove {

    private static int run;
    private static int failed;
    private static String suite = "";

    private Prove() { }

    public static void suite(String name) {
        suite = name;
        System.out.println("\n== " + name + " ==");
    }

    public static void vero(String description, boolean condition) {
        run++;
        if (condition) {
            System.out.println("  ok   " + description);
        } else {
            failed++;
            System.out.println("  FAIL " + description + "   [" + suite + "]");
        }
    }

    public static void uguale(String description, Object expected, Object actual) {
        vero(description + "  (expected " + expected + ", got " + actual + ")",
                expected == null ? actual == null : expected.equals(actual));
    }

    public static void vicino(String description, double expected, double actual, double tolerance) {
        vero(description + "  (expected " + rounded(expected) + " +/- " + tolerance
                + ", got " + rounded(actual) + ")", Math.abs(expected - actual) <= tolerance);
    }

    public static void esplode(String description, Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            vero(description + "  (nothing was thrown)", false);
        } catch (Throwable throwable) {
            vero(description + "  (" + throwable.getClass().getSimpleName() + ")",
                    type.isInstance(throwable));
        }
    }

    private static String rounded(double value) {
        return String.valueOf(Math.round(value * 1000) / 1000.0);
    }

    public static int conclusione() {
        System.out.println("\n--------------------------------------------");
        System.out.println(run + " checks, " + failed + " failed");
        return failed == 0 ? 0 : 1;
    }
}
