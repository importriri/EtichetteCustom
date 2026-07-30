package app.core;

/**
 * Manual test suite for {@link QrCode}. No JUnit; exits with status 1 on the
 * first failed run.
 *
 * <p>The two frozen matrices were originally verified with two independent
 * decoders (zxing-cpp and zbar). If an encoder change moves either matrix, the
 * reason must be understood before any label reaches a scanner.
 */
public final class QrCodeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        golden_alphanumericLabelCode_matchesFrozenMatrix();
        golden_utf8Accents_matchFrozenMatrix();
        structure_findersTimingDarkModule_wherePrescribed();
        structure_quietZone_readsLight();
        mode_pickedFromContent();
        version_growsWithContent_atExactBoundaries();
        utf8_accentedChars_countAsTwoBytes();
        masks_allEight_produceDistinctMatrices();
        encode_sameInput_isDeterministic();
        encode_invalidInput_isRejected();

        System.out.println("QrCode: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- frozen vectors -------------------------------------------------------

    private static final String[] GOLDEN_LABEL = {
            "111111101110001111111",
            "100000100101001000001",
            "101110100100001011101",
            "101110101000001011101",
            "101110101011101011101",
            "100000101110101000001",
            "111111101010101111111",
            "000000001000000000000",
            "100010111100111111001",
            "001110000100111101100",
            "101000100000101011001",
            "101100010001110101011",
            "100010101101001001010",
            "000000001000101100100",
            "111111101011010011001",
            "100000100101100001000",
            "101110101010111100110",
            "101110100110110000111",
            "101110100011000111000",
            "100000100111000111001",
            "111111101010111000111",
    };

    private static final String[] GOLDEN_UTF8 = {
            "111111100111001111111",
            "100000100010001000001",
            "101110101111101011101",
            "101110101001001011101",
            "101110101100101011101",
            "100000101001001000001",
            "111111101010101111111",
            "000000001100000000000",
            "101111100101001111100",
            "100011011011111001101",
            "000000101000101101110",
            "101110010011111001100",
            "100101100000100101000",
            "000000001000100100111",
            "111111100101010010010",
            "100000101100000111010",
            "101110101111010010110",
            "101110101111111100100",
            "101110101110101101000",
            "100000100001111110000",
            "111111101010100101010",
    };

    private static void golden_alphanumericLabelCode_matchesFrozenMatrix() {
        QrCode q = QrCode.encode("LAB-0000-TEST-001", QrCode.Ecc.MEDIUM, 4);
        yes("synthetic label code uses version 1", q.version == 1);
        yes("synthetic label code uses alphanumeric mode",
                q.mode == QrCode.Mode.ALPHANUMERIC);
        sameMatrix("frozen LAB-0000-TEST-001 matrix", GOLDEN_LABEL, q);
    }

    private static void golden_utf8Accents_matchFrozenMatrix() {
        QrCode q = QrCode.encode("però", QrCode.Ecc.MEDIUM, 2);
        yes("accented text uses byte mode", q.mode == QrCode.Mode.BYTE);
        sameMatrix("frozen UTF-8 accent matrix", GOLDEN_UTF8, q);
    }

    // --- structure ------------------------------------------------------------

    private static void structure_findersTimingDarkModule_wherePrescribed() {
        QrCode q = QrCode.encode("X1", QrCode.Ecc.MEDIUM);
        int n = q.size;
        yes("side length equals version*4+17", n == q.version * 4 + 17);
        yes("top-left finder has a dark corner", q.module(0, 0));
        yes("top-left finder has a light ring", !q.module(1, 1));
        yes("top-left finder has a dark centre", q.module(3, 3));
        yes("top-right finder exists", q.module(n - 1, 0));
        yes("bottom-left finder exists", q.module(0, n - 1));
        yes("fixed dark module is at (8, side-8)", q.module(8, n - 8));
        yes("horizontal timing alternates",
                q.module(8, 6) && !q.module(9, 6) && q.module(10, 6));
        yes("vertical timing alternates",
                q.module(6, 8) && !q.module(6, 9) && q.module(6, 10));
    }

    private static void structure_quietZone_readsLight() {
        QrCode q = QrCode.encode("X1", QrCode.Ecc.LOW);
        yes("outside the matrix reads light", !q.module(-1, 0) && !q.module(0, -1)
                && !q.module(q.size, 0) && !q.module(0, q.size));
    }

    // --- mode and version selection ------------------------------------------

    private static void mode_pickedFromContent() {
        yes("digits select numeric mode",
                QrCode.encode("00042", QrCode.Ecc.MEDIUM).mode == QrCode.Mode.NUMERIC);
        yes("uppercase letters and dashes select alphanumeric mode",
                QrCode.encode("LAB-2026", QrCode.Ecc.MEDIUM).mode
                        == QrCode.Mode.ALPHANUMERIC);
        yes("lowercase letters select byte mode",
                QrCode.encode("lab", QrCode.Ecc.MEDIUM).mode == QrCode.Mode.BYTE);
    }

    private static void version_growsWithContent_atExactBoundaries() {
        // Boundaries come from the MEDIUM error-correction capacity table.
        same("byte: 14 characters fit version 1", 1,
                QrCode.encode(repeat('x', 14), QrCode.Ecc.MEDIUM).version);
        same("byte: character 15 requires version 2", 2,
                QrCode.encode(repeat('x', 15), QrCode.Ecc.MEDIUM).version);
        same("alphanumeric: 20 characters fit version 1", 1,
                QrCode.encode(repeat('A', 20), QrCode.Ecc.MEDIUM).version);
        same("alphanumeric: character 21 requires version 2", 2,
                QrCode.encode(repeat('A', 21), QrCode.Ecc.MEDIUM).version);
        same("numeric: 34 digits fit version 1", 1,
                QrCode.encode(repeat('9', 34), QrCode.Ecc.MEDIUM).version);
        same("numeric: digit 35 requires version 2", 2,
                QrCode.encode(repeat('9', 35), QrCode.Ecc.MEDIUM).version);
        same("300 high-correction characters require version 15", 15,
                QrCode.encode(repeat('Z', 300), QrCode.Ecc.HIGH).version);
    }

    private static void utf8_accentedChars_countAsTwoBytes() {
        // "è" occupies two UTF-8 bytes: seven fit version 1, eight do not.
        same("seven accented characters fit version 1", 1,
                QrCode.encode(repeat('è', 7), QrCode.Ecc.MEDIUM).version);
        same("eight accented characters require version 2", 2,
                QrCode.encode(repeat('è', 8), QrCode.Ecc.MEDIUM).version);
    }

    // --- masks and determinism ------------------------------------------------

    private static void masks_allEight_produceDistinctMatrices() {
        String text = "LAB-0000-TEST-001";
        String[] seen = new String[8];
        boolean allDistinct = true;
        for (int m = 0; m < 8; m++) {
            QrCode q = QrCode.encode(text, QrCode.Ecc.MEDIUM, m);
            seen[m] = flatten(q);
            yes("mask " + m + " is reported by the result", q.mask == m);
            for (int prev = 0; prev < m; prev++) {
                if (seen[prev].equals(seen[m])) {
                    allDistinct = false;
                }
            }
        }
        yes("all eight masks produce distinct matrices", allDistinct);
    }

    private static void encode_sameInput_isDeterministic() {
        String a = flatten(QrCode.encode("LAB-DEMO-QR-000001", QrCode.Ecc.HIGH));
        String b = flatten(QrCode.encode("LAB-DEMO-QR-000001", QrCode.Ecc.HIGH));
        yes("the same input produces the same matrix", a.equals(b));
    }

    private static void encode_invalidInput_isRejected() {
        rejects("null text", new Runnable() {
            public void run() {
                QrCode.encode(null, QrCode.Ecc.MEDIUM);
            }
        });
        rejects("null correction level", new Runnable() {
            public void run() {
                QrCode.encode("X", null);
            }
        });
        rejects("mask 8", new Runnable() {
            public void run() {
                QrCode.encode("X", QrCode.Ecc.MEDIUM, 8);
            }
        });
        rejects("mask -2", new Runnable() {
            public void run() {
                QrCode.encode("X", QrCode.Ecc.MEDIUM, -2);
            }
        });
        rejects("content beyond version 40", new Runnable() {
            public void run() {
                QrCode.encode(repeat('7', 8000), QrCode.Ecc.MEDIUM);
            }
        });
    }

    // --- helpers --------------------------------------------------------------

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String flatten(QrCode q) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < q.size; y++) {
            for (int x = 0; x < q.size; x++) {
                sb.append(q.module(x, y) ? '1' : '0');
            }
        }
        return sb.toString();
    }

    private static void sameMatrix(String what, String[] expected, QrCode q) {
        if (expected.length != q.size) {
            failed++;
            System.out.println("FAIL  " + what + ": expected side " + expected.length
                    + ", got " + q.size);
            return;
        }
        for (int y = 0; y < q.size; y++) {
            for (int x = 0; x < q.size; x++) {
                boolean want = expected[y].charAt(x) == '1';
                if (q.module(x, y) != want) {
                    failed++;
                    System.out.println("FAIL  " + what + ": module (" + x + "," + y
                            + ") differs");
                    return;
                }
            }
        }
        passed++;
        System.out.println("  ok  " + what);
    }

    private static void same(String what, int expected, int actual) {
        if (expected == actual) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what + ": expected " + expected
                    + ", got " + actual);
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

    private static void rejects(String what, Runnable block) {
        try {
            block.run();
            failed++;
            System.out.println("FAIL  " + what + ": expected rejection");
        } catch (IllegalArgumentException e) {
            passed++;
            System.out.println("  ok  " + what + " -> " + e.getMessage());
        }
    }
}
