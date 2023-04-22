package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import mars.Globals;

public class RunClearBreakpointsAction extends GuiAction implements TableModelListener {
   public RunClearBreakpointsAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
      Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().registerTableModelListener(this);
   }

   public void actionPerformed(ActionEvent e) {
      Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().clearAllBreakpoints();
   }

   public void tableChanged(TableModelEvent e) {
      this.setEnabled(Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().getBreakpointCount() > 0);
   }
}
