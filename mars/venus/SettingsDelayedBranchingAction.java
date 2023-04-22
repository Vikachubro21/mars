package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import mars.Globals;
import mars.simulator.Simulator;

public class SettingsDelayedBranchingAction extends GuiAction {
   public SettingsDelayedBranchingAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      Globals.getSettings().setDelayedBranchingEnabled(((JCheckBoxMenuItem)e.getSource()).isSelected());
      if (Globals.getGui() != null && (FileStatus.get() == 5 || FileStatus.get() == 6 || FileStatus.get() == 7)) {
         if (FileStatus.get() == 6) {
            Simulator.getInstance().stopExecution(this);
         }

         Globals.getGui().getRunAssembleAction().actionPerformed((ActionEvent)null);
      }

   }
}
