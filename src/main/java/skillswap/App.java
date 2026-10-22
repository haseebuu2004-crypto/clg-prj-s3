package skillswap;

import skillswap.service.SkillSwapService;
import skillswap.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.io.IOException;

public final class App {
    private App() { }
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Skill Swap needs a graphical desktop. Run run.bat on a Windows desktop with Java 17 or newer.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                SkillSwapService service = new SkillSwapService();
                new MainFrame(service).setVisible(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Skill Swap could not open its saved data. Your file was left untouched. Check the local storage folder or restore a known-good copy before trying again.",
                        "Unable to start Skill Swap", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Skill Swap could not start.\n\n" + ex.getMessage(), "Unable to start Skill Swap", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
