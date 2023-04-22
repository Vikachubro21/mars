package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import mars.Globals;

public class SettingsAddressDisplayBaseAction extends GuiAction {
   public SettingsAddressDisplayBaseAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      boolean isHex = ((JCheckBoxMenuItem)e.getSource()).isSelected();
      Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBaseChooser().setSelected(isHex);
      Globals.getSettings().setDisplayAddressesInHex(isHex);
   }
}
