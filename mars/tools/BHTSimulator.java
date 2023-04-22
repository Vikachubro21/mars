package mars.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import javax.swing.JComponent;
import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.hardware.RegisterFile;

public class BHTSimulator extends AbstractMarsToolAndApplication implements ActionListener {
   public static final int BHT_DEFAULT_SIZE = 16;
   public static final int BHT_DEFAULT_HISTORY = 1;
   public static final boolean BHT_DEFAULT_INITVAL = false;
   public static final String BHT_NAME = "BHT Simulator";
   public static final String BHT_VERSION = "Version 1.0 (Ingo Kofler)";
   public static final String BHT_HEADING = "Branch History Table Simulator";
   private BHTSimGUI m_gui;
   private BHTableModel m_bhtModel;
   private int m_pendingBranchInstAddress;
   private boolean m_lastBranchTaken;

   public BHTSimulator() {
      super("BHT Simulator, Version 1.0 (Ingo Kofler)", "Branch History Table Simulator");
   }

   protected void addAsObserver() {
      this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
      this.addAsObserver(RegisterFile.getProgramCounterRegister());
   }

   protected JComponent buildMainDisplayArea() {
      this.m_gui = new BHTSimGUI();
      this.m_bhtModel = new BHTableModel(16, 1, false);
      this.m_gui.getTabBHT().setModel(this.m_bhtModel);
      this.m_gui.getCbBHThistory().setSelectedItem(new Integer(1));
      this.m_gui.getCbBHTentries().setSelectedItem(new Integer(16));
      this.m_gui.getCbBHTentries().addActionListener(this);
      this.m_gui.getCbBHThistory().addActionListener(this);
      this.m_gui.getCbBHTinitVal().addActionListener(this);
      return this.m_gui;
   }

   public String getName() {
      return "BHT Simulator";
   }

   protected void reset() {
      this.resetSimulator();
   }

   public void actionPerformed(ActionEvent event) {
      if (event.getSource() == this.m_gui.getCbBHTentries() || event.getSource() == this.m_gui.getCbBHThistory() || event.getSource() == this.m_gui.getCbBHTinitVal()) {
         this.resetSimulator();
      }

   }

   protected void resetSimulator() {
      this.m_gui.getTfInstruction().setText("");
      this.m_gui.getTfAddress().setText("");
      this.m_gui.getTfIndex().setText("");
      this.m_gui.getTaLog().setText("");
      this.m_bhtModel.initBHT((Integer)this.m_gui.getCbBHTentries().getSelectedItem(), (Integer)this.m_gui.getCbBHThistory().getSelectedItem(), ((String)this.m_gui.getCbBHTinitVal().getSelectedItem()).equals("TAKE"));
      this.m_pendingBranchInstAddress = 0;
      this.m_lastBranchTaken = false;
   }

   protected void handlePreBranchInst(ProgramStatement stmt) {
      String strStmt = stmt.getBasicAssemblyStatement();
      int address = stmt.getAddress();
      int idx = this.m_bhtModel.getIdxForAddress(address);
      this.m_gui.getTfInstruction().setText(strStmt);
      this.m_gui.getTfAddress().setText("0x" + Integer.toHexString(address));
      this.m_gui.getTfIndex().setText("" + idx);
      this.m_gui.getTabBHT().setSelectionBackground(BHTSimGUI.COLOR_PREPREDICTION);
      this.m_gui.getTabBHT().addRowSelectionInterval(idx, idx);
      this.m_gui.getTaLog().append("instruction " + strStmt + " at address 0x" + Integer.toHexString(address) + ", maps to index " + idx + "\n");
      this.m_gui.getTaLog().append("branches to address 0x" + extractBranchAddress(stmt) + "\n");
      this.m_gui.getTaLog().append("prediction is: " + (this.m_bhtModel.getPredictionAtIdx(idx) ? "take" : "do not take") + "...\n");
      this.m_gui.getTaLog().setCaretPosition(this.m_gui.getTaLog().getDocument().getLength());
   }

   protected void handleExecBranchInst(int branchInstAddr, boolean branchTaken) {
      int idx = this.m_bhtModel.getIdxForAddress(branchInstAddr);
      boolean correctPrediction = this.m_bhtModel.getPredictionAtIdx(idx) == branchTaken;
      this.m_gui.getTabBHT().setSelectionBackground(correctPrediction ? BHTSimGUI.COLOR_PREDICTION_CORRECT : BHTSimGUI.COLOR_PREDICTION_INCORRECT);
      this.m_gui.getTaLog().append("branch " + (branchTaken ? "taken" : "not taken") + ", prediction was " + (correctPrediction ? "correct" : "incorrect") + "\n\n");
      this.m_gui.getTaLog().setCaretPosition(this.m_gui.getTaLog().getDocument().getLength());
      this.m_bhtModel.updatePredictionAtIdx(idx, branchTaken);
   }

   protected static boolean isBranchInstruction(ProgramStatement stmt) {
      int opCode = stmt.getBinaryStatement() >>> 26;
      int funct = stmt.getBinaryStatement() & 31;
      if (opCode == 1) {
         if (0 <= funct && funct <= 7) {
            return true;
         }

         if (16 <= funct && funct <= 19) {
            return true;
         }
      }

      if (4 <= opCode && opCode <= 7) {
         return true;
      } else {
         return 20 <= opCode && opCode <= 23;
      }
   }

   protected static boolean willBranch(ProgramStatement stmt) {
      int opCode = stmt.getBinaryStatement() >>> 26;
      int funct = stmt.getBinaryStatement() & 31;
      int rs = stmt.getBinaryStatement() >>> 21 & 31;
      int rt = stmt.getBinaryStatement() >>> 16 & 31;
      int valRS = RegisterFile.getRegisters()[rs].getValue();
      int valRT = RegisterFile.getRegisters()[rt].getValue();
      if (opCode == 1) {
         switch (funct) {
            case 0:
               return valRS < 0;
            case 1:
               return valRS >= 0;
            case 2:
               return valRS < 0;
            case 3:
               return valRS >= 0;
         }
      }

      switch (opCode) {
         case 4:
            return valRS == valRT;
         case 5:
            return valRS != valRT;
         case 6:
            return valRS <= 0;
         case 7:
            return valRS >= 0;
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
         default:
            return true;
         case 20:
            return valRS == valRT;
         case 21:
            return valRS != valRT;
         case 22:
            return valRS <= 0;
         case 23:
            return valRS >= 0;
      }
   }

   protected static int extractBranchAddress(ProgramStatement stmt) {
      short offset = (short)(stmt.getBinaryStatement() & '\uffff');
      return stmt.getAddress() + (offset << 2) + 4;
   }

   protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
      if (notice.accessIsFromMIPS()) {
         if (notice.getAccessType() == 0 && notice instanceof MemoryAccessNotice) {
            MemoryAccessNotice memAccNotice = (MemoryAccessNotice)notice;

            try {
               ProgramStatement stmt = Memory.getInstance().getStatementNoNotify(memAccNotice.getAddress());
               if (stmt != null) {
                  boolean clearTextFields = true;
                  if (this.m_pendingBranchInstAddress != 0) {
                     this.handleExecBranchInst(this.m_pendingBranchInstAddress, this.m_lastBranchTaken);
                     clearTextFields = false;
                     this.m_pendingBranchInstAddress = 0;
                  }

                  if (isBranchInstruction(stmt)) {
                     this.handlePreBranchInst(stmt);
                     this.m_lastBranchTaken = willBranch(stmt);
                     this.m_pendingBranchInstAddress = stmt.getAddress();
                     clearTextFields = false;
                  }

                  if (clearTextFields) {
                     this.m_gui.getTfInstruction().setText("");
                     this.m_gui.getTfAddress().setText("");
                     this.m_gui.getTfIndex().setText("");
                     this.m_gui.getTabBHT().clearSelection();
                  }
               } else if (this.m_pendingBranchInstAddress != 0) {
                  this.handleExecBranchInst(this.m_pendingBranchInstAddress, this.m_lastBranchTaken);
                  this.m_pendingBranchInstAddress = 0;
               }
            } catch (AddressErrorException var6) {
            }
         }

      }
   }
}
