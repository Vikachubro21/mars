package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

class SettingsDialog extends JDialog {
   JButton applyButton;
   JButton cancelButton;
   JCheckBox captureResizeCheckBox;
   JCheckBox captureMoveCheckBox;
   JCheckBox captureRescaleCheckBox;
   JRadioButton captureDisplayCenteredButton;
   JRadioButton captureDisplayUpperleftButton;
   Integer[] scribblerLineWidthSettings = new Integer[]{new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5), new Integer(6), new Integer(7), new Integer(8)};
   JComboBox lineWidthSetting;
   JButton lineColorSetting;
   JCheckBox dialogCentered;
   JDialog dialog = this;
   Color scribblerLineColorSetting;
   static final String SETTINGS_APPLY_TOOLTIP_TEXT = "Apply current settings and close the dialog.";
   static final String SETTINGS_CANCEL_TOOLTIP_TEXT = "Close the dialog without applying any setting changes.";
   static final String SETTINGS_SCRIBBLER_WIDTH_TOOLTIP_TEXT = "Scribbler line thickness in pixels.";
   static final String SETTINGS_SCRIBBLER_COLOR_TOOLTIP_TEXT = "Click here to change Scribbler line color.";
   static final String SETTINGS_DIALOG_CENTERED_TOOLTIP_TEXT = "Whether to center this dialog over the Magnifier.";

   SettingsDialog(JFrame frame) {
      super(frame, "Magnifier Tool Settings");
      this.setDefaultCloseOperation(2);
      Container contentPane = this.getContentPane();
      contentPane.setLayout(new BorderLayout());
      new JPanel();
      JPanel selectionsPanel = new JPanel(new GridLayout(2, 1));
      selectionsPanel.add(this.getCaptureDisplayPanel());
      JPanel secondRow = new JPanel(new GridLayout(1, 2));
      secondRow.add(this.getAutomaticCaptureSettingsPanel());
      secondRow.add(this.getScribblerPanel(this));
      selectionsPanel.add(secondRow);
      contentPane.add(selectionsPanel);
      contentPane.add(this.getButtonRowPanel(), "South");
      this.pack();
      if (this.dialogCentered.isSelected()) {
         this.setLocationRelativeTo(frame);
      }

      this.setVisible(true);
   }

   private JPanel getButtonRowPanel() {
      JPanel buttonRow = new JPanel();
      this.applyButton = new JButton("Apply and Close");
      this.applyButton.setToolTipText("Apply current settings and close the dialog.");
      this.applyButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            ((Magnifier)SettingsDialog.this.getOwner()).captureResize.setEnabled(SettingsDialog.this.captureResizeCheckBox.isSelected());
            ((Magnifier)SettingsDialog.this.getOwner()).captureMove.setEnabled(SettingsDialog.this.captureMoveCheckBox.isSelected());
            ((Magnifier)SettingsDialog.this.getOwner()).captureRescale.setEnabled(SettingsDialog.this.captureRescaleCheckBox.isSelected());
            ((Magnifier)SettingsDialog.this.getOwner()).captureDisplayCenter.setEnabled(SettingsDialog.this.captureDisplayCenteredButton.isSelected());
            ((Magnifier)SettingsDialog.this.getOwner()).captureDisplayUpperleft.setEnabled(SettingsDialog.this.captureDisplayUpperleftButton.isSelected());
            ((Magnifier)SettingsDialog.this.getOwner()).dialogDisplayCenter.setEnabled(SettingsDialog.this.dialogCentered.isSelected());
            if (SettingsDialog.this.captureDisplayCenteredButton.isSelected()) {
               ((Magnifier)SettingsDialog.this.getOwner()).alignment = new CaptureDisplayCentered();
            } else if (SettingsDialog.this.captureDisplayUpperleftButton.isSelected()) {
               ((Magnifier)SettingsDialog.this.getOwner()).alignment = new CaptureDisplayUpperleft();
            }

            ((Magnifier)SettingsDialog.this.getOwner()).scribblerSettings.setLineWidth(SettingsDialog.this.scribblerLineWidthSettings[SettingsDialog.this.lineWidthSetting.getSelectedIndex()]);
            ((Magnifier)SettingsDialog.this.getOwner()).scribblerSettings.setLineColor(SettingsDialog.this.lineColorSetting.getBackground());
            SettingsDialog.this.dialog.dispose();
         }
      });
      this.cancelButton = new JButton("Cancel");
      this.cancelButton.setToolTipText("Close the dialog without applying any setting changes.");
      this.cancelButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            SettingsDialog.this.dialog.dispose();
         }
      });
      this.dialogCentered = new JCheckBox("Dialog centered", ((Magnifier)this.getOwner()).dialogDisplayCenter.isEnabled());
      this.dialogCentered.setToolTipText("Whether to center this dialog over the Magnifier.");
      buttonRow.add(this.applyButton);
      buttonRow.add(this.cancelButton);
      buttonRow.add(this.dialogCentered);
      return buttonRow;
   }

   private JPanel getAutomaticCaptureSettingsPanel() {
      JPanel automaticCaptureSettings = new JPanel();
      automaticCaptureSettings.setBorder(new TitledBorder("Automatic Capture"));
      Box automaticCaptureSettingsBox = Box.createHorizontalBox();
      automaticCaptureSettings.add(automaticCaptureSettingsBox);
      this.captureResizeCheckBox = new JCheckBox("Capture upon resize", ((Magnifier)this.getOwner()).captureResize.isEnabled());
      this.captureMoveCheckBox = new JCheckBox("Capture upon move", ((Magnifier)this.getOwner()).captureMove.isEnabled());
      this.captureRescaleCheckBox = new JCheckBox("Capture upon rescale", ((Magnifier)this.getOwner()).captureRescale.isEnabled());
      JPanel checkboxColumn = new JPanel(new GridLayout(3, 1));
      checkboxColumn.add(this.captureResizeCheckBox);
      checkboxColumn.add(this.captureMoveCheckBox);
      checkboxColumn.add(this.captureRescaleCheckBox);
      automaticCaptureSettingsBox.add(checkboxColumn);
      return automaticCaptureSettings;
   }

   private JPanel getCaptureDisplayPanel() {
      JPanel captureDisplaySetting = new JPanel();
      captureDisplaySetting.setBorder(new TitledBorder("Capture and Display"));
      Box captureDisplaySettingsBox = Box.createHorizontalBox();
      captureDisplaySetting.add(captureDisplaySettingsBox);
      this.captureDisplayCenteredButton = new JRadioButton("Capture area behind magnifier and display centered", ((Magnifier)this.getOwner()).captureDisplayCenter.isEnabled());
      this.captureDisplayUpperleftButton = new JRadioButton("Capture area behind magnifier and display upper-left", ((Magnifier)this.getOwner()).captureDisplayUpperleft.isEnabled());
      ButtonGroup displayButtonGroup = new ButtonGroup();
      displayButtonGroup.add(this.captureDisplayCenteredButton);
      displayButtonGroup.add(this.captureDisplayUpperleftButton);
      JPanel radioColumn = new JPanel(new GridLayout(2, 1));
      radioColumn.add(this.captureDisplayCenteredButton);
      radioColumn.add(this.captureDisplayUpperleftButton);
      new JPanel(new GridLayout(1, 1));
      captureDisplaySettingsBox.add(radioColumn);
      return captureDisplaySetting;
   }

   private JPanel getScribblerPanel(final JDialog dialog) {
      JPanel scribblerSettings = new JPanel();
      scribblerSettings.setBorder(new TitledBorder("Scribbler"));
      Box scribblerSettingsBox = Box.createHorizontalBox();
      scribblerSettings.add(scribblerSettingsBox);
      this.lineWidthSetting = new JComboBox(this.scribblerLineWidthSettings);
      this.lineWidthSetting.setToolTipText("Scribbler line thickness in pixels.");
      this.lineWidthSetting.setSelectedIndex(((Magnifier)this.getOwner()).scribblerSettings.getLineWidth() - 1);
      this.lineColorSetting = new JButton("   ");
      this.lineColorSetting.setToolTipText("Click here to change Scribbler line color.");
      this.lineColorSetting.setBackground(((Magnifier)this.getOwner()).scribblerSettings.getLineColor());
      this.lineColorSetting.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Color newColor = JColorChooser.showDialog(dialog, "Scribbler line color", SettingsDialog.this.lineColorSetting.getBackground());
            SettingsDialog.this.lineColorSetting.setBackground(newColor);
         }
      });
      this.scribblerLineColorSetting = this.lineColorSetting.getBackground();
      JPanel settingsColumn = new JPanel(new GridLayout(2, 1, 5, 5));
      settingsColumn.add(this.lineWidthSetting);
      settingsColumn.add(this.lineColorSetting);
      JPanel labelColumn = new JPanel(new GridLayout(2, 1, 5, 5));
      labelColumn.add(new JLabel("Line width ", 2));
      labelColumn.add(new JLabel("Line color ", 2));
      scribblerSettingsBox.add(labelColumn);
      scribblerSettingsBox.add(settingsColumn);
      return scribblerSettings;
   }
}
