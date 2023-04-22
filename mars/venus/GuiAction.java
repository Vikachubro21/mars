package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

public class GuiAction extends AbstractAction {
   protected VenusUI mainUI;

   protected GuiAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon);
      this.putValue("ShortDescription", descrip);
      this.putValue("MnemonicKey", mnemonic);
      this.putValue("AcceleratorKey", accel);
      this.mainUI = gui;
   }

   public void actionPerformed(ActionEvent e) {
   }
}
