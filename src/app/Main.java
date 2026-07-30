package app;

import app.config.AppTheme;
import app.ui.MainWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto di ingresso. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception keepDefault) {
                    // il look and feel di sistema non c'è: si va con quello di default
                }
                // le superfici condivise (tab, pannelli, finestre di dialogo) non
                // passano dalle nostre fabbriche: si tingono qui, altrimenti su
                // Windows restano grigio sistema in mezzo al resto
                UIManager.put("Panel.background", AppTheme.BASE);
                UIManager.put("TabbedPane.background", AppTheme.BASE);
                UIManager.put("TabbedPane.contentAreaColor", AppTheme.BASE);
                UIManager.put("TabbedPane.foreground", AppTheme.TEXT);
                UIManager.put("TabbedPane.selected", AppTheme.BASE);
                UIManager.put("ToolTip.background", AppTheme.SURFACE0);
                UIManager.put("ToolTip.foreground", AppTheme.TEXT);
                UIManager.put("OptionPane.background", AppTheme.BASE);
                UIManager.put("OptionPane.messageForeground", AppTheme.TEXT);
                UIManager.put("Label.foreground", AppTheme.TEXT);
                try {
                    new MainWindow().setVisible(true);
                } catch (RuntimeException e) {
                    JOptionPane.showMessageDialog(null,
                            "Avvio non riuscito: " + e.getMessage(),
                            "Etichette Custom", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
