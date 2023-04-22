package mars.venus;

import java.awt.Dimension;
import javax.swing.JDesktopPane;
import mars.Globals;

public class ExecutePane extends JDesktopPane {
   private RegistersWindow registerValues;
   private Coprocessor1Window coprocessor1Values;
   private Coprocessor0Window coprocessor0Values;
   private DataSegmentWindow dataSegment;
   private TextSegmentWindow textSegment;
   private LabelsWindow labelValues;
   private VenusUI mainUI;
   private NumberDisplayBaseChooser valueDisplayBase;
   private NumberDisplayBaseChooser addressDisplayBase;
   private boolean labelWindowVisible;

   public ExecutePane(VenusUI mainUI, RegistersWindow regs, Coprocessor1Window cop1Regs, Coprocessor0Window cop0Regs) {
      this.mainUI = mainUI;
      this.addressDisplayBase = new NumberDisplayBaseChooser("Hexadecimal Addresses", Globals.getSettings().getDisplayAddressesInHex());
      this.valueDisplayBase = new NumberDisplayBaseChooser("Hexadecimal Values", Globals.getSettings().getDisplayValuesInHex());
      this.addressDisplayBase.setToolTipText("If checked, displays all memory addresses in hexadecimal.  Otherwise, decimal.");
      this.valueDisplayBase.setToolTipText("If checked, displays all memory and register contents in hexadecimal.  Otherwise, decimal.");
      NumberDisplayBaseChooser[] choosers = new NumberDisplayBaseChooser[]{this.addressDisplayBase, this.valueDisplayBase};
      this.registerValues = regs;
      this.coprocessor1Values = cop1Regs;
      this.coprocessor0Values = cop0Regs;
      this.textSegment = new TextSegmentWindow();
      this.dataSegment = new DataSegmentWindow(choosers);
      this.labelValues = new LabelsWindow();
      this.labelWindowVisible = Globals.getSettings().getLabelWindowVisibility();
      this.add(this.textSegment);
      this.add(this.dataSegment);
      this.add(this.labelValues);
      this.textSegment.pack();
      this.dataSegment.pack();
      this.labelValues.pack();
      this.textSegment.setVisible(true);
      this.dataSegment.setVisible(true);
      this.labelValues.setVisible(this.labelWindowVisible);
   }

   public void setWindowBounds() {
      int fullWidth = this.getSize().width - this.getInsets().left - this.getInsets().right;
      int fullHeight = this.getSize().height - this.getInsets().top - this.getInsets().bottom;
      int halfHeight = fullHeight / 2;
      Dimension textDim = new Dimension((int)((double)fullWidth * 0.75), halfHeight);
      Dimension dataDim = new Dimension(fullWidth, halfHeight);
      Dimension lablDim = new Dimension((int)((double)fullWidth * 0.25), halfHeight);
      Dimension textFullDim = new Dimension(fullWidth, halfHeight);
      this.dataSegment.setBounds(0, textDim.height + 1, dataDim.width, dataDim.height);
      if (this.labelWindowVisible) {
         this.textSegment.setBounds(0, 0, textDim.width, textDim.height);
         this.labelValues.setBounds(textDim.width + 1, 0, lablDim.width, lablDim.height);
      } else {
         this.textSegment.setBounds(0, 0, textFullDim.width, textFullDim.height);
         this.labelValues.setBounds(0, 0, 0, 0);
      }

   }

   public void setLabelWindowVisibility(boolean visibility) {
      if (!visibility && this.labelWindowVisible) {
         this.labelWindowVisible = false;
         this.textSegment.setVisible(false);
         this.labelValues.setVisible(false);
         this.setWindowBounds();
         this.textSegment.setVisible(true);
      } else if (visibility && !this.labelWindowVisible) {
         this.labelWindowVisible = true;
         this.textSegment.setVisible(false);
         this.setWindowBounds();
         this.textSegment.setVisible(true);
         this.labelValues.setVisible(true);
      }

   }

   public void clearPane() {
      this.getTextSegmentWindow().clearWindow();
      this.getDataSegmentWindow().clearWindow();
      this.getRegistersWindow().clearWindow();
      this.getCoprocessor1Window().clearWindow();
      this.getCoprocessor0Window().clearWindow();
      this.getLabelsWindow().clearWindow();
      if (this.mainUI.getMainPane().getSelectedComponent() == this) {
         this.mainUI.getMainPane().setSelectedComponent(this.mainUI.getMainPane().getEditTabbedPane());
         this.mainUI.getMainPane().setSelectedComponent(this);
      }

   }

   public TextSegmentWindow getTextSegmentWindow() {
      return this.textSegment;
   }

   public DataSegmentWindow getDataSegmentWindow() {
      return this.dataSegment;
   }

   public RegistersWindow getRegistersWindow() {
      return this.registerValues;
   }

   public Coprocessor1Window getCoprocessor1Window() {
      return this.coprocessor1Values;
   }

   public Coprocessor0Window getCoprocessor0Window() {
      return this.coprocessor0Values;
   }

   public LabelsWindow getLabelsWindow() {
      return this.labelValues;
   }

   public int getValueDisplayBase() {
      return this.valueDisplayBase.getBase();
   }

   public int getAddressDisplayBase() {
      return this.addressDisplayBase.getBase();
   }

   public NumberDisplayBaseChooser getValueDisplayBaseChooser() {
      return this.valueDisplayBase;
   }

   public NumberDisplayBaseChooser getAddressDisplayBaseChooser() {
      return this.addressDisplayBase;
   }

   public void numberDisplayBaseChanged(NumberDisplayBaseChooser chooser) {
      if (chooser == this.valueDisplayBase) {
         this.registerValues.updateRegisters();
         this.coprocessor1Values.updateRegisters();
         this.coprocessor0Values.updateRegisters();
         this.dataSegment.updateValues();
         this.textSegment.updateBasicStatements();
      } else {
         this.dataSegment.updateDataAddresses();
         this.labelValues.updateLabelAddresses();
         this.textSegment.updateCodeAddresses();
         this.textSegment.updateBasicStatements();
      }

   }
}
