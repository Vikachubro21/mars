package mars.venus;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

public class FileOpenAction extends GuiAction {
   private File mostRecentlyOpenedFile;
   private JFileChooser fileChooser;
   private int fileFilterCount;
   private ArrayList fileFilterList;
   private PropertyChangeListener listenForUserAddedFileFilter;

   public FileOpenAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.mainUI.editor.open();
   }
}
