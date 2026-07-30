package app.core;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encoder QR scritto in casa, zero dipendenze esterne.
 *
 * <p>Copre le versioni da 1 a 40, i quattro livelli di correzione e le tre
 * modalità utili qui: numerica, alfanumerica e byte. La modalità viene scelta da
 * sola sul contenuto: per un codice come {@code TST-0000-00-001} la modalità
 * alfanumerica sta in versione 1 dove quella byte avrebbe già bisogno della 2,
 * e su un'etichetta piccola meno moduli vuol dire moduli più grandi, quindi
 * lettura più facile.
 *
 * <p>L'oggetto è immutabile. La matrice si legge con {@link #module(int, int)},
 * dove {@code true} è un modulo scuro.
 */
public final class QrCode {

    /** Livello di correzione d'errore. */
    public enum Ecc {
        /** ~7% recuperabile. */
        LOW(1),
        /** ~15% recuperabile. Default sensato per un'etichetta. */
        MEDIUM(0),
        /** ~25% recuperabile. */
        QUARTILE(3),
        /** ~30% recuperabile: da usare se l'etichetta si sporca. */
        HIGH(2);

        final int formatBits;

        Ecc(int formatBits) {
            this.formatBits = formatBits;
        }
    }

    private static final String ALPHANUMERIC =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

    private static final int MIN_VERSION = 1;
    private static final int MAX_VERSION = 40;

    private static final int[][] ECC_CODEWORDS_PER_BLOCK = {
        // versione: 0 non usata, poi 1..40
        {-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
        {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28},
        {-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
        {-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
    };

    private static final int[][] NUM_ECC_BLOCKS = {
        {-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25},
        {-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49},
        {-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68},
        {-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81},
    };

    /** Modalità di codifica scelta per il contenuto. */
    public enum Mode {
        NUMERIC(0x1, 10, 12, 14),
        ALPHANUMERIC(0x2, 9, 11, 13),
        BYTE(0x4, 8, 16, 16);

        final int indicator;
        private final int[] countBits;

        Mode(int indicator, int a, int b, int c) {
            this.indicator = indicator;
            this.countBits = new int[] {a, b, c};
        }

        int countBits(int version) {
            return countBits[(version + 7) / 17];
        }
    }

    public final int version;
    public final int size;
    public final Ecc ecc;
    public final int mask;
    public final Mode mode;

    private final boolean[][] modules;
    private final boolean[][] isFunction;

    // --- API ------------------------------------------------------------------

    /** Codifica il testo al livello di correzione indicato, scegliendo da sé modalità e versione. */
    public static QrCode encode(String text, Ecc ecc) {
        return encode(text, ecc, -1);
    }

    /**
     * Come {@link #encode(String, Ecc)}, ma con la maschera forzata.
     *
     * @param forcedMask da 0 a 7, oppure -1 per lasciarla scegliere al punteggio di penalità
     */
    public static QrCode encode(String text, Ecc ecc, int forcedMask) {
        if (text == null) {
            throw new IllegalArgumentException("Testo del QR nullo.");
        }
        if (ecc == null) {
            throw new IllegalArgumentException("Livello di correzione nullo.");
        }
        if (forcedMask < -1 || forcedMask > 7) {
            throw new IllegalArgumentException("Maschera fuori range: " + forcedMask + ".");
        }
        Mode mode = chooseMode(text);
        int version = chooseVersion(text, mode, ecc);
        BitBuffer bb = new BitBuffer();
        bb.append(mode.indicator, 4);
        bb.append(charCount(text, mode), mode.countBits(version));
        appendPayload(bb, text, mode);

        int capacityBits = numDataCodewords(version, ecc) * 8;
        bb.append(0, Math.min(4, capacityBits - bb.size()));
        bb.append(0, (8 - bb.size() % 8) % 8);
        for (int pad = 0xEC; bb.size() < capacityBits; pad ^= 0xEC ^ 0x11) {
            bb.append(pad, 8);
        }
        return new QrCode(version, ecc, mode, addEccAndInterleave(bb.toBytes(), version, ecc), forcedMask);
    }

    /** {@code true} se il modulo è scuro. Fuori dai bordi risponde {@code false} (zona di quiete). */
    public boolean module(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size && modules[y][x];
    }

    /** Il lato della matrice in moduli, zona di quiete esclusa. */
    public int size() {
        return size;
    }

    // --- costruzione ----------------------------------------------------------

    private QrCode(int version, Ecc ecc, Mode mode, byte[] codewords, int forcedMask) {
        this.version = version;
        this.ecc = ecc;
        this.mode = mode;
        this.size = version * 4 + 17;
        this.modules = new boolean[size][size];
        this.isFunction = new boolean[size][size];

        drawFunctionPatterns();
        drawCodewords(codewords);

        int chosen = forcedMask;
        if (chosen == -1) {
            int minPenalty = Integer.MAX_VALUE;
            for (int i = 0; i < 8; i++) {
                applyMask(i);
                drawFormatBits(i);
                int penalty = penaltyScore();
                if (penalty < minPenalty) {
                    minPenalty = penalty;
                    chosen = i;
                }
                applyMask(i); // di nuovo: XOR è la sua stessa inversa
            }
        }
        this.mask = chosen;
        applyMask(chosen);
        drawFormatBits(chosen);
    }

    private void drawFunctionPatterns() {
        for (int i = 0; i < size; i++) {
            setFunction(6, i, i % 2 == 0);
            setFunction(i, 6, i % 2 == 0);
        }
        drawFinder(3, 3);
        drawFinder(size - 4, 3);
        drawFinder(3, size - 4);

        int[] align = alignmentPositions();
        for (int i = 0; i < align.length; i++) {
            for (int j = 0; j < align.length; j++) {
                boolean corner = (i == 0 && j == 0)
                        || (i == 0 && j == align.length - 1)
                        || (i == align.length - 1 && j == 0);
                if (!corner) {
                    drawAlignment(align[i], align[j]);
                }
            }
        }
        drawFormatBits(0);
        drawVersionBits();
    }

    private void drawFinder(int x, int y) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                int xx = x + dx;
                int yy = y + dy;
                if (xx >= 0 && xx < size && yy >= 0 && yy < size) {
                    setFunction(xx, yy, dist != 2 && dist != 4);
                }
            }
        }
    }

    private void drawAlignment(int x, int y) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                setFunction(x + dx, y + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    private void drawFormatBits(int maskIndex) {
        int data = ecc.formatBits << 3 | maskIndex;
        int rem = data;
        for (int i = 0; i < 10; i++) {
            rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        }
        int bits = ((data << 10) | rem) ^ 0x5412;

        for (int i = 0; i <= 5; i++) {
            setFunction(8, i, bit(bits, i));
        }
        setFunction(8, 7, bit(bits, 6));
        setFunction(8, 8, bit(bits, 7));
        setFunction(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) {
            setFunction(14 - i, 8, bit(bits, i));
        }
        for (int i = 0; i < 8; i++) {
            setFunction(size - 1 - i, 8, bit(bits, i));
        }
        for (int i = 8; i < 15; i++) {
            setFunction(8, size - 15 + i, bit(bits, i));
        }
        setFunction(8, size - 8, true); // modulo scuro fisso
    }

    private void drawVersionBits() {
        if (version < 7) {
            return;
        }
        int rem = version;
        for (int i = 0; i < 12; i++) {
            rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
        }
        int bits = version << 12 | rem;
        for (int i = 0; i < 18; i++) {
            boolean b = bit(bits, i);
            int a = size - 11 + i % 3;
            int c = i / 3;
            setFunction(a, c, b);
            setFunction(c, a, b);
        }
    }

    private void drawCodewords(byte[] data) {
        int i = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }
            for (int vert = 0; vert < size; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - vert : vert;
                    if (!isFunction[y][x] && i < data.length * 8) {
                        modules[y][x] = bit(data[i >>> 3] & 0xFF, 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    private void applyMask(int maskIndex) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (isFunction[y][x]) {
                    continue;
                }
                boolean invert;
                switch (maskIndex) {
                    case 0: invert = (x + y) % 2 == 0; break;
                    case 1: invert = y % 2 == 0; break;
                    case 2: invert = x % 3 == 0; break;
                    case 3: invert = (x + y) % 3 == 0; break;
                    case 4: invert = (x / 3 + y / 2) % 2 == 0; break;
                    case 5: invert = x * y % 2 + x * y % 3 == 0; break;
                    case 6: invert = (x * y % 2 + x * y % 3) % 2 == 0; break;
                    case 7: invert = ((x + y) % 2 + x * y % 3) % 2 == 0; break;
                    default: throw new IllegalArgumentException("Maschera " + maskIndex + ".");
                }
                modules[y][x] ^= invert;
            }
        }
    }

    // --- punteggio di penalità (scelta della maschera) -------------------------

    private int penaltyScore() {
        int result = 0;

        for (int y = 0; y < size; y++) {
            boolean color = modules[y][0];
            int run = 1;
            for (int x = 1; x < size; x++) {
                if (modules[y][x] == color) {
                    run++;
                } else {
                    result += runPenalty(run);
                    color = modules[y][x];
                    run = 1;
                }
            }
            result += runPenalty(run);
        }
        for (int x = 0; x < size; x++) {
            boolean color = modules[0][x];
            int run = 1;
            for (int y = 1; y < size; y++) {
                if (modules[y][x] == color) {
                    run++;
                } else {
                    result += runPenalty(run);
                    color = modules[y][x];
                    run = 1;
                }
            }
            result += runPenalty(run);
        }

        for (int y = 0; y < size - 1; y++) {
            for (int x = 0; x < size - 1; x++) {
                boolean c = modules[y][x];
                if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) {
                    result += 3;
                }
            }
        }

        boolean[] pattern = {true, false, true, true, true, false, true, false, false, false, false};
        for (int y = 0; y < size; y++) {
            for (int x = 0; x + 11 <= size; x++) {
                if (matches(pattern, x, y, true, false) || matches(pattern, x, y, true, true)) {
                    result += 40;
                }
            }
        }
        for (int x = 0; x < size; x++) {
            for (int y = 0; y + 11 <= size; y++) {
                if (matches(pattern, x, y, false, false) || matches(pattern, x, y, false, true)) {
                    result += 40;
                }
            }
        }

        int dark = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (modules[y][x]) {
                    dark++;
                }
            }
        }
        int total = size * size;
        int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total; // ceil(|perc-50|/5)
        result += k * 10;
        return result;
    }

    private static int runPenalty(int run) {
        return run >= 5 ? 3 + (run - 5) : 0;
    }

    private boolean matches(boolean[] pattern, int x, int y, boolean horizontal, boolean reversed) {
        for (int i = 0; i < pattern.length; i++) {
            int idx = reversed ? pattern.length - 1 - i : i;
            boolean cell = horizontal ? modules[y][x + i] : modules[y + i][x];
            if (cell != pattern[idx]) {
                return false;
            }
        }
        return true;
    }

    // --- codifica dei dati ----------------------------------------------------

    private static Mode chooseMode(String text) {
        boolean numeric = !text.isEmpty();
        boolean alnum = !text.isEmpty();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                numeric = false;
            }
            if (ALPHANUMERIC.indexOf(c) < 0) {
                alnum = false;
            }
        }
        if (numeric) {
            return Mode.NUMERIC;
        }
        return alnum ? Mode.ALPHANUMERIC : Mode.BYTE;
    }

    private static int charCount(String text, Mode mode) {
        return mode == Mode.BYTE ? utf8(text).length : text.length();
    }

    private static void appendPayload(BitBuffer bb, String text, Mode mode) {
        switch (mode) {
            case NUMERIC:
                for (int i = 0; i < text.length(); ) {
                    int n = Math.min(3, text.length() - i);
                    bb.append(Integer.parseInt(text.substring(i, i + n)), n * 3 + 1);
                    i += n;
                }
                break;
            case ALPHANUMERIC:
                for (int i = 0; i + 1 < text.length(); i += 2) {
                    int v = ALPHANUMERIC.indexOf(text.charAt(i)) * 45
                            + ALPHANUMERIC.indexOf(text.charAt(i + 1));
                    bb.append(v, 11);
                }
                if (text.length() % 2 == 1) {
                    bb.append(ALPHANUMERIC.indexOf(text.charAt(text.length() - 1)), 6);
                }
                break;
            default:
                for (byte b : utf8(text)) {
                    bb.append(b & 0xFF, 8);
                }
                break;
        }
    }

    private static int payloadBits(String text, Mode mode) {
        switch (mode) {
            case NUMERIC:
                return 10 * (text.length() / 3) + new int[] {0, 4, 7}[text.length() % 3];
            case ALPHANUMERIC:
                return 11 * (text.length() / 2) + 6 * (text.length() % 2);
            default:
                return 8 * utf8(text).length;
        }
    }

    private static int chooseVersion(String text, Mode mode, Ecc ecc) {
        int bits = payloadBits(text, mode);
        for (int v = MIN_VERSION; v <= MAX_VERSION; v++) {
            int capacity = numDataCodewords(v, ecc) * 8;
            if (4 + mode.countBits(v) + bits <= capacity) {
                return v;
            }
        }
        throw new IllegalArgumentException(
                "Il contenuto è troppo lungo per un QR: " + text.length() + " caratteri.");
    }

    private static byte[] utf8(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    // --- correzione d'errore --------------------------------------------------

    private static byte[] addEccAndInterleave(byte[] data, int version, Ecc ecc) {
        int numBlocks = NUM_ECC_BLOCKS[ecc.ordinal()][version];
        int eccLen = ECC_CODEWORDS_PER_BLOCK[ecc.ordinal()][version];
        int rawCodewords = numRawDataModules(version) / 8;
        int numShort = numBlocks - rawCodewords % numBlocks;
        int shortLen = rawCodewords / numBlocks;
        int shortDataLen = shortLen - eccLen;

        byte[] divisor = rsDivisor(eccLen);
        byte[][] dataBlocks = new byte[numBlocks][];
        byte[][] eccBlocks = new byte[numBlocks][];
        int k = 0;
        for (int i = 0; i < numBlocks; i++) {
            int len = shortDataLen + (i < numShort ? 0 : 1);
            dataBlocks[i] = Arrays.copyOfRange(data, k, k + len);
            k += len;
            eccBlocks[i] = rsRemainder(dataBlocks[i], divisor);
        }

        byte[] result = new byte[rawCodewords];
        int idx = 0;
        for (int col = 0; col <= shortDataLen; col++) {
            for (int b = 0; b < numBlocks; b++) {
                if (col < dataBlocks[b].length) {
                    result[idx++] = dataBlocks[b][col];
                }
            }
        }
        for (int col = 0; col < eccLen; col++) {
            for (int b = 0; b < numBlocks; b++) {
                result[idx++] = eccBlocks[b][col];
            }
        }
        return result;
    }

    private static byte[] rsDivisor(int degree) {
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < result.length; j++) {
                result[j] = (byte) gfMul(result[j] & 0xFF, root);
                if (j + 1 < result.length) {
                    result[j] ^= result[j + 1];
                }
            }
            root = gfMul(root, 0x02);
        }
        return result;
    }

    private static byte[] rsRemainder(byte[] data, byte[] divisor) {
        byte[] result = new byte[divisor.length];
        for (byte b : data) {
            int factor = (b ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++) {
                result[i] ^= (byte) gfMul(divisor[i] & 0xFF, factor);
            }
        }
        return result;
    }

    private static int gfMul(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z & 0xFF;
    }

    // --- geometria ------------------------------------------------------------

    private int[] alignmentPositions() {
        if (version == 1) {
            return new int[0];
        }
        int num = version / 7 + 2;
        int step = (version == 32) ? 26
                : (version * 4 + num * 2 + 1) / (num * 2 - 2) * 2;
        List<Integer> pos = new ArrayList<Integer>();
        for (int i = 0, p = size - 7; i < num - 1; i++, p -= step) {
            pos.add(0, p);
        }
        pos.add(0, 6);
        int[] result = new int[pos.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = pos.get(i);
        }
        return result;
    }

    private static int numRawDataModules(int version) {
        int result = (16 * version + 128) * version + 64;
        if (version >= 2) {
            int num = version / 7 + 2;
            result -= (25 * num - 10) * num - 55;
            if (version >= 7) {
                result -= 36;
            }
        }
        return result;
    }

    private static int numDataCodewords(int version, Ecc ecc) {
        return numRawDataModules(version) / 8
                - ECC_CODEWORDS_PER_BLOCK[ecc.ordinal()][version]
                * NUM_ECC_BLOCKS[ecc.ordinal()][version];
    }

    private void setFunction(int x, int y, boolean dark) {
        modules[y][x] = dark;
        isFunction[y][x] = true;
    }

    private static boolean bit(int value, int index) {
        return ((value >>> index) & 1) != 0;
    }

    /** Accumulatore di bit, giusto quel tanto che serve. */
    private static final class BitBuffer {
        private final List<Boolean> bits = new ArrayList<Boolean>();

        void append(int value, int length) {
            if (length < 0 || length > 31) {
                throw new IllegalArgumentException("Lunghezza in bit: " + length + ".");
            }
            for (int i = length - 1; i >= 0; i--) {
                bits.add(bit(value, i));
            }
        }

        int size() {
            return bits.size();
        }

        byte[] toBytes() {
            byte[] result = new byte[(bits.size() + 7) / 8];
            for (int i = 0; i < bits.size(); i++) {
                if (bits.get(i)) {
                    result[i >>> 3] |= (byte) (1 << (7 - (i & 7)));
                }
            }
            return result;
        }
    }
}
