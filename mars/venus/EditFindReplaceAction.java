package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import mars.Globals;

public class EditFindReplaceAction extends GuiAction {
   private static String searchString = "";
   private static boolean caseSensitivity = true;
   private static final String DIALOG_TITLE = "Find and Replace";
   JDialog findReplaceDialog;

   public EditFindReplaceAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.findReplaceDialog = new FindReplaceDialog(Globals.getGui(), "Find and Replace", false);
      this.findReplaceDialog.setVisible(true);
   }

   private class FindReplaceDialog extends JDialog {
      JButton findButton;
      JButton replaceButton;
      JButton replaceAllButton;
      JButton closeButton;
      JTextField findInputField;
      JTextField replaceInputField;
      JCheckBox caseSensitiveCheckBox;
      JRadioButton linearFromStart;
      JRadioButton circularFromCursor;
      private JLabel resultsLabel;
      public static final String FIND_TOOL_TIP_TEXT = "Find next occurrence of given text; wraps around at end";
      public static final String REPLACE_TOOL_TIP_TEXT = "Replace current occurrence of text then find next";
      public static final String REPLACE_ALL_TOOL_TIP_TEXT = "Replace all occurrences of text";
      public static final String CLOSE_TOOL_TIP_TEXT = "Close the dialog";
      public static final String RESULTS_TOOL_TIP_TEXT = "Outcome of latest operation (button click)";
      public static final String RESULTS_TEXT_FOUND = "Text found";
      public static final String RESULTS_TEXT_NOT_FOUND = "Text not found";
      public static final String RESULTS_TEXT_REPLACED = "Text replaced and found next";
      public static final String RESULTS_TEXT_REPLACED_LAST = "Text replaced; last occurrence";
      public static final String RESULTS_TEXT_REPLACED_ALL = "Replaced";
      public static final String RESULTS_NO_TEXT_TO_FIND = "No text to find";

      public FindReplaceDialog(Frame owner, String title, boolean modality) {
         super(owner, title, modality);
         this.setContentPane(this.buildDialogPanel());
         this.setDefaultCloseOperation(0);
         this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
               FindReplaceDialog.this.performClose();
            }
         });
         this.pack();
         this.setLocationRelativeTo(owner);
      }

      private JPanel buildDialogPanel() {
         JPanel dialogPanel = new JPanel(new BorderLayout());
         dialogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
         dialogPanel.add(this.buildInputPanel(), "North");
         dialogPanel.add(this.buildOptionsPanel());
         dialogPanel.add(this.buildControlPanel(), "South");
         return dialogPanel;
      }

      private Component buildInputPanel() {
         this.findInputField = new JTextField(30);
         if (EditFindReplaceAction.searchString.length() > 0) {
            this.findInputField.setText(EditFindReplaceAction.searchString);
            this.findInputField.selectAll();
         }

         this.replaceInputField = new JTextField(30);
         JPanel inputPanel = new JPanel();
         JPanel labelsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
         JPanel fieldsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
         labelsPanel.add(new JLabel("Find what:"));
         labelsPanel.add(new JLabel("Replace with:"));
         fieldsPanel.add(this.findInputField);
         fieldsPanel.add(this.replaceInputField);
         Box columns = Box.createHorizontalBox();
         columns.add(labelsPanel);
         columns.add(Box.createHorizontalStrut(6));
         columns.add(fieldsPanel);
         inputPanel.add(columns);
         return inputPanel;
      }

      private Component buildOptionsPanel() {
         Box optionsPanel = Box.createHorizontalBox();
         this.caseSensitiveCheckBox = new JCheckBox("Case Sensitive", EditFindReplaceAction.caseSensitivity);
         JPanel casePanel = new JPanel(new GridLayout(2, 1));
         casePanel.add(this.caseSensitiveCheckBox);
         casePanel.setMaximumSize(casePanel.getPreferredSize());
         optionsPanel.add(casePanel);
         optionsPanel.add(Box.createHorizontalStrut(5));
         JPanel resultsPanel = new JPanel(new GridLayout(1, 1));
         resultsPanel.setBorder(BorderFactory.createTitledBorder("Outcome"));
         this.resultsLabel = new JLabel("");
         this.resultsLabel.setForeground(Color.RED);
         this.resultsLabel.setToolTipText("Outcome of latest operation (button click)");
         resultsPanel.add(this.resultsLabel);
         optionsPanel.add(resultsPanel);
         return optionsPanel;
      }

      private Component buildControlPanel() {
         Box controlPanel = Box.createHorizontalBox();
         controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
         this.findButton = new JButton("Find");
         this.findButton.setToolTipText("Find next occurrence of given text; wraps around at end");
         this.findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FindReplaceDialog.this.performFind();
            }
         });
         this.replaceButton = new JButton("Replace then Find");
         this.replaceButton.setToolTipText("Replace current occurrence of text then find next");
         this.replaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FindReplaceDialog.this.performReplace();
            }
         });
         this.replaceAllButton = new JButton("Replace all");
         this.replaceAllButton.setToolTipText("Replace all occurrences of text");
         this.replaceAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FindReplaceDialog.this.performReplaceAll();
            }
         });
         this.closeButton = new JButton("Close");
         this.closeButton.setToolTipText("Close the dialog");
         this.closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FindReplaceDialog.this.performClose();
            }
         });
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(this.findButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(this.replaceButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(this.replaceAllButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(this.closeButton);
         controlPanel.add(Box.createHorizontalGlue());
         return controlPanel;
      }

      private void performFind() {
         this.resultsLabel.setText("");
         if (this.findInputField.getText().length() > 0) {
            EditPane editPane = EditFindReplaceAction.this.mainUI.getMainPane().getEditPane();
            if (editPane != null) {
               EditFindReplaceAction.searchString = this.findInputField.getText();
               int posn = editPane.doFindText(EditFindReplaceAction.searchString, this.caseSensitiveCheckBox.isSelected());
               if (posn == 0) {
                  this.resultsLabel.setText(this.findButton.getText() + ": " + "Text not found");
               } else {
                  this.resultsLabel.setText(this.findButton.getText() + ": " + "Text found");
               }
            }
         } else {
            this.resultsLabel.setText(this.findButton.getText() + ": " + "No text to find");
         }

      }

      private void performReplace() {
         this.resultsLabel.setText("");
         if (this.findInputField.getText().length() > 0) {
            EditPane editPane = EditFindReplaceAction.this.mainUI.getMainPane().getEditPane();
            if (editPane != null) {
               EditFindReplaceAction.searchString = this.findInputField.getText();
               int posn = editPane.doReplace(EditFindReplaceAction.searchString, this.replaceInputField.getText(), this.caseSensitiveCheckBox.isSelected());
               String result = this.replaceButton.getText() + ": ";
               switch (posn) {
                  case 0:
                     result = result + "Text not found";
                     break;
                  case 1:
                     result = result + "Text found";
                     break;
                  case 2:
                     result = result + "Text replaced and found next";
                     break;
                  case 3:
                     result = result + "Text replaced; last occurrence";
               }

               this.resultsLabel.setText(result);
            }
         } else {
            this.resultsLabel.setText(this.replaceButton.getText() + ": " + "No text to find");
         }

      }

      private void performReplaceAll() {
         this.resultsLabel.setText("");
         if (this.findInputField.getText().length() > 0) {
            EditPane editPane = EditFindReplaceAction.this.mainUI.getMainPane().getEditPane();
            if (editPane != null) {
               EditFindReplaceAction.searchString = this.findInputField.getText();
               int replaceCount = editPane.doReplaceAll(EditFindReplaceAction.searchString, this.replaceInputField.getText(), this.caseSensitiveCheckBox.isSelected());
               if (replaceCount == 0) {
                  this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + "Text not found");
               } else {
                  this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + "Replaced" + " " + replaceCount + " occurrence" + (replaceCount == 1 ? "" : "s"));
               }
            }
         } else {
            this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + "No text to find");
         }

      }

      private void performClose() {
         EditFindReplaceAction.caseSensitivity = this.caseSensitiveCheckBox.isSelected();
         this.setVisible(false);
         this.dispose();
      }
   }
}
