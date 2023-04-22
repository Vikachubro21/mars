package mars.venus;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;

public class EditTabbedPane extends JTabbedPane {
   EditPane editTab;
   MainPane mainPane;
   private VenusUI mainUI;
   private Editor editor;
   private static FileOpener fileOpener;

   int state = 0;

   public EditTabbedPane(VenusUI appFrame, Editor editor, MainPane mainPane) {
      this.mainUI = appFrame;
      this.editor = editor;
      fileOpener = new FileOpener(editor);
      this.mainPane = mainPane;
      this.editor.setEditTabbedPane(this);
      this.editTab = (EditPane) this.getSelectedComponent();
      this.addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
            EditPane editPane = (EditPane)EditTabbedPane.this.getSelectedComponent();
            if (editPane != null) {
               editPane.setChangeListener(this);
               if (Globals.getSettings().getBooleanSetting(3)) {
                  EditTabbedPane.this.updateTitles(editPane);
               } else {
                  EditTabbedPane.this.updateTitlesAndMenuState(editPane);
                  EditTabbedPane.this.mainPane.getExecutePane().clearPane();
               }

               editPane.tellEditingComponentToRequestFocusInWindow();
            }

         }
      });
   }

   public EditPane getCurrentEditTab() {
      return (EditPane)this.getSelectedComponent();
   }

   public void setCurrentEditTab(EditPane editPane) {
      this.setSelectedComponent(editPane);
   }

   public EditPane getCurrentEditTabForFile(File file) {
      EditPane result = null;
      EditPane tab = this.getEditPaneForFile(file.getPath());
      if (tab != null) {
         if (tab != this.getCurrentEditTab()) {
            this.setCurrentEditTab(tab);
         }

         return tab;
      } else {
         if (openFile(file)) {
            result = this.getCurrentEditTab();
         }

         return result;
      }
   }

   public void newFile() {
      EditPane editPane = new EditPane(this.mainUI);
      editPane.setSourceCode("", true);
      editPane.setShowLineNumbersEnabled(true);
      editPane.setFileStatus(1);
      String name = this.editor.getNextDefaultFilename();
      editPane.setPathname(name);
      this.addTab(name, editPane);
      FileStatus.reset();
      FileStatus.setName(name);
      FileStatus.set(1);
      RegisterFile.resetRegisters();
      VenusUI.setReset(true);
      this.mainPane.getExecutePane().clearPane();
      this.mainPane.setSelectedComponent(this);
      editPane.displayCaretPosition(new Point(1, 1));
      this.setSelectedComponent(editPane);
      this.updateTitlesAndMenuState(editPane);
      editPane.tellEditingComponentToRequestFocusInWindow();
   }

   public boolean openFile() {
      return fileOpener.openFile();
   }

   public static boolean openFile(File file) {
      return fileOpener.openFile(file);
   }

   public boolean closeCurrentFile() {
      EditPane editPane = this.getCurrentEditTab();
      if (editPane != null) {
         if (!this.editsSavedOrAbandoned()) {
            return false;
         }

         this.remove(editPane);
         this.mainPane.getExecutePane().clearPane();
         this.mainPane.setSelectedComponent(this);
      }

      return true;
   }

   public boolean closeAllFiles() {
      boolean result = true;
      boolean unsavedChanges = false;
      int tabCount = this.getTabCount();
      if (tabCount > 0) {
         this.mainPane.getExecutePane().clearPane();
         this.mainPane.setSelectedComponent(this);
         EditPane[] tabs = new EditPane[tabCount];

         int i;
         for(i = 0; i < tabCount; ++i) {
            tabs[i] = (EditPane)this.getComponentAt(i);
            if (tabs[i].hasUnsavedEdits()) {
               unsavedChanges = true;
            }
         }

         if (unsavedChanges) {
            switch (this.confirm("one or more files")) {
               case 0:
                  boolean removedAll = true;

                  for(i = 0; i < tabCount; ++i) {
                     if (tabs[i].hasUnsavedEdits()) {
                        this.setSelectedComponent(tabs[i]);
                        boolean saved = this.saveCurrentFile();
                        if (saved) {
                           this.remove(tabs[i]);
                        } else {
                           removedAll = false;
                        }
                     } else {
                        this.remove(tabs[i]);
                     }
                  }

                  return removedAll;
               case 1:
                  for(i = 0; i < tabCount; ++i) {
                     this.remove(tabs[i]);
                  }

                  return true;
               case 2:
                  return false;
               default:
                  return false;
            }
         }

         for(i = 0; i < tabCount; ++i) {
            this.remove(tabs[i]);
         }
      }

      return result;
   }

   public boolean saveCurrentFile() {
      EditPane editPane = this.getCurrentEditTab();
      if (this.saveFile(editPane)) {
         FileStatus.setSaved(true);
         FileStatus.setEdited(false);
         FileStatus.set(3);
         editPane.setFileStatus(3);
         this.updateTitlesAndMenuState(editPane);
         return true;
      } else {
         return false;
      }
   }

   private boolean saveFile(EditPane editPane) {
      if (editPane != null) {
         File theFile;
         if (editPane.isNew()) {
            theFile = this.saveAsFile(editPane);
            if (theFile != null) {
               editPane.setPathname(theFile.getPath());
            }

            return theFile != null;
         } else {
            theFile = new File(editPane.getPathname());

            try {
               BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile));
               outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
               outFileStream.close();
               return true;
            } catch (IOException var4) {
               JOptionPane.showMessageDialog((Component)null, "Save operation could not be completed due to an error:\n" + var4, "Save Operation Failed", 0);
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean saveAsCurrentFile() {
      EditPane editPane = this.getCurrentEditTab();
      File theFile = this.saveAsFile(editPane);
      if (theFile != null) {
         FileStatus.setFile(theFile);
         FileStatus.setName(theFile.getPath());
         FileStatus.setSaved(true);
         FileStatus.setEdited(false);
         FileStatus.set(3);
         this.editor.setCurrentSaveDirectory(theFile.getParent());
         editPane.setPathname(theFile.getPath());
         editPane.setFileStatus(3);
         this.updateTitlesAndMenuState(editPane);
         return true;
      } else {
         return false;
      }
   }

   private File saveAsFile(EditPane editPane) {
      File theFile = null;
      if (editPane != null) {
         JFileChooser saveDialog = null;
         boolean operationOK = false;

         while(!operationOK) {
            if (editPane.isNew()) {
               saveDialog = new JFileChooser(this.editor.getCurrentSaveDirectory());
            } else {
               File f = new File(editPane.getPathname());
               if (f != null) {
                  saveDialog = new JFileChooser(f.getParent());
               } else {
                  saveDialog = new JFileChooser(this.editor.getCurrentSaveDirectory());
               }
            }

            String paneFile = editPane.getFilename();
            if (paneFile != null) {
               saveDialog.setSelectedFile(new File(paneFile));
            }

            saveDialog.setDialogTitle("Save As");
            int decision = saveDialog.showSaveDialog(this.mainUI);
            if (decision != 0) {
               return null;
            }

            theFile = saveDialog.getSelectedFile();
            operationOK = true;
            if (theFile.exists()) {
               int overwrite = JOptionPane.showConfirmDialog(this.mainUI, "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?", "Overwrite existing file?", 1, 2);
               switch (overwrite) {
                  case 0:
                     operationOK = true;
                     break;
                  case 1:
                     operationOK = false;
                     break;
                  case 2:
                     return null;
                  default:
                     return null;
               }
            }
         }

         try {
            BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile));
            outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
            outFileStream.close();
         } catch (IOException var8) {
            JOptionPane.showMessageDialog((Component)null, "Save As operation could not be completed due to an error:\n" + var8, "Save As Operation Failed", 0);
            return null;
         }
      }

      return theFile;
   }

   public boolean saveAllFiles() {
      boolean result = false;
      int tabCount = this.getTabCount();
      if (tabCount > 0) {
         result = true;
         EditPane[] tabs = new EditPane[tabCount];
         EditPane savedPane = this.getCurrentEditTab();

         for(int i = 0; i < tabCount; ++i) {
            tabs[i] = (EditPane)this.getComponentAt(i);
            if (tabs[i].hasUnsavedEdits()) {
               this.setCurrentEditTab(tabs[i]);
               if (this.saveFile(tabs[i])) {
                  tabs[i].setFileStatus(3);
                  this.editor.setTitle(tabs[i].getPathname(), tabs[i].getFilename(), tabs[i].getFileStatus());
               } else {
                  result = false;
               }
            }
         }

         this.setCurrentEditTab(savedPane);
         if (result) {
            EditPane editPane = this.getCurrentEditTab();
            FileStatus.setSaved(true);
            FileStatus.setEdited(false);
            FileStatus.set(3);
            editPane.setFileStatus(3);
            this.updateTitlesAndMenuState(editPane);
         }
      }

      return result;
   }

   public void remove(EditPane editPane) {
      super.remove(editPane);
      editPane = this.getCurrentEditTab();
      if (editPane == null) {
         FileStatus.set(0);
         this.editor.setTitle("", "", 0);
         Globals.getGui().setMenuState(0);
      } else {
         FileStatus.set(editPane.getFileStatus());
         this.updateTitlesAndMenuState(editPane);
      }

      if (this.getTabCount() == 0) {
         this.mainUI.haveMenuRequestFocus();
      }

   }

   private void updateTitlesAndMenuState(EditPane editPane) {
      this.editor.setTitle(editPane.getPathname(), editPane.getFilename(), editPane.getFileStatus());
      editPane.updateStaticFileStatus();
      Globals.getGui().setMenuState(editPane.getFileStatus());
   }

   private void updateTitles(EditPane editPane) {
      this.editor.setTitle(editPane.getPathname(), editPane.getFilename(), editPane.getFileStatus());
      boolean assembled = FileStatus.isAssembled();
      editPane.updateStaticFileStatus();
      FileStatus.setAssembled(assembled);
   }

   public EditPane getEditPaneForFile(String pathname) {
      EditPane openPane = null;

      for(int i = 0; i < this.getTabCount(); ++i) {
         EditPane pane = (EditPane)this.getComponentAt(i);
         if (pane.getPathname().equals(pathname)) {
            openPane = pane;
            break;
         }
      }

      return openPane;
   }

   public boolean editsSavedOrAbandoned() {
      EditPane currentPane = this.getCurrentEditTab();
      if (currentPane != null && currentPane.hasUnsavedEdits()) {
         switch (this.confirm(currentPane.getFilename())) {
            case 0:
               return this.saveCurrentFile();
            case 1:
               return true;
            case 2:
               return false;
            default:
               return false;
         }
      } else {
         return true;
      }
   }

   private int confirm(String name) {
      return JOptionPane.showConfirmDialog(this.mainUI, "Changes to " + name + " will be lost unless you save.  Do you wish to save all changes now?", "Save program changes?", 1, 2);
   }

   private class FileOpener {
      private File mostRecentlyOpenedFile = null;
      private JFileChooser fileChooser;
      private int fileFilterCount;
      private ArrayList fileFilterList;
      private PropertyChangeListener listenForUserAddedFileFilter;
      private Editor theEditor;

      public FileOpener(Editor theEditor) {
         this.theEditor = theEditor;
         this.fileChooser = new JFileChooser();
         this.listenForUserAddedFileFilter = new ChoosableFileFilterChangeListener((ChoosableFileFilterChangeListener)null);
         this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);
         this.fileFilterList = new ArrayList();
         this.fileFilterList.add(this.fileChooser.getAcceptAllFileFilter());
         this.fileFilterList.add(FilenameFinder.getFileFilter(Globals.fileExtensions, "Assembler Files", true));
         this.fileFilterCount = 0;
         this.setChoosableFileFilters();
      }

      private boolean openFile() {
         this.setChoosableFileFilters();
         this.fileChooser.setCurrentDirectory(new File(this.theEditor.getCurrentOpenDirectory()));
         if (Globals.getSettings().getAssembleOnOpenEnabled() && this.mostRecentlyOpenedFile != null) {
            this.fileChooser.setSelectedFile(this.mostRecentlyOpenedFile);
         }

         if (this.fileChooser.showOpenDialog(EditTabbedPane.this.mainUI) == 0) {
            File theFile = this.fileChooser.getSelectedFile();
            this.theEditor.setCurrentOpenDirectory(theFile.getParent());
            if (!this.openFile(theFile)) {
               return false;
            }

            if (theFile.canRead() && Globals.getSettings().getAssembleOnOpenEnabled()) {
               EditTabbedPane.this.mainUI.getRunAssembleAction().actionPerformed((ActionEvent)null);
            }
         }

         return true;
      }

      public boolean openFile(File theFile) {
         try {
            theFile = theFile.getCanonicalFile();
         } catch (IOException var8) {
         }

         String currentFilePath = theFile.getPath();
         EditPane editPane = EditTabbedPane.this.getEditPaneForFile(currentFilePath);
         if (editPane != null) {
            EditTabbedPane.this.setSelectedComponent(editPane);
            EditTabbedPane.this.updateTitles(editPane);
            return false;
         } else {
            editPane = new EditPane(EditTabbedPane.this.mainUI);
            editPane.setPathname(currentFilePath);
            FileStatus.setName(currentFilePath);
            FileStatus.setFile(theFile);
            FileStatus.set(8);
            if (theFile.canRead()) {
               Globals.program = new MIPSprogram();

               try {
                  Globals.program.readSource(currentFilePath);
               } catch (ProcessingException var7) {
               }

               StringBuffer fileContents = new StringBuffer((int)theFile.length());
               int lineNumber = 1;

               for(String line = Globals.program.getSourceLine(lineNumber++); line != null; line = Globals.program.getSourceLine(lineNumber++)) {
                  fileContents.append(line + "\n");
               }

               editPane.setSourceCode(fileContents.toString(), true);
               editPane.discardAllUndoableEdits();
               editPane.setShowLineNumbersEnabled(true);
               editPane.setFileStatus(3);
               EditTabbedPane.this.addTab(editPane.getFilename(), editPane);
               EditTabbedPane.this.setToolTipTextAt(EditTabbedPane.this.indexOfComponent(editPane), editPane.getPathname());
               EditTabbedPane.this.setSelectedComponent(editPane);
               FileStatus.setSaved(true);
               FileStatus.setEdited(false);
               FileStatus.set(3);
               if (Globals.getSettings().getBooleanSetting(3)) {
                  EditTabbedPane.this.updateTitles(editPane);
               } else {
                  EditTabbedPane.this.updateTitlesAndMenuState(editPane);
                  EditTabbedPane.this.mainPane.getExecutePane().clearPane();
               }

               EditTabbedPane.this.mainPane.setSelectedComponent(EditTabbedPane.this);
               editPane.tellEditingComponentToRequestFocusInWindow();
               this.mostRecentlyOpenedFile = theFile;
               editPane.onFileOpenSetLineNumbersActive();
               EditTabbedPane.this.addMouseListener(new MouseAdapter() {
                  public void mouseReleased(MouseEvent e) {
                     if (e.getButton() == 2) {
                        EditTabbedPane.this.closeCurrentFile();
                     }

                  }
               });
            }

            return true;
         }
      }

      private void setChoosableFileFilters() {
         if (this.fileFilterCount < this.fileFilterList.size() || this.fileFilterList.size() != this.fileChooser.getChoosableFileFilters().length) {
            this.fileFilterCount = this.fileFilterList.size();
            boolean activeListener = false;
            if (this.fileChooser.getPropertyChangeListeners().length > 0) {
               this.fileChooser.removePropertyChangeListener(this.listenForUserAddedFileFilter);
               activeListener = true;
            }

            this.fileChooser.resetChoosableFileFilters();

            for(int i = 0; i < this.fileFilterList.size(); ++i) {
               this.fileChooser.addChoosableFileFilter((FileFilter)this.fileFilterList.get(i));
            }

            if (activeListener) {
               this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);
            }
         }

      }

      private class ChoosableFileFilterChangeListener implements PropertyChangeListener {
         private ChoosableFileFilterChangeListener() {
         }

         public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName() == "ChoosableFileFilterChangedProperty") {
               FileFilter[] newFilters = (FileFilter[])e.getNewValue();
               FileFilter[] oldFilters = (FileFilter[])e.getOldValue();
               if (newFilters.length > FileOpener.this.fileFilterList.size()) {
                  FileOpener.this.fileFilterList.add(newFilters[newFilters.length - 1]);
               }
            }

         }

         // $FF: synthetic method
         ChoosableFileFilterChangeListener(ChoosableFileFilterChangeListener var2) {
            this();
         }
      }
   }
}
