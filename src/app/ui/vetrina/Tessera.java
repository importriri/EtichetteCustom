package app.ui.vetrina;

import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Etichetta;
import app.modello.Serie;
import app.render.Disegno;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.comp.CodiceView;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/** Gallery card rendered from the same label renderer used for printing. */
public class Tessera extends JComponent {
    public interface Apri { void apri(Etichetta label); }
    public interface Menu { void mostra(Etichetta label, int x, int y); }

    private final Etichetta label;
    private final SorgenteQr qr;
    private final boolean isNew;
    private boolean hover;
    private Apri open;
    private Menu menu;

    public Tessera(Etichetta label, SorgenteQr qr) {
        this(label, qr, false);
    }

    private Tessera(Etichetta label, SorgenteQr qr, boolean isNew) {
        this.label = label;
        this.qr = qr;
        this.isNew = isNew;
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        setToolTipText(isNew ? "Parti da un foglio vuoto"
                : label.nome() + " — " + size(label));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                hover = true;
                repaint();
            }

            @Override public void mouseExited(MouseEvent event) {
                hover = false;
                repaint();
            }

            @Override public void mouseClicked(MouseEvent event) {
                if (Tessera.this.label == null) {
                    if (open != null) open.apri(null);
                    return;
                }
                if (menu != null && (event.isPopupTrigger()
                        || javax.swing.SwingUtilities.isRightMouseButton(event)
                        || overMenu(event))) {
                    menu.mostra(Tessera.this.label, event.getX(), event.getY());
                    return;
                }
                if (open != null) open.apri(Tessera.this.label);
            }
        });
    }

    public static Tessera nuova(Apri open) {
        Tessera card = new Tessera(null, null, true);
        card.open = open;
        return card;
    }

    public Tessera azione(Apri action) {
        open = action;
        return this;
    }

    public Tessera menu(Menu menu) {
        this.menu = menu;
        return this;
    }

    private boolean overMenu(MouseEvent event) {
        int side = Stile.px(30);
        return event.getX() > getWidth() - side && event.getY() < side;
    }

    public Etichetta etichetta() { return label; }

    private static String size(Etichetta label) {
        return num(label.larghezza()) + " × " + num(label.altezza()) + " mm";
    }

    private static String num(double value) {
        String text = String.valueOf(Math.round(value * 10) / 10.0);
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        return text.replace('.', ',');
    }

    @Override public Dimension getPreferredSize() {
        return new Dimension(Stile.px(258), Stile.px(236));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = Stile.liscio(g);
        try {
            int width = getWidth();
            int height = getHeight();
            int radius = Stile.px(12);

            if (isNew) {
                paintNewCard(g2, width, height, radius);
                return;
            }

            g2.setColor(new Color(35, 48, 74, hover ? 24 : 14));
            g2.fillRoundRect(Stile.px(2), Stile.px(3), width - Stile.px(3),
                    height - Stile.px(3), radius, radius);
            Stile.riquadro(g2, 0, 0, width - Stile.px(3), height - Stile.px(4),
                    radius, Color.WHITE, hover ? Stile.BLU : Stile.S0);

            if (hover) paintMenuButton(g2, width);

            int margin = Stile.px(12);
            int previewHeight = Stile.px(138);
            Stile.riquadro(g2, margin, margin, width - 2 * margin - Stile.px(3),
                    previewHeight, Stile.px(8), Stile.BANCO, null);
            paintPaper(g2, margin, margin, width - 2 * margin - Stile.px(3), previewHeight);

            int y = margin + previewHeight + Stile.px(20);
            g2.setFont(Stile.forte());
            g2.setColor(Stile.TESTO);
            g2.drawString(label.nome(), margin, y);

            y += Stile.px(17);
            g2.setFont(Stile.piccolo());
            g2.setColor(Stile.OV1);
            g2.drawString(size(label), margin, y);

            y += Stile.px(22);
            paintState(g2, margin, y);
        } finally {
            g2.dispose();
        }
    }

    private void paintNewCard(Graphics2D g2, int width, int height, int radius) {
        Color border = hover ? Stile.BLU : Stile.S1;
        Color background = hover ? Stile.BLU_SOFT : Color.WHITE;
        Stile.riquadro(g2, 0, 0, width - Stile.px(3), height - Stile.px(4),
                radius, background, border);

        g2.setColor(hover ? Stile.BLU : Stile.SUB0);
        g2.setFont(Stile.font(28, Font.PLAIN));
        centered(g2, "+", width - Stile.px(3), height / 2 - Stile.px(14));
        g2.setFont(Stile.forte());
        centered(g2, "Nuova etichetta", width - Stile.px(3), height / 2 + Stile.px(18));
        g2.setFont(Stile.piccolo());
        g2.setColor(Stile.OV1);
        centered(g2, "parti da un foglio vuoto", width - Stile.px(3), height / 2 + Stile.px(38));
    }

    private void paintMenuButton(Graphics2D g2, int width) {
        int side = Stile.px(24);
        int x = width - side - Stile.px(11);
        int y = Stile.px(10);
        Stile.riquadro(g2, x, y, side, side, Stile.px(6), Color.WHITE, Stile.S1);
        g2.setColor(Stile.SUB0);
        for (int i = 0; i < 3; i++) {
            g2.fillOval(x + Stile.px(5) + i * Stile.px(5), y + side / 2 - 1, 2, 2);
        }
    }

    private void paintState(Graphics2D g2, int x, int baseline) {
        Serie series = label.serie();
        if (series != null) {
            String prefix = series.prefisso();
            if (prefix.length() > 7) prefix = "…" + prefix.substring(prefix.length() - 6);
            Font font = Stile.mono(11);
            g2.setColor(Stile.SUB1);
            g2.setFont(Stile.piccolo());
            g2.drawString("Prossimo", x, baseline);
            int codeX = x + Stile.px(58);
            CodiceView.disegna(g2, codeX, baseline, prefix, series.finestra(series.prossimo()), font);
            return;
        }

        boolean asksAtPrint = false;
        for (Campo field : label.campiUsati()) {
            if (field.comportamento() == Comportamento.CHIESTO) {
                asksAtPrint = true;
                break;
            }
        }
        g2.setFont(Stile.piccolo());
        g2.setColor(Stile.SUB0);
        g2.drawString(asksAtPrint ? "Valori alla stampa" : "Pronta da stampare", x, baseline);
    }

    private void centered(Graphics2D g, String text, int width, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (width - metrics.stringWidth(text)) / 2, baseline);
    }

    private void paintPaper(Graphics2D g, int bx, int by, int bw, int bh) {
        double mmPx = Math.min((bw - Stile.px(16)) / label.larghezza(),
                (bh - Stile.px(16)) / label.altezza());
        int paperWidth = (int) Math.round(label.larghezza() * mmPx);
        int paperHeight = (int) Math.round(label.altezza() * mmPx);
        int x = bx + (bw - paperWidth) / 2;
        int y = by + (bh - paperHeight) / 2;

        g.setColor(new Color(0, 0, 0, 24));
        g.fillRect(x + Stile.px(2), y + Stile.px(3), paperWidth, paperHeight);

        Graphics2D paper = (Graphics2D) g.create(x, y, paperWidth, paperHeight);
        try {
            Disegno.disegna(paper, label, mmPx, qr, 0);
        } finally {
            paper.dispose();
        }
        g.setColor(Stile.S1);
        g.drawRect(x, y, paperWidth - 1, paperHeight - 1);
    }
}
