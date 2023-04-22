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

public class InstructionStatistics extends AbstractMarsToolAndApplication {
   private static String NAME = "Instruction Statistics";
   private static String VERSION = "Version 1.0 (Ingo Kofler)";
   private static String HEADING = "";
   private static final int MAX_CATEGORY = 5;
   private static final int CATEGORY_ALU = 0;
   private static final int CATEGORY_JUMP = 1;
   private static final int CATEGORY_BRANCH = 2;
   private static final int CATEGORY_MEM = 3;
   private static final int CATEGORY_OTHER = 4;
   private JTextField m_tfTotalCounter;
   private JTextField[] m_tfCounters;
   private JProgressBar[] m_pbCounters;
   private int m_totalCounter = 0;
   private int[] m_counters = new int[5];
   private String[] m_categoryLabels = new String[]{"ALU", "Jump", "Branch", "Memory", "Other"};
   protected int lastAddress = -1;

   public InstructionStatistics(String title, String heading) {
      super(title, heading);
   }

   public InstructionStatistics() {
      super(NAME + ", " + VERSION, HEADING);
   }

   public String getName() {
      return NAME;
   }

   protected JComponent buildMainDisplayArea() {
      JPanel panel = new JPanel(new GridBagLayout());
      this.m_tfTotalCounter = new JTextField("0", 10);
      this.m_tfTotalCounter.setEditable(false);
      this.m_tfCounters = new JTextField[5];
      this.m_pbCounters = new JProgressBar[5];

      for(int i = 0; i < 5; ++i) {
         this.m_tfCounters[i] = new JTextField("0", 10);
         this.m_tfCounters[i].setEditable(false);
         this.m_pbCounters[i] = new JProgressBar(0);
         this.m_pbCounters[i].setStringPainted(true);
      }

      GridBagConstraints c = new GridBagConstraints();
      c.anchor = 21;
      c.gridheight = c.gridwidth = 1;
      c.gridx = 2;
      c.gridy = 1;
      c.insets = new Insets(0, 0, 17, 0);
      panel.add(new JLabel("Total: "), c);
      c.gridx = 3;
      panel.add(this.m_tfTotalCounter, c);
      c.insets = new Insets(3, 3, 3, 3);

      for(int i = 0; i < 5; ++i) {
         ++c.gridy;
         c.gridx = 2;
         panel.add(new JLabel(this.m_categoryLabels[i] + ":   "), c);
         c.gridx = 3;
         panel.add(this.m_tfCounters[i], c);
         c.gridx = 4;
         panel.add(this.m_pbCounters[i], c);
      }

      return panel;
   }

   protected void addAsObserver() {
      this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
   }

   protected int getInstructionCategory(ProgramStatement stmt) {
      int opCode = stmt.getBinaryStatement() >>> 26;
      int funct = stmt.getBinaryStatement() & 31;
      if (opCode == 0) {
         if (funct == 0) {
            return 0;
         } else if (2 <= funct && funct <= 7) {
            return 0;
         } else if (funct != 8 && funct != 9) {
            return 16 <= funct && funct <= 47 ? 0 : 4;
         } else {
            return 1;
         }
      } else if (opCode == 1) {
         if (0 <= funct && funct <= 7) {
            return 2;
         } else {
            return 16 <= funct && funct <= 19 ? 2 : 4;
         }
      } else if (opCode != 2 && opCode != 3) {
         if (4 <= opCode && opCode <= 7) {
            return 2;
         } else if (8 <= opCode && opCode <= 15) {
            return 0;
         } else if (20 <= opCode && opCode <= 23) {
            return 2;
         } else if (32 <= opCode && opCode <= 38) {
            return 3;
         } else {
            return 40 <= opCode && opCode <= 46 ? 3 : 4;
         }
      } else {
         return 1;
      }
   }

   protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
      if (notice.accessIsFromMIPS()) {
         if (notice.getAccessType() == 0 && notice instanceof MemoryAccessNotice) {
            MemoryAccessNotice memAccNotice = (MemoryAccessNotice)notice;
            int a = memAccNotice.getAddress();
            if (a == this.lastAddress) {
               return;
            }

            this.lastAddress = a;

            try {
               ProgramStatement stmt = Memory.getInstance().getStatementNoNotify(memAccNotice.getAddress());
               if (stmt != null) {
                  int category = this.getInstructionCategory(stmt);
                  ++this.m_totalCounter;
                  int var10002 = this.m_counters[category]++;
                  this.updateDisplay();
               }
            } catch (AddressErrorException var7) {
            }
         }

      }
   }

   protected void initializePreGUI() {
      this.m_totalCounter = 0;
      this.lastAddress = -1;

      for(int i = 0; i < 5; ++i) {
         this.m_counters[i] = 0;
      }

   }

   protected void reset() {
      this.m_totalCounter = 0;
      this.lastAddress = -1;

      for(int i = 0; i < 5; ++i) {
         this.m_counters[i] = 0;
      }

      this.updateDisplay();
   }

   protected void updateDisplay() {
      this.m_tfTotalCounter.setText(String.valueOf(this.m_totalCounter));

      for(int i = 0; i < 5; ++i) {
         this.m_tfCounters[i].setText(String.valueOf(this.m_counters[i]));
         this.m_pbCounters[i].setMaximum(this.m_totalCounter);
         this.m_pbCounters[i].setValue(this.m_counters[i]);
      }

   }
}
