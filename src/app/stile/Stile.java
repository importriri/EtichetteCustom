package app.stile;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** Shared application styling; color is reserved for status and action meaning. */
public final class Stile {
    private Stile() { }

    public static final Color CRUST  = new Color(0xD9DEE8);
    public static final Color MANTLE = new Color(0xE8ECF3);
    public static final Color BASE   = new Color(0xF4F6FA);
    public static final Color S0     = new Color(0xD2D8E3);
    public static final Color S1     = new Color(0xBEC7D5);

    public static final Color TESTO = new Color(0x283246);
    public static final Color SUB1  = new Color(0x4B5870);
    public static final Color SUB0  = new Color(0x66738A);
    public static final Color OV1   = new Color(0x8792A6);
    public static final Color OV0   = new Color(0x9CA7B8);

    public static final Color BLU     = new Color(0x2563EB);
    public static final Color LAVANDA = new Color(0x665CF6);
    public static final Color VERDE   = new Color(0x16803C);
    public static final Color ROSSO   = new Color(0xC93648);
    public static final Color PESCA   = new Color(0xE66A25);
    public static final Color CELESTE = new Color(0x0F8FA8);

    public static final Color VERDE_SOFT = new Color(0xE7F6EC);
    public static final Color VERDE_BORDO = new Color(0xA7D9B6);
    public static final Color BLU_SOFT = new Color(0xEAF1FF);
    public static final Color PESCA_SOFT = new Color(0xFFF1E8);
    public static final Color LAVANDA_SOFT = new Color(0xF0EEFF);

    public static final Color BANCO = new Color(0xDDE2EA);
    public static final Color CARTA = Color.WHITE;
    public static final Color INCHIOSTRO = new Color(0x111827);

    private static float base = 12f;
    private static String famiglia = Font.SANS_SERIF;

    public static void adottaFontDiSistema() {
        Font f = UIManager.getFont("Label.font");
        if (f != null) {
            if (f.getSize2D() >= 8f && f.getSize2D() <= 40f) base = f.getSize2D();
            famiglia = f.getFamily();
        }
        configuraSwing();
    }

    private static void configuraSwing() {
        Font n = normale();
        Font p = piccolo();
        Border field = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(S1),
                BorderFactory.createEmptyBorder(px(5), px(8), px(5), px(8)));
        UIManager.put("Panel.background", BASE);
        UIManager.put("Label.foreground", TESTO);
        UIManager.put("Label.font", n);
        UIManager.put("TextField.font", n);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", TESTO);
        UIManager.put("TextField.caretForeground", BLU);
        UIManager.put("TextField.selectionBackground", new Color(0xCFE0FF));
        UIManager.put("TextField.border", field);
        UIManager.put("FormattedTextField.font", n);
        UIManager.put("FormattedTextField.background", Color.WHITE);
        UIManager.put("FormattedTextField.border", field);
        UIManager.put("ComboBox.font", n);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", TESTO);
        UIManager.put("Spinner.font", n);
        UIManager.put("Spinner.background", Color.WHITE);
        UIManager.put("CheckBox.font", n);
        UIManager.put("CheckBox.foreground", TESTO);
        UIManager.put("TabbedPane.font", forte());
        UIManager.put("TabbedPane.background", BASE);
        UIManager.put("TabbedPane.foreground", SUB1);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.width", Integer.valueOf(px(12)));
        UIManager.put("ToolTip.font", p);
        UIManager.put("ToolTip.background", TESTO);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(px(5), px(7), px(5), px(7)));
        UIManager.put("OptionPane.messageFont", n);
        UIManager.put("OptionPane.buttonFont", n);
    }

    public static float corpoBase() { return base; }
    public static int px(double a12) { return (int) Math.round(a12 * base / 12.0); }
    public static Font font(double corpo, int stile) {
        return new Font(famiglia, stile, Math.max(1, (int) Math.round(corpo * base / 12.0)));
    }
    public static Font minuscolo() { return font(10.5, Font.PLAIN); }
    public static Font piccolo()   { return font(11.5, Font.PLAIN); }
    public static Font normale()   { return font(13.0, Font.PLAIN); }
    public static Font forte()     { return font(13.0, Font.BOLD); }
    public static Font titolo()    { return font(17.0, Font.BOLD); }
    public static Font mono(double corpo) {
        return new Font(Font.MONOSPACED, Font.PLAIN, Math.max(1, (int) Math.round(corpo * base / 12.0)));
    }

    public static Color tinta(Color c, double versoBianco) {
        double k = Math.max(0, Math.min(1, versoBianco));
        return new Color((int) Math.round(c.getRed() + (255 - c.getRed()) * k),
                (int) Math.round(c.getGreen() + (255 - c.getGreen()) * k),
                (int) Math.round(c.getBlue() + (255 - c.getBlue()) * k));
    }

    public static Graphics2D liscio(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    public static void riquadro(Graphics2D g, int x, int y, int w, int h, int raggio,
                                Color dentro, Color bordo) {
        if (dentro != null) { g.setColor(dentro); g.fillRoundRect(x, y, w, h, raggio, raggio); }
        if (bordo != null) { g.setColor(bordo); g.drawRoundRect(x, y, w - 1, h - 1, raggio, raggio); }
    }
}
