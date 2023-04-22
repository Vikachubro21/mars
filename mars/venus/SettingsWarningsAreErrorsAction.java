package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import mars.Globals;

public class SettingsWarningsAreErrorsAction extends GuiAction {
   public SettingsWarningsAreErrorsAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      Globals.getSettings().setWarningsAreErrors(((JCheckBoxMenuItem)e.getSource()).isSelected());
   }
}
