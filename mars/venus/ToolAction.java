package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import mars.tools.MarsTool;

public class ToolAction extends AbstractAction {
   private Class toolClass;

   public ToolAction(Class toolClass, String toolName) {
      super(toolName, (Icon)null);
      this.toolClass = toolClass;
   }

   public void actionPerformed(ActionEvent e) {
      try {
         ((MarsTool)this.toolClass.newInstance()).action();
      } catch (Exception var3) {
      }

   }
}
