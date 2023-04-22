package mars.venus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import mars.Globals;

public class SettingsExceptionHandlerAction extends GuiAction {
   JDialog exceptionHandlerDialog;
   JCheckBox exceptionHandlerSetting;
   JButton exceptionHandlerSelectionButton;
   JTextField exceptionHandlerDisplay;
   boolean initialSelected;
   String initialPathname;

   public SettingsExceptionHandlerAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.initialSelected = Globals.getSettings().getExceptionHandlerEnabled();
      this.initialPathname = Globals.getSettings().getExceptionHandler();
      this.exceptionHandlerDialog = new JDialog(Globals.getGui(), "Exception Handler", true);
      this.exceptionHandlerDialog.setContentPane(this.buildDialogPanel());
      this.exceptionHandlerDialog.setDefaultCloseOperation(0);
      this.exceptionHandlerDialog.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent we) {
            SettingsExceptionHandlerAction.this.closeDialog();
         }
      });
      this.exceptionHandlerDialog.pack();
      this.exceptionHandlerDialog.setLocationRelativeTo(Globals.getGui());
      this.exceptionHandlerDialog.setVisible(true);
   }

   private JPanel buildDialogPanel() {
      JPanel contents = new JPanel(new BorderLayout(20, 20));
      contents.setBorder(new EmptyBorder(10, 10, 10, 10));
      this.exceptionHandlerSetting = new JCheckBox("Include this exception handler file in all assemble operations");
      this.exceptionHandlerSetting.setSelected(Globals.getSettings().getExceptionHandlerEnabled());
      this.exceptionHandlerSetting.addActionListener(new ExceptionHandlerSettingAction((ExceptionHandlerSettingAction)null));
      contents.add(this.exceptionHandlerSetting, "North");
      JPanel specifyHandlerFile = new JPanel();
      this.exceptionHandlerSelectionButton = new JButton("Browse");
      this.exceptionHandlerSelectionButton.setEnabled(this.exceptionHandlerSetting.isSelected());
      this.exceptionHandlerSelectionButton.addActionListener(new ExceptionHandlerSelectionAction((ExceptionHandlerSelectionAction)null));
      this.exceptionHandlerDisplay = new JTextField(Globals.getSettings().getExceptionHandler(), 30);
      this.exceptionHandlerDisplay.setEditable(false);
      this.exceptionHandlerDisplay.setEnabled(this.exceptionHandlerSetting.isSelected());
      specifyHandlerFile.add(this.exceptionHandlerSelectionButton);
      specifyHandlerFile.add(this.exceptionHandlerDisplay);
      contents.add(specifyHandlerFile, "Center");
      Box controlPanel = Box.createHorizontalBox();
      JButton okButton = new JButton("OK");
      okButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            SettingsExceptionHandlerAction.this.performOK();
            SettingsExceptionHandlerAction.this.closeDialog();
         }
      });
      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            SettingsExceptionHandlerAction.this.closeDialog();
         }
      });
      controlPanel.add(Box.createHorizontalGlue());
      controlPanel.add(okButton);
      controlPanel.add(Box.createHorizontalGlue());
      controlPanel.add(cancelButton);
      controlPanel.add(Box.createHorizontalGlue());
      contents.add(controlPanel, "South");
      return contents;
   }

   private void performOK() {
      boolean finalSelected = this.exceptionHandlerSetting.isSelected();
      String finalPathname = this.exceptionHandlerDisplay.getText();
      if (this.initialSelected != finalSelected || this.initialPathname == null && finalPathname != null || this.initialPathname != null && !this.initialPathname.equals(finalPathname)) {
         Globals.getSettings().setExceptionHandlerEnabled(finalSelected);
         if (finalSelected) {
            Globals.getSettings().setExceptionHandler(finalPathname);
         }
      }

   }

   private void closeDialog() {
      this.exceptionHandlerDialog.setVisible(false);
      this.exceptionHandlerDialog.dispose();
   }

   private class ExceptionHandlerSelectionAction implements ActionListener {
      private ExceptionHandlerSelectionAction() {
      }

      public void actionPerformed(ActionEvent e) {
         JFileChooser chooser = new JFileChooser();
         String pathname = Globals.getSettings().getExceptionHandler();
         if (pathname != null) {
            File file = new File(pathname);
            if (file.exists()) {
               chooser.setSelectedFile(file);
            }
         }

         int result = chooser.showOpenDialog(Globals.getGui());
         if (result == 0) {
            pathname = chooser.getSelectedFile().getPath();
            SettingsExceptionHandlerAction.this.exceptionHandlerDisplay.setText(pathname);
         }

      }

      // $FF: synthetic method
      ExceptionHandlerSelectionAction(ExceptionHandlerSelectionAction var2) {
         this();
      }
   }

   private class ExceptionHandlerSettingAction implements ActionListener {
      private ExceptionHandlerSettingAction() {
      }

      public void actionPerformed(ActionEvent e) {
         boolean selected = ((JCheckBox)e.getSource()).isSelected();
         SettingsExceptionHandlerAction.this.exceptionHandlerSelectionButton.setEnabled(selected);
         SettingsExceptionHandlerAction.this.exceptionHandlerDisplay.setEnabled(selected);
      }

      // $FF: synthetic method
      ExceptionHandlerSettingAction(ExceptionHandlerSettingAction var2) {
         this();
      }
   }
}
