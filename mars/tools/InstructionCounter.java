package mars.tools;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Observable;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.BasicInstructionFormat;

public class InstructionCounter extends AbstractMarsToolAndApplication {
   private static String name = "Instruction Counter";
   private static String version = "Version 1.0 (Felipe Lessa)";
   private static String heading = "Counting the number of instructions executed";
   protected int counter = 0;
   private JTextField counterField;
   protected int counterR = 0;
   private JTextField counterRField;
   private JProgressBar progressbarR;
   protected int counterI = 0;
   private JTextField counterIField;
   private JProgressBar progressbarI;
   protected int counterJ = 0;
   private JTextField counterJField;
   private JProgressBar progressbarJ;
   protected int lastAddress = -1;

   public InstructionCounter(String title, String heading) {
      super(title, heading);
   }

   public InstructionCounter() {
      super(name + ", " + version, heading);
   }

   public String getName() {
      return name;
   }

   protected JComponent buildMainDisplayArea() {
      JPanel panel = new JPanel(new GridBagLayout());
      this.counterField = new JTextField("0", 10);
      this.counterField.setEditable(false);
      this.counterRField = new JTextField("0", 10);
      this.counterRField.setEditable(false);
      this.progressbarR = new JProgressBar(0);
      this.progressbarR.setStringPainted(true);
      this.counterIField = new JTextField("0", 10);
      this.counterIField.setEditable(false);
      this.progressbarI = new JProgressBar(0);
      this.progressbarI.setStringPainted(true);
      this.counterJField = new JTextField("0", 10);
      this.counterJField.setEditable(false);
      this.progressbarJ = new JProgressBar(0);
      this.progressbarJ.setStringPainted(true);
      GridBagConstraints c = new GridBagConstraints();
      c.anchor = 21;
      c.gridheight = c.gridwidth = 1;
      c.gridx = 3;
      c.gridy = 1;
      c.insets = new Insets(0, 0, 17, 0);
      panel.add(this.counterField, c);
      c.insets = new Insets(0, 0, 0, 0);
      ++c.gridy;
      panel.add(this.counterRField, c);
      ++c.gridy;
      panel.add(this.counterIField, c);
      ++c.gridy;
      panel.add(this.counterJField, c);
      c.anchor = 22;
      c.gridx = 1;
      c.gridwidth = 2;
      c.gridy = 1;
      c.insets = new Insets(0, 0, 17, 0);
      panel.add(new JLabel("Instructions so far: "), c);
      c.insets = new Insets(0, 0, 0, 0);
      c.gridx = 2;
      c.gridwidth = 1;
      ++c.gridy;
      panel.add(new JLabel("R-type: "), c);
      ++c.gridy;
      panel.add(new JLabel("I-type: "), c);
      ++c.gridy;
      panel.add(new JLabel("J-type: "), c);
      c.insets = new Insets(3, 3, 3, 3);
      c.gridx = 4;
      c.gridy = 2;
      panel.add(this.progressbarR, c);
      ++c.gridy;
      panel.add(this.progressbarI, c);
      ++c.gridy;
      panel.add(this.progressbarJ, c);
      return panel;
   }

   protected void addAsObserver() {
      this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
   }

   protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
      if (notice.accessIsFromMIPS()) {
         if (notice.getAccessType() == 0) {
            MemoryAccessNotice m = (MemoryAccessNotice)notice;
            int a = m.getAddress();
            if (a != this.lastAddress) {
               this.lastAddress = a;
               ++this.counter;

               try {
                  ProgramStatement stmt = Memory.getInstance().getStatement(a);
                  BasicInstruction instr = (BasicInstruction)stmt.getInstruction();
                  BasicInstructionFormat format = instr.getInstructionFormat();
                  if (format == BasicInstructionFormat.R_FORMAT) {
                     ++this.counterR;
                  } else if (format != BasicInstructionFormat.I_FORMAT && format != BasicInstructionFormat.I_BRANCH_FORMAT) {
                     if (format == BasicInstructionFormat.J_FORMAT) {
                        ++this.counterJ;
                     }
                  } else {
                     ++this.counterI;
                  }
               } catch (AddressErrorException var8) {
                  var8.printStackTrace();
               }

               this.updateDisplay();
            }
         }
      }
   }

   protected void initializePreGUI() {
      this.counter = this.counterR = this.counterI = this.counterJ = 0;
      this.lastAddress = -1;
   }

   protected void reset() {
      this.counter = this.counterR = this.counterI = this.counterJ = 0;
      this.lastAddress = -1;
      this.updateDisplay();
   }

   protected void updateDisplay() {
      this.counterField.setText(String.valueOf(this.counter));
      this.counterRField.setText(String.valueOf(this.counterR));
      this.progressbarR.setMaximum(this.counter);
      this.progressbarR.setValue(this.counterR);
      this.counterIField.setText(String.valueOf(this.counterI));
      this.progressbarI.setMaximum(this.counter);
      this.progressbarI.setValue(this.counterI);
      this.counterJField.setText(String.valueOf(this.counterJ));
      this.progressbarJ.setMaximum(this.counter);
      this.progressbarJ.setValue(this.counterJ);
      if (this.counter == 0) {
         this.progressbarR.setString("0%");
         this.progressbarI.setString("0%");
         this.progressbarJ.setString("0%");
      } else {
         this.progressbarR.setString(this.counterR * 100 / this.counter + "%");
         this.progressbarI.setString(this.counterI * 100 / this.counter + "%");
         this.progressbarJ.setString(this.counterJ * 100 / this.counter + "%");
      }

   }
}
