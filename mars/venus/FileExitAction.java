package mars.venus;

import mars.Globals;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.KeyStroke;

public class FileExitAction extends GuiAction {
   public FileExitAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      if (this.mainUI.editor.closeAll()) {
         System.exit(0);
      }

   }
}
