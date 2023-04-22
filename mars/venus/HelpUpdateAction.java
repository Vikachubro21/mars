package mars.venus;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public class HelpUpdateAction extends GuiAction {
   public HelpUpdateAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      try {
         if (JOptionPane.showConfirmDialog(this.mainUI, "Current Version: 0.2 Beta\nCheck for Update?", "Update MARS Plus", 0) == 0) {
            Desktop.getDesktop().browse(new URI("http://www.calebhoff.com/2018/10/mars-plus.html"));
         }
      } catch (IOException var3) {
         System.out.println("ERROR 504: Failed to Open Web Page");
      } catch (URISyntaxException var4) {
         System.out.println("ERROR 503: Failed to Open Web Page");
      }

   }
}
