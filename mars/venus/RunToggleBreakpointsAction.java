package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import mars.Globals;

public class RunToggleBreakpointsAction extends GuiAction {
   public RunToggleBreakpointsAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().toggleBreakpoints();
   }
}
