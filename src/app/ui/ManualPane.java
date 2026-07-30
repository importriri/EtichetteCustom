package app.ui;

import app.config.AppTheme;
import app.config.Manuals;
import app.config.UiScale;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Il manuale, dentro l'applicazione.
 *
 * <p>Il PC di reparto ha una finestra aperta e non è GitHub. Un operatore che
 * per sapere cosa fa il contatore deve uscire dal programma, cercare un
 * indirizzo e trovare la pagina giusta, semplicemente non lo saprà mai.
 *
 * <p>Tutte e due le lingue viaggiano nel JAR; il cambio è un pulsante, e il
 * riquadro tiene una misura sua, così la finestra delle impostazioni non cresce
 * fino alla lunghezza del documento.
 */
public final class ManualPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JEditorPane view = new JEditorPane();
    private final JButton italiano = AppTheme.ghost("Italiano");
    private final JButton english = AppTheme.ghost("English");

    private String language = Manuals.IT;

    public ManualPane() {
        setLayout(new BorderLayout(0, UiScale.px(6)));
        setBackground(AppTheme.BASE);
        setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(10), UiScale.px(12), UiScale.px(10), UiScale.px(12)));

        view.setEditable(false);
        view.setContentType("text/html");
        view.setBackground(AppTheme.LATTE ? AppTheme.PAPER : AppTheme.MANTLE);
        view.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(2), UiScale.px(6), UiScale.px(2), UiScale.px(6)));
        // il carattere dell'app, non quello del motore HTML: il manuale non
        // deve disfare il tema, e su Windows deve seguire la scala dello schermo
        view.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        view.setFont(AppTheme.UI);

        JScrollPane scroll = new JScrollPane(view);
        // un MINIMO, non una pretesa: il riquadro riempie lo spazio che riceve,
        // così il manuale non può essere il motivo per cui la finestra delle
        // impostazioni esce dallo schermo di un portatile
        scroll.setPreferredSize(new Dimension(UiScale.px(520), UiScale.px(300)));
        scroll.setBorder(new AppTheme.RoundBorder(AppTheme.SURFACE1, UiScale.px(8),
                new java.awt.Insets(2, 2, 2, 2)));
        scroll.getViewport().setBackground(AppTheme.LATTE ? AppTheme.PAPER : AppTheme.MANTLE);
        AppTheme.styleScroll(scroll);

        add(buildHeader(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        show(Manuals.IT);
    }

    private JPanel buildHeader() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiScale.px(6), 0));
        row.setOpaque(false);
        italiano.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                show(Manuals.IT);
            }
        });
        english.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                show(Manuals.EN);
            }
        });
        row.add(italiano);
        row.add(english);
        return row;
    }

    private JPanel buildFooter() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiScale.px(6), 0));
        row.setOpaque(false);
        row.add(AppTheme.hint("Lo stesso testo dei file in <b>src/app/docs</b>, "
                + "dentro il programma.", 300));
        row.add(link());
        return row;
    }

    /** L'indirizzo del repository, cliccabile dove il sistema lo permette. */
    private JLabel link() {
        final JLabel label = new JLabel("github.com/importriri/etichette-custom");
        label.setFont(AppTheme.UI_SMALL);
        label.setForeground(AppTheme.MAUVE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setToolTipText(Manuals.REPOSITORY);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                open();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(AppTheme.BLUE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(AppTheme.MAUVE);
            }
        });
        return label;
    }

    private void open() {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(Manuals.REPOSITORY));
                return;
            }
        } catch (Exception noBrowser) {
            // niente browser su questa macchina: l'indirizzo resta scritto qui
            // e nel suggerimento, che è tutto quello che serve per copiarlo
        }
        MainWindow.status(this, "Indirizzo del repository: " + Manuals.REPOSITORY, 0);
    }

    /** Mostra il manuale nella lingua indicata. */
    public void show(String lang) {
        language = Manuals.EN.equalsIgnoreCase(lang) ? Manuals.EN : Manuals.IT;
        italiano.setEnabled(!Manuals.IT.equals(language));
        english.setEnabled(!Manuals.EN.equals(language));
        view.setText(page(Markdown.toHtml(Manuals.text(language))));
        view.setCaretPosition(0);
    }

    /** La lingua mostrata adesso. */
    public String language() {
        return language;
    }

    /** Solo i colori: famiglia e corpo arrivano dal carattere del componente. */
    private static String page(String body) {
        String text = hex(AppTheme.TEXT);
        String head = hex(AppTheme.MAUVE);
        String rule = hex(AppTheme.SURFACE1);
        String code = hex(AppTheme.PEACH);
        return "<html><head><style>"
                + "body { color: " + text + "; margin: 4px 6px 8px 6px; }"
                + "h1 { color: " + head + "; font-size: 15pt; }"
                + "h2 { color: " + head + "; font-size: 13pt; }"
                + "h3 { color: " + head + "; font-size: 12pt; }"
                + "p, li, td, th { color: " + text + "; }"
                + "tt { color: " + code + "; }"
                + "hr { color: " + rule + "; }"
                + "table { border-color: " + rule + "; }"
                + "</style></head><body>" + body + "</body></html>";
    }

    private static String hex(Color c) {
        return String.format("#%06x", c.getRGB() & 0xFFFFFF);
    }
}
