package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mars.util.EditorFont;

public abstract class AbstractFontSettingDialog extends JDialog {
   JDialog editorDialog;
   JComboBox fontFamilySelector;
   JComboBox fontStyleSelector;
   JSlider fontSizeSelector;
   JSpinner fontSizeSpinSelector;
   JLabel fontSample;
   protected Font currentFont;
   String initialFontFamily;
   String initialFontStyle;
   String initialFontSize;
   private static String SEPARATOR = "___SEPARATOR____";

   public AbstractFontSettingDialog(Frame owner, String title, boolean modality, Font currentFont) {
      super(owner, title, modality);
      this.currentFont = currentFont;
      JPanel overallPanel = new JPanel(new BorderLayout());
      overallPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
      overallPanel.add(this.buildDialogPanel(), "Center");
      overallPanel.add(this.buildControlPanel(), "South");
      this.setContentPane(overallPanel);
      this.setDefaultCloseOperation(0);
      this.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent we) {
            AbstractFontSettingDialog.this.closeDialog();
         }
      });
      this.pack();
      this.setLocationRelativeTo(owner);
   }

   protected JPanel buildDialogPanel() {
      JPanel contents = new JPanel(new BorderLayout(20, 20));
      contents.setBorder(new EmptyBorder(10, 10, 10, 10));
      this.initialFontFamily = this.currentFont.getFamily();
      this.initialFontStyle = EditorFont.styleIntToStyleString(this.currentFont.getStyle());
      this.initialFontSize = EditorFont.sizeIntToSizeString(this.currentFont.getSize());
      String[] commonFontFamilies = EditorFont.getCommonFamilies();
      String[] allFontFamilies = EditorFont.getAllFamilies();
      String[][] fullList = new String[][]{commonFontFamilies, allFontFamilies};
      this.fontFamilySelector = new JComboBox(this.makeVectorData(fullList));
      this.fontFamilySelector.setRenderer(new ComboBoxRenderer());
      this.fontFamilySelector.addActionListener(new BlockComboListener(this.fontFamilySelector));
      this.fontFamilySelector.setSelectedItem(this.currentFont.getFamily());
      this.fontFamilySelector.setEditable(false);
      this.fontFamilySelector.setMaximumRowCount(commonFontFamilies.length);
      this.fontFamilySelector.setToolTipText("Short list of common font families followed by complete list.");
      String[] fontStyles = EditorFont.getFontStyleStrings();
      this.fontStyleSelector = new JComboBox(fontStyles);
      this.fontStyleSelector.setSelectedItem(EditorFont.styleIntToStyleString(this.currentFont.getStyle()));
      this.fontStyleSelector.setEditable(false);
      this.fontStyleSelector.setToolTipText("List of available font styles.");
      this.fontSizeSelector = new JSlider(6, 72, this.currentFont.getSize());
      this.fontSizeSelector.setToolTipText("Use slider to select font size from 6 to 72.");
      this.fontSizeSelector.addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
            Integer value = new Integer(((JSlider)e.getSource()).getValue());
            AbstractFontSettingDialog.this.fontSizeSpinSelector.setValue(value);
            AbstractFontSettingDialog.this.fontSample.setFont(AbstractFontSettingDialog.this.getFont());
         }
      });
      SpinnerNumberModel fontSizeSpinnerModel = new SpinnerNumberModel(this.currentFont.getSize(), 6, 72, 1);
      this.fontSizeSpinSelector = new JSpinner(fontSizeSpinnerModel);
      this.fontSizeSpinSelector.setToolTipText("Current font size in points.");
      this.fontSizeSpinSelector.addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
            Object value = ((JSpinner)e.getSource()).getValue();
            AbstractFontSettingDialog.this.fontSizeSelector.setValue((Integer)value);
            AbstractFontSettingDialog.this.fontSample.setFont(AbstractFontSettingDialog.this.getFont());
         }
      });
      ActionListener updateSample = new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            AbstractFontSettingDialog.this.fontSample.setFont(AbstractFontSettingDialog.this.getFont());
         }
      };
      this.fontFamilySelector.addActionListener(updateSample);
      this.fontStyleSelector.addActionListener(updateSample);
      JPanel familyStyleComponents = new JPanel(new GridLayout(2, 2, 4, 4));
      familyStyleComponents.add(new JLabel("Font Family"));
      familyStyleComponents.add(new JLabel("Font Style"));
      familyStyleComponents.add(this.fontFamilySelector);
      familyStyleComponents.add(this.fontStyleSelector);
      this.fontSample = new JLabel("Sample of this font", 0);
      this.fontSample.setBorder(new LineBorder(Color.BLACK));
      this.fontSample.setFont(this.getFont());
      this.fontSample.setToolTipText("Dynamically updated font sample based on current settings");
      JPanel sizeComponents = new JPanel();
      sizeComponents.add(new JLabel("Font Size "));
      sizeComponents.add(this.fontSizeSelector);
      sizeComponents.add(this.fontSizeSpinSelector);
      JPanel sizeAndSample = new JPanel(new GridLayout(2, 1, 4, 8));
      sizeAndSample.add(sizeComponents);
      sizeAndSample.add(this.fontSample);
      contents.add(familyStyleComponents, "North");
      contents.add(sizeAndSample, "Center");
      return contents;
   }

   protected abstract Component buildControlPanel();

   public Font getFont() {
      return EditorFont.createFontFromStringValues((String)this.fontFamilySelector.getSelectedItem(), (String)this.fontStyleSelector.getSelectedItem(), this.fontSizeSpinSelector.getValue().toString());
   }

   protected void performApply() {
      this.apply(this.getFont());
   }

   protected void closeDialog() {
      this.setVisible(false);
      this.dispose();
   }

   protected void reset() {
      this.fontFamilySelector.setSelectedItem(this.initialFontFamily);
      this.fontStyleSelector.setSelectedItem(this.initialFontStyle);
      this.fontSizeSelector.setValue(EditorFont.sizeStringToSizeInt(this.initialFontSize));
      this.fontSizeSpinSelector.setValue(new Integer(EditorFont.sizeStringToSizeInt(this.initialFontSize)));
   }

   protected abstract void apply(Font var1);

   private Vector makeVectorData(String[][] str) {
      boolean needSeparator = false;
      Vector data = new Vector();

      for(int i = 0; i < str.length; ++i) {
         if (needSeparator) {
            data.addElement(SEPARATOR);
         }

         for(int j = 0; j < str[i].length; ++j) {
            data.addElement(str[i][j]);
            needSeparator = true;
         }
      }

      return data;
   }

   private class BlockComboListener implements ActionListener {
      JComboBox combo;
      Object currentItem;

      BlockComboListener(JComboBox combo) {
         this.combo = combo;
         combo.setSelectedIndex(0);
         this.currentItem = combo.getSelectedItem();
      }

      public void actionPerformed(ActionEvent e) {
         String tempItem = (String)this.combo.getSelectedItem();
         if (AbstractFontSettingDialog.SEPARATOR.equals(tempItem)) {
            this.combo.setSelectedItem(this.currentItem);
         } else {
            this.currentItem = tempItem;
         }

      }
   }

   private class ComboBoxRenderer extends JLabel implements ListCellRenderer {
      JSeparator separator;

      public ComboBoxRenderer() {
         this.setOpaque(true);
         this.setBorder(new EmptyBorder(1, 1, 1, 1));
         this.separator = new JSeparator(0);
      }

      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         String str = value == null ? "" : value.toString();
         if (AbstractFontSettingDialog.SEPARATOR.equals(str)) {
            return this.separator;
         } else {
            if (isSelected) {
               this.setBackground(list.getSelectionBackground());
               this.setForeground(list.getSelectionForeground());
            } else {
               this.setBackground(list.getBackground());
               this.setForeground(list.getForeground());
            }

            this.setFont(list.getFont());
            this.setText(str);
            return this;
         }
      }
   }
}
