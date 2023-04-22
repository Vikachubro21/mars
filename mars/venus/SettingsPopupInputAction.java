package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import mars.Globals;

public class SettingsPopupInputAction extends GuiAction {
   public SettingsPopupInputAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      boolean usePopup = ((JCheckBoxMenuItem)e.getSource()).isSelected();
      Globals.getSettings().setBooleanSetting(17, usePopup);
   }
}
