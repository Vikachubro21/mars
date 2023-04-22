package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.KeyStroke;

public class EditUndoAction extends GuiAction {
   public EditUndoAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
      this.setEnabled(false);
   }

   public void actionPerformed(ActionEvent e) {
      EditPane editPane = this.mainUI.getMainPane().getEditPane();
      if (editPane != null) {
         editPane.undo();
         this.updateUndoState();
         this.mainUI.editRedoAction.updateRedoState();
      }

   }

   void updateUndoState() {
      EditPane editPane = this.mainUI.getMainPane().getEditPane();
      this.setEnabled(editPane != null && editPane.getUndoManager().canUndo());
   }
}
