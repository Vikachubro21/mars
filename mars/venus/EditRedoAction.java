package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.KeyStroke;

public class EditRedoAction extends GuiAction {
   public EditRedoAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
      this.setEnabled(false);
   }

   public void actionPerformed(ActionEvent e) {
      EditPane editPane = this.mainUI.getMainPane().getEditPane();
      if (editPane != null) {
         editPane.redo();
         this.updateRedoState();
         this.mainUI.editUndoAction.updateUndoState();
      }

   }

   void updateRedoState() {
      EditPane editPane = this.mainUI.getMainPane().getEditPane();
      this.setEnabled(editPane != null && editPane.getUndoManager().canRedo());
   }
}
