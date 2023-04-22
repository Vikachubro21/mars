package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;
import mars.Globals;
import mars.venus.editors.jeditsyntax.SyntaxStyle;
import mars.venus.editors.jeditsyntax.SyntaxUtilities;
import mars.venus.editors.jeditsyntax.tokenmarker.MIPSTokenMarker;

public class SettingsEditorAction extends GuiAction {
   JDialog editorDialog;
   JComboBox fontFamilySelector;
   JComboBox fontStyleSelector;
   JSlider tabSizeSelector;
   JTextField fontSizeDisplay;
   String initialFontFamily;
   String initialFontStyle;
   String initialFontSize;
   private static final int gridVGap = 2;
   private static final int gridHGap = 2;
   private static final Border ColorSelectButtonEnabledBorder;
   private static final Border ColorSelectButtonDisabledBorder;
   private static final String GENERIC_TOOL_TIP_TEXT = "Use generic editor (original MARS editor, similar to Notepad) instead of language-aware styled editor";
   private static final String SAMPLE_TOOL_TIP_TEXT = "Current setting; modify using buttons to the right";
   private static final String FOREGROUND_TOOL_TIP_TEXT = "Click, to select text color";
   private static final String BOLD_TOOL_TIP_TEXT = "Toggle text bold style";
   private static final String ITALIC_TOOL_TIP_TEXT = "Toggle text italic style";
   private static final String DEFAULT_TOOL_TIP_TEXT = "Check, to select defaults (disables buttons)";
   private static final String BOLD_BUTTON_TOOL_TIP_TEXT = "B";
   private static final String ITALIC_BUTTON_TOOL_TIP_TEXT = "I";
   private static final String TAB_SIZE_TOOL_TIP_TEXT = "Current tab size in characters";
   private static final String BLINK_SPINNER_TOOL_TIP_TEXT = "Current blinking rate in milliseconds";
   private static final String BLINK_SAMPLE_TOOL_TIP_TEXT = "Displays current blinking rate";
   private static final String CURRENT_LINE_HIGHLIGHT_TOOL_TIP_TEXT = "Check, to highlight line currently being edited";
   private static final String AUTO_INDENT_TOOL_TIP_TEXT = "Check, to enable auto-indent to previous line when Enter key is pressed";
   private static final String[] POPUP_GUIDANCE_TOOL_TIP_TEXT;

   static {
      ColorSelectButtonEnabledBorder = new BevelBorder(0, Color.WHITE, Color.GRAY);
      ColorSelectButtonDisabledBorder = new LineBorder(Color.GRAY, 2);
      POPUP_GUIDANCE_TOOL_TIP_TEXT = new String[]{"Turns off instruction and directive guide popup while typing", "Generates instruction guide popup after first letter of potential instruction is typed", "Generates instruction guide popup after second letter of potential instruction is typed"};
   }

   public SettingsEditorAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.editorDialog = new EditorFontDialog(Globals.getGui(), "Text Editor Settings", true, Globals.getSettings().getEditorFont());
      this.editorDialog.setVisible(true);
   }

   private class EditorFontDialog extends AbstractFontSettingDialog {
      private JButton[] foregroundButtons;
      private JLabel[] samples;
      private JToggleButton[] bold;
      private JToggleButton[] italic;
      private JCheckBox[] useDefault;
      private int[] syntaxStyleIndex;
      private SyntaxStyle[] defaultStyles;
      private SyntaxStyle[] initialStyles;
      private SyntaxStyle[] currentStyles;
      private Font previewFont;
      private JPanel dialogPanel;
      private JPanel syntaxStylePanel;
      private JPanel otherSettingsPanel;
      private JSlider tabSizeSelector;
      private JSpinner tabSizeSpinSelector;
      private JSpinner blinkRateSpinSelector;
      private JSpinner popupPrefixLengthSpinSelector;
      private JCheckBox lineHighlightCheck;
      private JCheckBox genericEditorCheck;
      private JCheckBox autoIndentCheck;
      private Caret blinkCaret;
      private JTextField blinkSample;
      private ButtonGroup popupGuidanceButtons;
      private JRadioButton[] popupGuidanceOptions;
      private boolean syntaxStylesAction = false;
      private int initialEditorTabSize;
      private int initialCaretBlinkRate;
      private int initialPopupGuidance;
      private boolean initialLineHighlighting;
      private boolean initialGenericTextEditor;
      private boolean initialAutoIndent;

      public EditorFontDialog(Frame owner, String title, boolean modality, Font font) {
         super(owner, title, modality, font);
         if (Globals.getSettings().getBooleanSetting(18)) {
            this.syntaxStylePanel.setVisible(false);
            this.otherSettingsPanel.setVisible(false);
         }

      }

      protected JPanel buildDialogPanel() {
         JPanel dialog = new JPanel(new BorderLayout());
         JPanel fontDialogPanel = super.buildDialogPanel();
         JPanel syntaxStylePanel = this.buildSyntaxStylePanel();
         JPanel otherSettingsPanel = this.buildOtherSettingsPanel();
         fontDialogPanel.setBorder(BorderFactory.createTitledBorder("Editor Font"));
         syntaxStylePanel.setBorder(BorderFactory.createTitledBorder("Syntax Styling"));
         otherSettingsPanel.setBorder(BorderFactory.createTitledBorder("Other Editor Settings"));
         dialog.add(fontDialogPanel, "West");
         dialog.add(syntaxStylePanel, "Center");
         dialog.add(otherSettingsPanel, "South");
         this.dialogPanel = dialog;
         this.syntaxStylePanel = syntaxStylePanel;
         this.otherSettingsPanel = otherSettingsPanel;
         return dialog;
      }

      protected Component buildControlPanel() {
         Box controlPanel = Box.createHorizontalBox();
         JButton okButton = new JButton("Apply and Close");
         okButton.setToolTipText("Apply current settings and close dialog");
         okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               EditorFontDialog.this.performApply();
               EditorFontDialog.this.closeDialog();
            }
         });
         JButton applyButton = new JButton("Apply");
         applyButton.setToolTipText("Apply current settings now and leave dialog open");
         applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               EditorFontDialog.this.performApply();
            }
         });
         JButton cancelButton = new JButton("Cancel");
         cancelButton.setToolTipText("Close dialog without applying current settings");
         cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               EditorFontDialog.this.closeDialog();
            }
         });
         JButton resetButton = new JButton("Reset");
         resetButton.setToolTipText("Reset to initial settings without applying");
         resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               EditorFontDialog.this.reset();
            }
         });
         this.initialGenericTextEditor = Globals.getSettings().getBooleanSetting(18);
         this.genericEditorCheck = new JCheckBox("Use Generic Editor", this.initialGenericTextEditor);
         this.genericEditorCheck.setToolTipText("Use generic editor (original MARS editor, similar to Notepad) instead of language-aware styled editor");
         this.genericEditorCheck.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
               if (e.getStateChange() == 1) {
                  EditorFontDialog.this.syntaxStylePanel.setVisible(false);
                  EditorFontDialog.this.otherSettingsPanel.setVisible(false);
               } else {
                  EditorFontDialog.this.syntaxStylePanel.setVisible(true);
                  EditorFontDialog.this.otherSettingsPanel.setVisible(true);
               }

            }
         });
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(okButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(applyButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(cancelButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(resetButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(this.genericEditorCheck);
         controlPanel.add(Box.createHorizontalGlue());
         return controlPanel;
      }

      protected void apply(Font font) {
         Globals.getSettings().setBooleanSetting(18, this.genericEditorCheck.isSelected());
         Globals.getSettings().setBooleanSetting(15, this.lineHighlightCheck.isSelected());
         Globals.getSettings().setBooleanSetting(19, this.autoIndentCheck.isSelected());
         Globals.getSettings().setCaretBlinkRate((Integer)this.blinkRateSpinSelector.getValue());
         Globals.getSettings().setEditorTabSize(this.tabSizeSelector.getValue());
         int i;
         if (this.syntaxStylesAction) {
            for(i = 0; i < this.syntaxStyleIndex.length; ++i) {
               Globals.getSettings().setEditorSyntaxStyleByPosition(this.syntaxStyleIndex[i], new SyntaxStyle(this.samples[i].getForeground(), this.italic[i].isSelected(), this.bold[i].isSelected()));
            }

            this.syntaxStylesAction = false;
         }

         Globals.getSettings().setEditorFont(font);

         for(i = 0; i < this.popupGuidanceOptions.length; ++i) {
            if (this.popupGuidanceOptions[i].isSelected()) {
               if (i == 0) {
                  Globals.getSettings().setBooleanSetting(16, false);
               } else {
                  Globals.getSettings().setBooleanSetting(16, true);
                  Globals.getSettings().setEditorPopupPrefixLength(i);
               }
               break;
            }
         }

      }

      protected void reset() {
         super.reset();
         this.initializeSyntaxStyleChangeables();
         this.resetOtherSettings();
         this.syntaxStylesAction = true;
         this.genericEditorCheck.setSelected(this.initialGenericTextEditor);
      }

      private void resetOtherSettings() {
         this.tabSizeSelector.setValue(this.initialEditorTabSize);
         this.tabSizeSpinSelector.setValue(new Integer(this.initialEditorTabSize));
         this.lineHighlightCheck.setSelected(this.initialLineHighlighting);
         this.autoIndentCheck.setSelected(this.initialAutoIndent);
         this.blinkRateSpinSelector.setValue(new Integer(this.initialCaretBlinkRate));
         this.blinkCaret.setBlinkRate(this.initialCaretBlinkRate);
         this.popupGuidanceOptions[this.initialPopupGuidance].setSelected(true);
      }

      private JPanel buildOtherSettingsPanel() {
         JPanel otherSettingsPanel = new JPanel();
         this.initialEditorTabSize = Globals.getSettings().getEditorTabSize();
         this.tabSizeSelector = new JSlider(1, 32, this.initialEditorTabSize);
         this.tabSizeSelector.setToolTipText("Use slider to select tab size from 1 to 32.");
         this.tabSizeSelector.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
               Integer value = new Integer(((JSlider)e.getSource()).getValue());
               EditorFontDialog.this.tabSizeSpinSelector.setValue(value);
            }
         });
         SpinnerNumberModel tabSizeSpinnerModel = new SpinnerNumberModel(this.initialEditorTabSize, 1, 32, 1);
         this.tabSizeSpinSelector = new JSpinner(tabSizeSpinnerModel);
         this.tabSizeSpinSelector.setToolTipText("Current tab size in characters");
         this.tabSizeSpinSelector.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
               Object value = ((JSpinner)e.getSource()).getValue();
               EditorFontDialog.this.tabSizeSelector.setValue((Integer)value);
            }
         });
         this.initialLineHighlighting = Globals.getSettings().getBooleanSetting(15);
         this.lineHighlightCheck = new JCheckBox("Highlight the line currently being edited");
         this.lineHighlightCheck.setSelected(this.initialLineHighlighting);
         this.lineHighlightCheck.setToolTipText("Check, to highlight line currently being edited");
         this.initialAutoIndent = Globals.getSettings().getBooleanSetting(19);
         this.autoIndentCheck = new JCheckBox("Auto-Indent");
         this.autoIndentCheck.setSelected(this.initialAutoIndent);
         this.autoIndentCheck.setToolTipText("Check, to enable auto-indent to previous line when Enter key is pressed");
         this.initialCaretBlinkRate = Globals.getSettings().getCaretBlinkRate();
         this.blinkSample = new JTextField("     ");
         this.blinkSample.setCaretPosition(2);
         this.blinkSample.setToolTipText("Displays current blinking rate");
         this.blinkSample.setEnabled(false);
         this.blinkCaret = this.blinkSample.getCaret();
         this.blinkCaret.setBlinkRate(this.initialCaretBlinkRate);
         this.blinkCaret.setVisible(true);
         SpinnerNumberModel blinkRateSpinnerModel = new SpinnerNumberModel(this.initialCaretBlinkRate, 0, 1000, 100);
         this.blinkRateSpinSelector = new JSpinner(blinkRateSpinnerModel);
         this.blinkRateSpinSelector.setToolTipText("Current blinking rate in milliseconds");
         this.blinkRateSpinSelector.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
               Object value = ((JSpinner)e.getSource()).getValue();
               EditorFontDialog.this.blinkCaret.setBlinkRate((Integer)value);
               EditorFontDialog.this.blinkSample.requestFocus();
               EditorFontDialog.this.blinkCaret.setVisible(true);
            }
         });
         JPanel tabPanel = new JPanel(new FlowLayout(0));
         tabPanel.add(new JLabel("Tab Size"));
         tabPanel.add(this.tabSizeSelector);
         tabPanel.add(this.tabSizeSpinSelector);
         JPanel blinkPanel = new JPanel(new FlowLayout(0));
         blinkPanel.add(new JLabel("Cursor Blinking Rate in ms (0=no blink)"));
         blinkPanel.add(this.blinkRateSpinSelector);
         blinkPanel.add(this.blinkSample);
         otherSettingsPanel.setLayout(new GridLayout(1, 2));
         JPanel leftColumnSettingsPanel = new JPanel(new GridLayout(4, 1));
         leftColumnSettingsPanel.add(tabPanel);
         leftColumnSettingsPanel.add(blinkPanel);
         leftColumnSettingsPanel.add(this.lineHighlightCheck);
         leftColumnSettingsPanel.add(this.autoIndentCheck);
         JPanel rightColumnSettingsPanel = new JPanel(new GridLayout(4, 1));
         this.popupGuidanceButtons = new ButtonGroup();
         this.popupGuidanceOptions = new JRadioButton[3];
         this.popupGuidanceOptions[0] = new JRadioButton("No popup instruction or directive guide");
         this.popupGuidanceOptions[1] = new JRadioButton("Display instruction guide after 1 letter typed");
         this.popupGuidanceOptions[2] = new JRadioButton("Display instruction guide after 2 letters typed");

         for(int i = 0; i < this.popupGuidanceOptions.length; ++i) {
            this.popupGuidanceOptions[i].setSelected(false);
            this.popupGuidanceOptions[i].setToolTipText(SettingsEditorAction.POPUP_GUIDANCE_TOOL_TIP_TEXT[i]);
            this.popupGuidanceButtons.add(this.popupGuidanceOptions[i]);
         }

         this.initialPopupGuidance = Globals.getSettings().getBooleanSetting(16) ? Globals.getSettings().getEditorPopupPrefixLength() : 0;
         this.popupGuidanceOptions[this.initialPopupGuidance].setSelected(true);
         new JPanel(new GridLayout(3, 1));
         rightColumnSettingsPanel.setBorder(BorderFactory.createTitledBorder("Popup Instruction Guide"));
         rightColumnSettingsPanel.add(this.popupGuidanceOptions[0]);
         rightColumnSettingsPanel.add(this.popupGuidanceOptions[1]);
         rightColumnSettingsPanel.add(this.popupGuidanceOptions[2]);
         otherSettingsPanel.add(leftColumnSettingsPanel);
         otherSettingsPanel.add(rightColumnSettingsPanel);
         return otherSettingsPanel;
      }

      private JPanel buildSyntaxStylePanel() {
         JPanel syntaxStylePanel = new JPanel();
         this.defaultStyles = SyntaxUtilities.getDefaultSyntaxStyles(Globals.getSettings().getBooleanSetting(21));
         this.initialStyles = SyntaxUtilities.getCurrentSyntaxStyles();
         String[] labels = MIPSTokenMarker.getMIPSTokenLabels();
         String[] sampleText = MIPSTokenMarker.getMIPSTokenExamples();
         this.syntaxStylesAction = false;
         int count = 0;

         for(int ix = 0; ix < labels.length; ++ix) {
            if (labels[ix] != null) {
               ++count;
            }
         }

         this.syntaxStyleIndex = new int[count];
         this.currentStyles = new SyntaxStyle[count];
         String[] label = new String[count];
         this.samples = new JLabel[count];
         this.foregroundButtons = new JButton[count];
         this.bold = new JToggleButton[count];
         this.italic = new JToggleButton[count];
         this.useDefault = new JCheckBox[count];
         Font genericFont = (new JLabel()).getFont();
         this.previewFont = new Font("Monospaced", 0, genericFont.getSize());
         Font boldFont = new Font("Serif", 1, genericFont.getSize());
         Font italicFont = new Font("Serif", 2, genericFont.getSize());
         count = 0;

         for(int ixx = 0; ixx < labels.length; ++ixx) {
            if (labels[ixx] != null) {
               this.syntaxStyleIndex[count] = ixx;
               this.samples[count] = new JLabel();
               this.samples[count].setOpaque(true);
               this.samples[count].setHorizontalAlignment(0);
               this.samples[count].setBorder(BorderFactory.createLineBorder(Color.black));
               this.samples[count].setText(sampleText[ixx]);
               this.samples[count].setBackground(Color.WHITE);
               this.samples[count].setToolTipText("Current setting; modify using buttons to the right");
               this.foregroundButtons[count] = new ColorSelectButton();
               this.foregroundButtons[count].addActionListener(new ForegroundChanger(count));
               this.foregroundButtons[count].setToolTipText("Click, to select text color");
               BoldItalicChanger boldItalicChanger = new BoldItalicChanger(count);
               this.bold[count] = new JToggleButton("B", false);
               this.bold[count].setFont(boldFont);
               this.bold[count].addActionListener(boldItalicChanger);
               this.bold[count].setToolTipText("Toggle text bold style");
               this.italic[count] = new JToggleButton("I", false);
               this.italic[count].setFont(italicFont);
               this.italic[count].addActionListener(boldItalicChanger);
               this.italic[count].setToolTipText("Toggle text italic style");
               label[count] = labels[ixx];
               this.useDefault[count] = new JCheckBox();
               this.useDefault[count].addItemListener(new DefaultChanger(count));
               this.useDefault[count].setToolTipText("Check, to select defaults (disables buttons)");
               ++count;
            }
         }

         this.initializeSyntaxStyleChangeables();
         syntaxStylePanel.setLayout(new BorderLayout());
         JPanel labelPreviewPanel = new JPanel(new GridLayout(this.syntaxStyleIndex.length, 2, 2, 2));
         JPanel buttonsPanel = new JPanel(new GridLayout(this.syntaxStyleIndex.length, 4, 2, 2));

         for(int i = 0; i < this.syntaxStyleIndex.length; ++i) {
            labelPreviewPanel.add(new JLabel(label[i], 4));
            labelPreviewPanel.add(this.samples[i]);
            buttonsPanel.add(this.foregroundButtons[i]);
            buttonsPanel.add(this.bold[i]);
            buttonsPanel.add(this.italic[i]);
            buttonsPanel.add(this.useDefault[i]);
         }

         JPanel instructions = new JPanel(new FlowLayout(1));
         JCheckBox illustrate = new JCheckBox() {
            protected void processMouseEvent(MouseEvent e) {
            }

            protected void processKeyEvent(KeyEvent e) {
            }
         };
         illustrate.setSelected(true);
         instructions.add(illustrate);
         instructions.add(new JLabel("= use defaults (disables buttons)"));
         labelPreviewPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
         buttonsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
         syntaxStylePanel.add(instructions, "North");
         syntaxStylePanel.add(labelPreviewPanel, "West");
         syntaxStylePanel.add(buttonsPanel, "Center");
         return syntaxStylePanel;
      }

      private void initializeSyntaxStyleChangeables() {
         for(int count = 0; count < this.samples.length; ++count) {
            int i = this.syntaxStyleIndex[count];
            this.samples[count].setFont(this.previewFont);
            this.samples[count].setForeground(this.initialStyles[i].getColor());
            this.foregroundButtons[count].setBackground(this.initialStyles[i].getColor());
            this.foregroundButtons[count].setEnabled(true);
            this.currentStyles[count] = this.initialStyles[i];
            this.bold[count].setSelected(this.initialStyles[i].isBold());
            Font f;
            if (this.bold[count].isSelected()) {
               f = this.samples[count].getFont();
               this.samples[count].setFont(f.deriveFont(f.getStyle() ^ 1));
            }

            this.italic[count].setSelected(this.initialStyles[i].isItalic());
            if (this.italic[count].isSelected()) {
               f = this.samples[count].getFont();
               this.samples[count].setFont(f.deriveFont(f.getStyle() ^ 2));
            }

            this.useDefault[count].setSelected(this.initialStyles[i].toString().equals(this.defaultStyles[i].toString()));
            if (this.useDefault[count].isSelected()) {
               this.foregroundButtons[count].setEnabled(false);
               this.bold[count].setEnabled(false);
               this.italic[count].setEnabled(false);
            }
         }

      }

      private void setSampleStyles(JLabel sample, SyntaxStyle style) {
         Font f = this.previewFont;
         if (style.isBold()) {
            f = f.deriveFont(f.getStyle() ^ 1);
         }

         if (style.isItalic()) {
            f = f.deriveFont(f.getStyle() ^ 2);
         }

         sample.setFont(f);
         sample.setForeground(style.getColor());
      }

      private class BoldItalicChanger implements ActionListener {
         private int row;

         public BoldItalicChanger(int row) {
            this.row = row;
         }

         public void actionPerformed(ActionEvent e) {
            Font f = EditorFontDialog.this.samples[this.row].getFont();
            if (e.getActionCommand() == "B") {
               if (EditorFontDialog.this.bold[this.row].isSelected()) {
                  EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() | 1));
               } else {
                  EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() ^ 1));
               }
            } else if (EditorFontDialog.this.italic[this.row].isSelected()) {
               EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() | 2));
            } else {
               EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() ^ 2));
            }

            EditorFontDialog.this.currentStyles[this.row] = new SyntaxStyle(EditorFontDialog.this.foregroundButtons[this.row].getBackground(), EditorFontDialog.this.italic[this.row].isSelected(), EditorFontDialog.this.bold[this.row].isSelected());
            EditorFontDialog.this.syntaxStylesAction = true;
         }
      }

      private class DefaultChanger implements ItemListener {
         private int row;

         public DefaultChanger(int pos) {
            this.row = pos;
         }

         public void itemStateChanged(ItemEvent e) {
            Color newBackground = null;
            Font newFont = null;
            if (e.getStateChange() == 1) {
               EditorFontDialog.this.foregroundButtons[this.row].setEnabled(false);
               EditorFontDialog.this.bold[this.row].setEnabled(false);
               EditorFontDialog.this.italic[this.row].setEnabled(false);
               EditorFontDialog.this.currentStyles[this.row] = new SyntaxStyle(EditorFontDialog.this.foregroundButtons[this.row].getBackground(), EditorFontDialog.this.italic[this.row].isSelected(), EditorFontDialog.this.bold[this.row].isSelected());
               SyntaxStyle defaultStyle = EditorFontDialog.this.defaultStyles[EditorFontDialog.this.syntaxStyleIndex[this.row]];
               EditorFontDialog.this.setSampleStyles(EditorFontDialog.this.samples[this.row], defaultStyle);
               EditorFontDialog.this.foregroundButtons[this.row].setBackground(defaultStyle.getColor());
               EditorFontDialog.this.bold[this.row].setSelected(defaultStyle.isBold());
               EditorFontDialog.this.italic[this.row].setSelected(defaultStyle.isItalic());
            } else {
               EditorFontDialog.this.setSampleStyles(EditorFontDialog.this.samples[this.row], EditorFontDialog.this.currentStyles[this.row]);
               EditorFontDialog.this.foregroundButtons[this.row].setBackground(EditorFontDialog.this.currentStyles[this.row].getColor());
               EditorFontDialog.this.bold[this.row].setSelected(EditorFontDialog.this.currentStyles[this.row].isBold());
               EditorFontDialog.this.italic[this.row].setSelected(EditorFontDialog.this.currentStyles[this.row].isItalic());
               EditorFontDialog.this.foregroundButtons[this.row].setEnabled(true);
               EditorFontDialog.this.bold[this.row].setEnabled(true);
               EditorFontDialog.this.italic[this.row].setEnabled(true);
            }

            EditorFontDialog.this.syntaxStylesAction = true;
         }
      }

      private class ForegroundChanger implements ActionListener {
         private int row;

         public ForegroundChanger(int pos) {
            this.row = pos;
         }

         public void actionPerformed(ActionEvent e) {
            JButton button = (JButton)e.getSource();
            Color newColor = JColorChooser.showDialog((Component)null, "Set Text Color", button.getBackground());
            if (newColor != null) {
               button.setBackground(newColor);
               EditorFontDialog.this.samples[this.row].setForeground(newColor);
            }

            EditorFontDialog.this.currentStyles[this.row] = new SyntaxStyle(button.getBackground(), EditorFontDialog.this.italic[this.row].isSelected(), EditorFontDialog.this.bold[this.row].isSelected());
            EditorFontDialog.this.syntaxStylesAction = true;
         }
      }
   }
}
