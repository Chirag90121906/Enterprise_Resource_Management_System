import gui.MainDashboard;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--web".equals(args[0])) {
            WebServer.start();
            return;
        }

        SwingUtilities.invokeLater(() -> new MainDashboard().setVisible(true));
    }
}